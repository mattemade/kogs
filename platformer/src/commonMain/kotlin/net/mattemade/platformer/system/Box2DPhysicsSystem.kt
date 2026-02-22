package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import net.mattemade.platformer.GRAVITY_IN_FALL
import net.mattemade.platformer.GRAVITY_IN_JUMP
import net.mattemade.platformer.GRAVITY_IN_JUMPFALL
import net.mattemade.platformer.JUMP_VELOCITY
import net.mattemade.platformer.MAX_FALL_VELOCITY
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
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
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.World as B2dWorld

class Box2DPhysicsSystem(
    //private val physics: B2dWorld = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(Box2DPhysicsComponent, PositionComponent) }, interval = interval), ContactListener, Releasing by Self() {

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
        val physicsComponent = entity[Box2DPhysicsComponent].apply {
            previousPosition.set(
                body.position.x, body.position.y,
            )
        }

        entity.getOrNull(MoveComponent)?.let { move ->
            tempVec2.set(
                move.direction.x * move.speed - physicsComponent.body.linearVelocityX,
                if (move.direction.y != 0f) move.direction.y * move.speed - physicsComponent.body.linearVelocityY else 0f
            ).mulLocal(physicsComponent.body.getMass()) // so the applied velocity won't depend on mass
            physicsComponent.body.applyLinearImpulse(tempVec2, physicsComponent.body.worldCenter, wake = true)
        }
        entity.getOrNull(JumpComponent)?.apply {
            if (jumping) {
                wasJumping = true
                if (canHoldJumpForTicks-- > 0) {
                    tempVec2.set(0f, -JUMP_VELOCITY - physicsComponent.body.linearVelocityY)
                        .mulLocal(physicsComponent.body.getMass()) // so the applied velocity won't depend on mass
                    physicsComponent.body.applyLinearImpulse(tempVec2, physicsComponent.body.worldCenter, wake = true)
                } else {
                    jumping = false
                }
            }

            // TODO nononono, make a better check with a ground sensor
            if (physicsComponent.body.linearVelocityY == 0f) {
                coyoteTimeInTicks = JumpComponent.COYOTE_TICKS
                canJumpFromGround = true
                canJumpInAir = 2
                wasJumping = false
            } else {
                coyoteTimeInTicks--
                canJumpFromGround = coyoteTimeInTicks > 0
            }
            physicsComponent.body.gravityScale = if (jumping) GRAVITY_IN_JUMP else if (wasJumping) GRAVITY_IN_JUMPFALL else GRAVITY_IN_FALL
        }
        entity.getOrNull(MomentaryForceComponent)?.let { force ->
            tempVec2.set(force.force.x, force.force.y)
                .mulLocal(physicsComponent.body.getMass()) // so the applied velocity won't depend on mass
            physicsComponent.body.applyLinearImpulse(tempVec2, physicsComponent.body.worldCenter, wake = true)
            entity.configure {
                it -= MomentaryForceComponent
            }
        }

        if (physicsComponent.body.linearVelocityY > MAX_FALL_VELOCITY) {
            tempVec2.set(0f, MAX_FALL_VELOCITY - physicsComponent.body.linearVelocityY)
                .mulLocal(physicsComponent.body.getMass()) // so the applied velocity won't depend on mass
            physicsComponent.body.applyLinearImpulse(tempVec2, physicsComponent.body.worldCenter, wake = true)
        }
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
        with (entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(initialPlayerBounds.cx, initialPlayerBounds.cy)
                    gravityScale = GRAVITY_IN_FALL
                }).apply {
                    // physical boundaries
                    createFixture(FixtureDef().apply {
                        isFixedRotation = false
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
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = PLAYER_FOOT_MASK
                            maskBits = PLAYER_LIMB_COLLISIONS
                        }
                        shape = PolygonShape().apply {
                            setAsBox(
                                initialPlayerBounds.width * 0.5f * 0.9f,
                                initialPlayerBounds.height * 0.5f * 0.9f
                            )
                        }
                        userData = entity
                    })
                },
            )
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
                this.userData = userData
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
        contact.get<Entity>()?.let { player ->
            contact.get<Platform>()?.let { platform ->
                if (player[Box2DPhysicsComponent].body.position.y + 0.9f > platform.top) {
                    contact.isEnabled = false
                } else {
                    val moveComponent = player[MoveComponent]
                    if (moveComponent.fallThrough) {
                        contact.isEnabled = false
                        moveComponent.fallThrough = false
                        // prevent ControlsSystem from activating jump the next frame
                        player[JumpComponent].apply {
                            canJumpFromGround = false
                            coyoteTimeInTicks = 0
                            jumpBuffer = JumpComponent.BUFFER_TICKS
                        }
                    } else {
                        contact.isEnabled = true
                    }
                }
            } ?: run {

            }
        }
    }

    private class Platform(/*var isActive: Boolean = true, */val top: Float = 0f)
    private object Water

    companion object {
        private val tempVec2 = Vec2()

        private var SHIFT_INDEX = 0
        private val NEXT_MASK get() = 1 shl SHIFT_INDEX++
        private val WALL_MASK = NEXT_MASK
        private val WATER_MASK = NEXT_MASK
        private val PLAYER_BODY_MASK = NEXT_MASK
        private val PLAYER_FOOT_MASK = NEXT_MASK
        private val PLAYER_CENTER_MASK = NEXT_MASK
        private val PLAYER_HEAD_MASK = NEXT_MASK

        private val PLAYER_BODY_COLLISIONS = WALL_MASK
        private val PLAYER_LIMB_COLLISIONS = WALL_MASK or WATER_MASK

        private inline fun <reified T> Contact.get(): T? =
            getFixtureA()?.userData as? T ?: getFixtureB()?.userData as? T

        private fun Contact.getPlayer(): Entity? =
            // TODO: that might be something else as well
            (getFixtureA()?.userData ?: getFixtureB()?.userData) as? Entity
    }

}