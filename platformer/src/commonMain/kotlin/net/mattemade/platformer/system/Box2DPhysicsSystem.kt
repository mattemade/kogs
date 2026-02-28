package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.HALF_PI_F
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
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
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.KnockbackComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.utils.math.NO_ROTATION
import net.mattemade.utils.math.lerp
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.CircleShape
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
import kotlin.math.abs
import kotlin.math.sign
import org.jbox2d.dynamics.World as B2dWorld

class Box2DPhysicsSystem(
    //private val physics: B2dWorld = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(
    family { all(Box2DPhysicsComponent, PositionComponent, RotationComponent, ContextComponent) },
    interval = interval
),
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
        family.forEach { entity ->
            val physicsComponent = entity[Box2DPhysicsComponent]
            val body = physicsComponent.body
            entity[ContextComponent].apply {
                val wasStanding = standing
                standing = body.getContactList().let { it.isTouching<Feet, Wall>() || it.isTouching<Feet, Platform>() } && body.linearVelocityY == 0f
                touchingWalls = body.getContactList().isTouching<Hands, Wall>()

                var currentlySwimming = false
                var currentlyDiving = false
                body.getContactList().touchAll<Torso, Water> { torso, water ->
                    currentlySwimming = true
                    currentlyDiving = currentlyDiving || torso.bodyPosition.y > water.top + 1f
                }

                if (!swimming && currentlySwimming) { // started swimming
                    physicsComponent.landBodyFixture.filterData.maskBits = 0
                    physicsComponent.waterBodyFixture.filterData.maskBits = PLAYER_BODY_COLLISIONS
                    entity[JumpComponent].apply {
                        jumping = false
                        wasJumping = false
                        jumpBuffer = 0
                        coyoteTimeInTicks = JumpComponent.COYOTE_TICKS
                        canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS
                    }
                } else if (swimming && !currentlySwimming) { // finished swimming
                    physicsComponent.waterBodyFixture.filterData.maskBits = 0
                    physicsComponent.landBodyFixture.filterData.maskBits = PLAYER_BODY_COLLISIONS
                    body.setTransformRadians(body.position, 0f)
                    entity[RotationComponent].targetRotation = 0f
                    entity.getOrNull(MoveComponent)?.let {
                        val direction = it.moveDirection
                        // maybe jump a bit from the water if we are moving mostly up?
                        if (direction.y < 0f && abs(direction.y) >= abs(direction.x)) {
                            entity.getOrNull(MomentaryForceComponent)?.let {
                                it.forces += Vec2f(0f, -15f)
                            }
                        }
                    }
                }
                swimming = currentlySwimming

                if (currentlyDiving) {
                    //entity.getOrNull(FloatUpComponent)?.floatUpAcceleration = -0.001f
                } else {
                    entity.getOrNull(FloatUpComponent)?.floatUpAcceleration = 0f
                }

                val knockbackEffect = entity.getOrNull(KnockbackComponent)
                if (knockbackEffect != null) {
                    knockbackEffect.atLeastForTicks -= 1
                    knockbackEffect.ticksToWearOff -= 1
                    knockbackEffect.canStop = knockbackEffect.canStop || (!wasStanding && standing)
                    if (knockbackEffect.canStop && knockbackEffect.atLeastForTicks <= 0 || knockbackEffect.ticksToWearOff <= 0) {
                        entity.configure {
                            it -= KnockbackComponent
                        }
                    }
                }
            }
        }
    }

    override fun onTickEntity(entity: Entity) {
        val context = entity[ContextComponent]
        val physicsComponent = entity[Box2DPhysicsComponent].apply {
            previousPosition.set(
                body.position.x, body.position.y,
            )
        }

        if (context.swimming) {
            waterBasedMovement(physicsComponent, entity)
        } else {
            landBasedMovement(physicsComponent, context, entity)
        }

    }

    private fun waterBasedMovement(
        physicsComponent: Box2DPhysicsComponent,
        entity: Entity
    ) {
        entity.getOrNull(MomentaryForceComponent)?.let {
            it.forces.forEach { force ->
                physicsComponent.body.applyImpulse(force.x, force.y)
            }
            it.forces.clear()
        }
        if (entity.getOrNull(KnockbackComponent) != null) {
            return
        }

        physicsComponent.body.gravityScale = 0f
        entity.getOrNull(MoveComponent)?.let { move ->
            physicsComponent.body.applyImpulse(
                if (move.moveDirection.x != 0f) move.moveDirection.x * move.speed else 0f,
                if (move.moveDirection.y != 0f) move.moveDirection.y * move.speed else 0f,
            )

            if (move.dashDirection.x != 0f || move.dashDirection.y != 0f) {
                // override everything we calculated so far!!
                physicsComponent.body.gravityScale = 0f
                physicsComponent.body.linearVelocityX = move.dashDirection.x
                physicsComponent.body.linearVelocityY = move.dashDirection.y

                entity[RotationComponent].currentRotation =
                    (move.dashDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
                entity[RotationComponent].targetRotation =
                    (move.dashDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
            } else if (move.moveDirection.x != 0f || move.moveDirection.y != 0f) {
                entity[RotationComponent].targetRotation =
                    (move.moveDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
            }
        }

        // movement dampening
        physicsComponent.body.apply {
            if (linearVelocityX != 0f || linearVelocityY != 0f) {
                val rotation = entity[RotationComponent].currentRotation
                setTransformRadians(position, rotation)

                linearVelocityX *= 0.9f
                linearVelocityY *= 0.9f
            }
        }

        entity.getOrNull(FloatUpComponent)?.let { (speed, _) ->
            physicsComponent.body.applyImpulse(0f, speed)
        }
    }

    private fun landBasedMovement(
        physicsComponent: Box2DPhysicsComponent,
        context: ContextComponent,
        entity: Entity
    ) {
        entity.getOrNull(MomentaryForceComponent)?.let {
            it.forces.forEach { force ->
                physicsComponent.body.applyImpulse(force.x, force.y)
            }
            it.forces.clear()
        }
        if (entity.getOrNull(KnockbackComponent) != null) {
            return
        }

        physicsComponent.apply {
            if (context.touchingWalls && (body.linearVelocityY > 0f || context.wallSlide) && body.linearVelocityX == 0f) {
                body.linearVelocityY = 1f
                context.wallSlide = true
            } else if (context.wallSlide) {
                entity[JumpComponent].apply {
                    coyoteTimeInTicks =
                        JumpComponent.COYOTE_TICKS // just to allow jump off the wall without using double jump
                    wasJumping = true // just to force applying lower gravity
                }
                context.wallSlide = false
                body.isAwake = true
            }
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
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
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

                // underwater body
                waterBodyFixture = body.createFixture(FixtureDef().apply {
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = PLAYER_BODY_MASK
                        maskBits = 0
                    }
                    shape = CircleShape(radius = initialPlayerBounds.width * 0.45f)
                    userData = entity
                })!!

                body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = PLAYER_FOOT_MASK
                        maskBits = PLAYER_LIMB_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            initialPlayerBounds.width * 0.5f * 0.8f, // a bit shorter that body width
                            0.1f, // just a tiny block at the bottom
                            center = Vec2(0f, initialPlayerBounds.height * 0.5f),
                            angle = Angle.ZERO
                        )
                    }
                    userData = Feet(entity)
                })!!
                body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = PLAYER_TORSO_MASK
                        maskBits = PLAYER_LIMB_COLLISIONS
                    }
                    shape = CircleShape(radius = initialPlayerBounds.width * 0.3f)
                    userData = Torso(body.position)
                })!!
            }
        }
    }

    fun createCrabBody(entityCreateContext: EntityCreateContext, entity: Entity, x: Float, y: Float, width: Float, height: Float,) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.STATIC
                    position.set(x, y)
                    gravityScale = GRAVITY_IN_FALL
                }).apply {
                    isFixedRotation = false
                },
            ).apply {
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = ENEMY_BODY_MASK
                        maskBits = ENEMY_BODY_COLLISION
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            width * 0.48f,
                            height * 0.48f
                        )
                    }
                    userData = Hazard(1f, body.position)
                })!!
                waterBodyFixture = landBodyFixture
            }
        }
    }

    // free-shape chains of solid surface
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

    // horizontal lines of solid surface
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

    // vertical narrow "stripes"
    fun createWater(fromY: Float, toY: Float, x: Float) {
        val hy = (toY - fromY) * 0.5f
        physics.createBody(BodyDef()).apply {
            position.set(x + 0.5f, fromY + hy)
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = WATER_MASK
                }
                shape = PolygonShape().apply {
                    setAsBox(0.3f, hy)
                }
                userData = Water(fromY)
            })
        }

        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = WATER_MASK
                }
                shape = PolygonShape().apply {
                    //createLoop(vertices, vertices.size)
                }
                userData = Platform()
            })
        }
    }

    fun teleport(entity: Entity, moveToPosition: Vec2f, physicsComponentFromPreviousRoom: Box2DPhysicsComponent) {
        entity[Box2DPhysicsComponent].apply {
            previousPosition.set(moveToPosition)
            body.setTransformDegrees(Vec2(moveToPosition.x, moveToPosition.y), 0f)
            body.linearVelocityX = physicsComponentFromPreviousRoom.body.linearVelocityX
            body.linearVelocityY = physicsComponentFromPreviousRoom.body.linearVelocityY

            if (entity[ContextComponent].swimming) { // was swimming when teleported
                landBodyFixture.filterData.maskBits = 0
                waterBodyFixture.filterData.maskBits = PLAYER_BODY_COLLISIONS
            } else { // walked in
                waterBodyFixture.filterData.maskBits = 0
                landBodyFixture.filterData.maskBits = PLAYER_BODY_COLLISIONS
            }
        }
    }

    override fun beginContact(contact: Contact) {
        contact.with<Entity> { other ->
            when (other) {
                is Hazard -> {
                    if (contact.isTouching) {
                        if (this.getOrNull(KnockbackComponent) == null) {
                            this.configure {
                                it += KnockbackComponent()
                            }
                            println("DAMAGE: ${other.damage}")
                            val body = this[Box2DPhysicsComponent].body
                            body.linearVelocityY = 0f
                            body.linearVelocityX = 0f
                            val position = body.position
                            if (this[ContextComponent].swimming) {
                                tempVec2f.set(position.x - other.bodyPosition.x, position.y - other.bodyPosition.y).setLength(10f)
                                this[MomentaryForceComponent].forces += Vec2f(
                                    tempVec2f.x,
                                    tempVec2f.y,
                                )
                            } else {
                                this[MomentaryForceComponent].forces += Vec2f(
                                    10f * sign(position.x - other.bodyPosition.x),
                                    -10f
                                )
                            }
                        }
                    }
                }
            }
        }
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
    private class Water(val top: Float = 0f)
    private object Wall
    private class Feet(val entity: Entity)
    private class Torso(val bodyPosition: Vec2)
    private class Hands(val entity: Entity)
    private class Hazard(val damage: Float, val bodyPosition: Vec2)

    companion object {
        private val tempVec2 = Vec2()
        private val tempVec2f = MutableVec2f()

        private var SHIFT_INDEX = 0
        private val NEXT_MASK get() = 1 shl SHIFT_INDEX++
        private val WALL_MASK = NEXT_MASK

        //private val PLATFORM_MASK = NEXT_MASK
        private val WATER_MASK = NEXT_MASK
        private val PLAYER_BODY_MASK = NEXT_MASK
        private val PLAYER_FOOT_MASK = NEXT_MASK
        private val PLAYER_TORSO_MASK = NEXT_MASK
        private val PLAYER_HEAD_MASK = NEXT_MASK
        private val PLAYER_HANDS_MASK = NEXT_MASK

        private val ENEMY_BODY_MASK = NEXT_MASK

        private val PLAYER_BODY_COLLISIONS = WALL_MASK or ENEMY_BODY_MASK
        private val PLAYER_LIMB_COLLISIONS = WALL_MASK or WATER_MASK
        private val ENEMY_BODY_COLLISION = WALL_MASK or PLAYER_BODY_MASK or PLAYER_TORSO_MASK

        private inline fun <reified T> Contact.with(crossinline action: T.(Any?) -> Unit) =
            (getFixtureA()?.userData as? T)?.action(getFixtureB()?.userData) ?: (getFixtureB()?.userData as? T)?.action(
                getFixtureA()?.userData
            )

        private inline fun <reified T, reified K> ContactEdge?.isTouching(): Boolean = this.touch<T, K>() != null

        private inline fun <reified T, reified K> ContactEdge?.touch(): Contact? {
            var edge = this
            while (edge != null) {
                edge.contact?.let {
                    if (it.isTouching && (
                                (it.getFixtureA()?.userData is T && it.getFixtureB()?.userData is K)
                                        || (it.getFixtureB()?.userData is T && it.getFixtureA()?.userData is K)
                                )
                    ) {
                        return it
                    }
                }
                edge = edge.next
            }
            return null
        }

        private inline fun <reified T, reified K> ContactEdge?.touchAll(crossinline action: (T, K) -> Unit) {
            var edge = this
            while (edge != null) {
                edge.contact?.let {
                    if (it.isTouching) {
                        (it.getFixtureA()?.userData as? T ?: it.getFixtureB()?.userData as? T)?.let { left ->
                            (it.getFixtureA()?.userData as? K ?: it.getFixtureB()?.userData as? K)?.let { right ->
                                action(left, right)
                            }
                        }
                    }
                }
                edge = edge.next
            }
        }

    }

}