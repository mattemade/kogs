package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import com.soywiz.korma.geom.Angle
import net.mattemade.platformer.GRAVITY_IN_FALL
import net.mattemade.platformer.GRAVITY_IN_JUMP
import net.mattemade.platformer.GRAVITY_IN_JUMPFALL
import net.mattemade.platformer.JUMP_VELOCITY
import net.mattemade.platformer.MAX_FALL_VELOCITY
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.utils.math.lerp
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.contacts.ContactEdge
import org.jbox2d.dynamics.World as B2dWorld

class Box2DPhysicsSystem(
    //private val physics: B2dWorld = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(Box2DPhysicsComponent, PositionComponent, ContextComponent) }, interval = interval),
    ContactListener, Releasing by Self() {

    private val physics: B2dWorld = B2dWorld().rememberTo {
        var body = it.bodyList
        while (body != null) {
            it.destroyBody(body)
            body = body.getNext()
        }
    }.also {
        it.setContactListener(this)
    }

    override fun onUpdate() {
        if (physics.autoClearForces) {
            physics.autoClearForces = false
        }
        super.onUpdate()
        physics.clearForces()
    }

    override fun onTick() {
        super.onTick()
        physics.step(deltaTime, 6, 2)
    }

    override fun onTickEntity(entity: Entity) {
        val context = entity[ContextComponent]
        val physicsComponent = entity[Box2DPhysicsComponent].apply {
            previousPosition.set(
                body.position.x, body.position.y,
            )

            if (context.touchingWalls && (body.linearVelocityY > 0f || context.wallSlide) && body.linearVelocityX == 0f) {
                body.linearVelocityY = 1f
                context.wallSlide = true
            } else if (context.wallSlide) {
                entity[JumpComponent].apply {
                    coyoteTimeInTicks = JumpComponent.COYOTE_TICKS // just to allow jump off the wall without using double jump
                    wasJumping = true // just to force applying lower gravity
                }
                context.wallSlide = false
                body.isAwake = true
            }
            context.standing = body.getContactList().let { it.isTouching<Feet, Wall>() || it.isTouching<Feet, Platform>() }
            context.touchingWalls = body.getContactList().isTouching<Hands, Wall>()
        }


        entity.getOrNull(JumpComponent)?.apply {
            if (jumping) {
                wasJumping = true
                if (canHoldJumpForTicks-- > 0) {
                    physicsComponent.body.applyImpulse(0f, -JUMP_VELOCITY - physicsComponent.body.linearVelocityY)
                } else {
                    jumping = false
                }
            }

            if (physicsComponent.body.linearVelocityY == 0f && context.standing) {
                coyoteTimeInTicks = JumpComponent.COYOTE_TICKS
                canJumpFromGround = true
                canJumpInAir = JumpComponent.MAX_AIR_JUMPS
                wasJumping = false
            } else {
                coyoteTimeInTicks--
                canJumpFromGround = coyoteTimeInTicks > 0
            }
            physicsComponent.body.gravityScale =
                when {
                    context.wallSlide -> 0f
                    jumping -> GRAVITY_IN_JUMP
                    wasJumping -> GRAVITY_IN_JUMPFALL
                    else -> GRAVITY_IN_FALL
                }
        }
        entity.getOrNull(MomentaryForceComponent)?.let { force ->
            physicsComponent.body.applyImpulse(force.force.x, force.force.y)
            entity.configure {
                it -= MomentaryForceComponent
            }
        }
        entity.getOrNull(MoveComponent)?.let { move ->
            physicsComponent.body.applyImpulse(
                move.moveDirection.x * move.speed - physicsComponent.body.linearVelocityX,
                if (move.moveDirection.y != 0f) move.moveDirection.y * move.speed - physicsComponent.body.linearVelocityY else 0f
            )
            if (move.fallThrough) {
                physicsComponent.body.applyImpulse(0f, WALK_VELOCITY)
                tempVec2.set(physicsComponent.body.position.x, physicsComponent.body.position.y + 0.05f)
                physicsComponent.body.setTransformDegrees(tempVec2, 0f)
                move.fallThrough = false
                entity[JumpComponent].coyoteTimeInTicks = 0 // to prevent coyote jump right after falling
            }
            if (move.dashDirection.x != 0f || move.dashDirection.y != 0f) {
                // override everything we calculated so far!!
                physicsComponent.body.gravityScale = 0f
                physicsComponent.body.linearVelocityX = move.dashDirection.x
                physicsComponent.body.linearVelocityY = move.dashDirection.y
            }
        }

        if (physicsComponent.body.linearVelocityY > MAX_FALL_VELOCITY) {
            physicsComponent.body.applyImpulse(0f, MAX_FALL_VELOCITY - physicsComponent.body.linearVelocityY)
        }
    }

    private fun Body.applyImpulse(x: Float, y: Float) {
        tempVec2.set(x, y).mulLocal(getMass()) // so the applied velocity won't depend on mass
        applyLinearImpulse(tempVec2, worldCenter, wake = true)
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        // interpolate the simulated position to better fit the render time
        val positionComponent = entity[PositionComponent]
        val physicsComponent = entity[Box2DPhysicsComponent]

        positionComponent.position.set(
            lerp(physicsComponent.previousPosition.x, physicsComponent.body.position.x, alpha),
            lerp(physicsComponent.previousPosition.y, physicsComponent.body.position.y, alpha),
        )
    }

    fun createPlayerBody(entityCreateContext: EntityCreateContext, entity: Entity, initialPlayerBounds: Rect) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(initialPlayerBounds.cx, initialPlayerBounds.cy)
                    gravityScale = GRAVITY_IN_FALL
                }).apply {
                    isFixedRotation = false
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = PLAYER_HANDS_MASK
                            maskBits = PLAYER_LIMB_COLLISIONS
                        }
                        shape = PolygonShape().apply {
                            setAsBox(
                                initialPlayerBounds.width * 0.5f * 1f, // a bit longer that body width
                                initialPlayerBounds.height * 0.15f, // little portion of the body
                                center = Vec2(0f, -initialPlayerBounds.height * 0.25f), // closer to the top
                                angle = Angle.ZERO
                            )
                        }
                        userData = Hands(entity)
                    })
                },
            ).apply {
                bodyFixture = body.createFixture(FixtureDef().apply {
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = PLAYER_BODY_MASK
                        maskBits = PLAYER_BODY_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            initialPlayerBounds.width * 0.5f * 0.9f,
                            initialPlayerBounds.height * 0.5f * 0.9f
                        )
                    }
                    userData = entity
                })!!
                feetFixture = body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = PLAYER_FOOT_MASK
                        maskBits = PLAYER_LIMB_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            initialPlayerBounds.width * 0.5f * 0.8f, // a bit shorter that body width
                            0.2f, // just a tiny block at the bottom
                            center = Vec2(0f, initialPlayerBounds.height * 0.5f),
                            angle = Angle.ZERO
                        )
                    }
                    userData = Feet(entity)
                })!!
            }
        }
    }

    fun createWall(vertices: Array<Vec2>, userData: Any? = null) {
        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = WALL_MASK
                }
                shape = ChainShape().apply {
                    createLoop(vertices, vertices.size)
                }
                this.userData = userData ?: Wall
            })
        }
    }

    fun createPlatform(fromX: Float, toX: Float, y: Float) {
        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = WALL_MASK
                }
                shape = EdgeShape(fromX, y, toX, y)
                userData = Platform(y)
            })
        }
    }

    fun createWater(vertices: Array<Vec2>) {
        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = WATER_MASK
                }
                shape = ChainShape().apply {
                    createLoop(vertices, vertices.size)
                }
                userData = Platform()
            })
        }
    }

    fun teleport(entity: Entity, moveToPosition: Vec2f, physicsComponent: Box2DPhysicsComponent) {
        entity[Box2DPhysicsComponent].apply {
            previousPosition.set(moveToPosition)
            body.setTransformDegrees(Vec2(moveToPosition.x, moveToPosition.y), 0f)
            body.linearVelocityX = physicsComponent.body.linearVelocityX
            body.linearVelocityY = physicsComponent.body.linearVelocityY
        }
    }

    override fun beginContact(contact: Contact) {

    }

    override fun endContact(contact: Contact) {

    }

    override fun postSolve(
        contact: Contact,
        impulse: ContactImpulse
    ) {

    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        contact.with<Entity> { other ->
            when (other) {
                is Platform -> contact.isEnabled = this[Box2DPhysicsComponent].body.position.y + 0.9f <= other.top
            }
        }
    }


    private class Platform(/*var isActive: Boolean = true, */val top: Float = 0f)
    private object Water
    private object Wall
    private class Feet(val entity: Entity)
    private class Hands(val entity: Entity)

    companion object {
        private val tempVec2 = Vec2()

        private var SHIFT_INDEX = 0
        private val NEXT_MASK get() = 1 shl SHIFT_INDEX++
        private val WALL_MASK = NEXT_MASK

        //private val PLATFORM_MASK = NEXT_MASK
        private val WATER_MASK = NEXT_MASK
        private val PLAYER_BODY_MASK = NEXT_MASK
        private val PLAYER_FOOT_MASK = NEXT_MASK
        private val PLAYER_CENTER_MASK = NEXT_MASK
        private val PLAYER_HEAD_MASK = NEXT_MASK
        private val PLAYER_HANDS_MASK = NEXT_MASK

        private val PLAYER_BODY_COLLISIONS = WALL_MASK// or PLATFORM_MASK
        private val PLAYER_LIMB_COLLISIONS = WALL_MASK or WATER_MASK

        private inline fun <reified T> Contact.with(crossinline action: T.(Any?) -> Unit) =
            (getFixtureA()?.userData as? T)?.action(getFixtureB()?.userData) ?: (getFixtureB()?.userData as? T)?.action(
                getFixtureA()?.userData
            )

        private inline fun <reified T, reified K> ContactEdge?.isTouching(): Boolean {
            var edge = this
            while (edge != null) {
                edge.contact?.let {
                    if (it.isTouching && (
                                (it.getFixtureA()?.userData is T && it.getFixtureB()?.userData is K)
                                        || (it.getFixtureB()?.userData is T && it.getFixtureA()?.userData is K)
                                )
                        ) {
                        return true
                    }
                }
                edge = edge.next
            }
            return false
        }

    }

}