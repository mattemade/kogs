package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.math.HALF_PI_F
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.soywiz.korma.geom.Angle
import net.mattemade.fmod.FMOD
import net.mattemade.platformer.GRAVITY_IN_FALL
import net.mattemade.platformer.GRAVITY_IN_JUMP
import net.mattemade.platformer.GRAVITY_IN_JUMPFALL
import net.mattemade.platformer.JUMP_VELOCITY
import net.mattemade.platformer.MAX_FALL_VELOCITY
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.EnemyComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.InvincibilityComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.KnockbackComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PlayerComponent
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
    private val spawnPlayerAttack: (x: Float, y: Float, vx: Float, vy: Float, damage: Float) -> Unit,
    private val roomSize: Vec2f,
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(
    family { all(Box2DPhysicsComponent, PositionComponent, RotationComponent, ContextComponent) }, interval = interval
), ContactListener, Releasing by Self() {

    private val landVelocity by lazy { gameContext.fmodAssets.land.getParameterDescriptionByName("Velocity").id }

    private val physics: B2dWorld = B2dWorld().rememberTo {
        var body = it.bodyList
        while (body != null) {
            var fixture = body.getFixtureList()
            while (fixture != null) {
                fixture.userData = null
                fixture = fixture.getNext()
            }
            body.userData = null
            it.destroyBody(body)
            body = body.getNext()
        }
    }.also {
        it.setContactListener(this)

        it.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = ROOM_BOUNDS_MASK
                }
                shape = ChainShape().apply {
                    createLoop(
                        arrayOf(
                            Vec2(0f, 0f),
                            Vec2(roomSize.x, 0f),
                            Vec2(roomSize.x, roomSize.y),
                            Vec2(0f, roomSize.y),
                        ), 4
                    )
                }
                userData = Wall // but only for enemies, by collision bits
            })
        }
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
                standingLeftFoot = body.getContactList()
                    .let { it.isTouching<LeftFoot, Wall>() || it.isTouching<LeftFoot, Platform>() || it.isTouching<LeftFoot, Spike>() || it.isTouching<LeftFoot, EnemyHazard>() }
                standingRightFoot = body.getContactList()
                    .let { it.isTouching<RightFoot, Wall>() || it.isTouching<RightFoot, Platform>() || it.isTouching<RightFoot, Spike>() || it.isTouching<RightFoot, EnemyHazard>() }

                standing = (standingLeftFoot || standingRightFoot) && body.linearVelocityY == 0f
                touchingLeftWall = body.getContactList()
                    .let { it.isTouching<LeftHand, Wall>() || it.isTouching<LeftHand, EnemyHazard>() } // enemies should feel each other
                touchingRightWall = body.getContactList()
                    .let { it.isTouching<RightHand, Wall>() || it.isTouching<RightHand, EnemyHazard>() } // enemies should feel each other

                if (standing && physicsComponent.previousVelocity.y != 0f) {
                    physicsComponent.playSound(gameContext.fmodAssets.land)
                        .setParameterByID(landVelocity, physicsComponent.previousVelocity.y, 0)
                }

                var currentlySwimming = false
                var currentlyDiving = false
                body.getContactList().touchAll<Torso, Water> { torso, water ->
                    currentlySwimming = true
                    currentlyDiving = currentlyDiving || torso.bodyPosition.y > water.top + 1f
                }

                if (!swimming && currentlySwimming) { // started swimming
                    if (entity.getOrNull(PlayerComponent) != null) {
                        physicsComponent.playSound(gameContext.fmodAssets.getInWater)
                    }
                    physicsComponent.landBodyFixture.filterData.maskBits = 0
                    physicsComponent.waterBodyFixture.filterData.maskBits = physicsComponent.collisionMask
                    entity[JumpComponent].apply {
                        jumping = false
                        wasJumping = false
                        jumpBuffer = 0
                        coyoteTimeInTicks = JumpComponent.COYOTE_TICKS
                        canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS
                    }/*entity.getOrNull(MoveComponent)?.let {
                        it.forceStopWaterDash = true // do not continue dashing between substances
                    }*/
                } else if (swimming && !currentlySwimming) { // finished swimming
                    if (entity.getOrNull(PlayerComponent) != null) {
                        physicsComponent.playSound(gameContext.fmodAssets.getOutOfWater)
                        swimmingSound?.let { sound ->
                            sound.stop(FMOD.FMOD_STUDIO_STOP_ALLOWFADEOUT)
                            sound.release()
                            entity[Box2DPhysicsComponent].attachedSounds.removeAll { it.first === sound }
                            swimmingSound = null
                        }
                    }
                    physicsComponent.waterBodyFixture.filterData.maskBits = 0
                    physicsComponent.landBodyFixture.filterData.maskBits = physicsComponent.collisionMask
                    body.setTransformRadians(body.position, 0f)
                    entity[RotationComponent].targetRotation = 0f
                    entity.getOrNull(MoveComponent)?.let {
                        val direction = it.moveDirection
                        // maybe jump a bit from the water if we are moving mostly up?
                        val movingVertically = abs(direction.y) >= abs(direction.x)
                        if (direction.y < 0f && movingVertically) {
                            entity.getOrNull(MomentaryForceComponent)?.let {
                                it.forces += Vec2f(0f, -15f)
                            }
                            //tempVec2f.set(body.position.x, body.position.y - 3f)
                            //teleport(entity, tempVec2f, physicsComponent)
                        }
                        if (direction.x != 0f) {
                            entity[JumpComponent].apply {
                                coyoteTimeInTicks = if (gameContext.gameState.waterPearl) {
                                    JumpComponent.COYOTE_TICKS
                                } else {
                                    0 // do not allow to jump up the waterfall without the pearl!!
                                }
                            }
                        }
                        if (movingVertically) {
                            it.forceStopAirDash = true // do not continue dashing in the air when jumping up or down
                        }
                    }

                }
                swimming = currentlySwimming

                if (currentlyDiving && !gameContext.gameState.waterPearl) {
                    entity.getOrNull(FloatUpComponent)?.floatUpAcceleration = -0.001f
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

                body.getContactList().touchAll<Entity, Spike> { entity, spike ->
                    if (entity.getOrNull(InvincibilityComponent) == null) {
                        physicsComponent.playSound(gameContext.fmodAssets.damaged)
                        entity.getOrNull(HealthComponent)?.let {
                            it.health -= 1f
                        }
                        val body = entity[Box2DPhysicsComponent].body
                        val context = entity[ContextComponent]
                        if (context.swimming) {
                            tempVec2f.set(0f, -10f).rotate(entity[RotationComponent].currentRotation.radians)
                                .add(body.position.x, body.position.y)
                        } else {
                            tempVec2f.set(body.position.x + if (context.facingRight) 1f else -1f, body.position.y)
                        }
                        applyKnockback(entity, tempVec2f.x, tempVec2f.y)
                        entity.configure {
                            it += InvincibilityComponent()
                        }
                    }
                }
                body.getContactList().touchAll<Entity, EnemyHazard> { entity, hazard ->
                    if (entity.getOrNull(InvincibilityComponent) == null) {
                        physicsComponent.playSound(gameContext.fmodAssets.damaged)
                        entity.getOrNull(HealthComponent)?.let {
                            it.health -= hazard.damage
                        }

                        applyKnockback(entity, hazard.bodyPosition.x, hazard.bodyPosition.y)
                        entity.configure {
                            it += InvincibilityComponent()
                        }
                    }
                }
            }
            physicsComponent.attachedSounds.removeAll { (sound, attributes) ->
                val shouldBeRemoved = sound.getPlaybackState() == FMOD.STUDIO_PLAYBACK_STOPPED
                if (shouldBeRemoved) {
                    sound.release()
                } else {
                    attributes.position.apply { x = body.position.x; y = body.position.y; }
                    attributes.velocity.apply { x = body.linearVelocityY; y = body.linearVelocityY; }
                    sound.set3DAttributes(attributes)
                }

                shouldBeRemoved
            }
        }
    }

    override fun onTickEntity(entity: Entity) {
        val context = entity[ContextComponent]
        val physicsComponent = entity[Box2DPhysicsComponent].apply {
            previousVelocity.set(body.linearVelocityX, body.linearVelocityY)
            previousPosition.set(
                body.position.x, body.position.y,
            )
        }

        if (context.swimming) {
            waterBasedMovement(physicsComponent, context, entity)
        } else {
            landBasedMovement(physicsComponent, context, entity)
        }

        entity.getOrNull(AttackComponent)?.let {
            if (it.requestingPhysicsToSpawnAttack > 0f) {
                val angle = entity[RotationComponent].currentRotation.radians
                if (context.swimming) {
                    tempVec2f.set(0f, -1.5f).rotate(angle)
                } else if (context.facingRight) {
                    tempVec2f.set(1f, 0f)
                } else {
                    tempVec2f.set(-1f, 0f)
                }
                tempVec2f.add(physicsComponent.body.position.x, physicsComponent.body.position.y)
                spawnPlayerAttack(
                    tempVec2f.x,
                    tempVec2f.y,
                    physicsComponent.body.linearVelocityX,
                    if (context.swimming) physicsComponent.body.linearVelocityY else 0f,
                    it.requestingPhysicsToSpawnAttack
                )
                it.requestingPhysicsToSpawnAttack = 0f
            }
        }

    }

    private fun waterBasedMovement(
        physicsComponent: Box2DPhysicsComponent, context: ContextComponent, entity: Entity
    ) {
        if (entity.getOrNull(KnockbackComponent) != null) {
            applyMomentaryForces(entity, physicsComponent) // they are also applied in the end of normal routine!
            return
        }

        physicsComponent.body.gravityScale = 0f
        val rotationComponent = entity[RotationComponent]
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

                rotationComponent.apply {
                    if (!fixedRotation) {
                        currentRotation = (move.dashDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
                        targetRotation = (move.dashDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
                    }
                }
            } else if (move.moveDirection.x != 0f || move.moveDirection.y != 0f) {
                rotationComponent.apply {
                    if (!fixedRotation) {
                        targetRotation = (move.moveDirection.angleTo(NO_ROTATION).radians + HALF_PI_F + PI2_F) % PI2_F
                    }
                }
            }

            if (rotationComponent.fixedRotation) {
                if (move.moveDirection.x > 0f || move.dashDirection.x > 0f) {
                    context.facingRight = true
                } else if (move.moveDirection.x < 0f || move.dashDirection.x < 0f) {
                    context.facingRight = false
                }
            }
        }

        // movement dampening
        physicsComponent.body.apply {
            if (linearVelocityX != 0f || linearVelocityY != 0f) {
                if (!rotationComponent.fixedRotation) {
                    val rotation = entity[RotationComponent].currentRotation
                    setTransformRadians(position, rotation)
                }

                linearVelocityX *= 0.9f
                linearVelocityY *= 0.9f
            }
        }

        entity.getOrNull(FloatUpComponent)?.let { (speed, _) ->
            physicsComponent.body.applyImpulse(0f, speed)
        }
        applyMomentaryForces(entity, physicsComponent)
    }

    private fun landBasedMovement(
        physicsComponent: Box2DPhysicsComponent, context: ContextComponent, entity: Entity
    ) {
        if (entity.getOrNull(KnockbackComponent) != null) {
            applyMomentaryForces(entity, physicsComponent) // they are also applied in the end of normal routine!
            return
        }

        entity.getOrNull(MoveComponent)?.let {
            physicsComponent.apply {
                entity.getOrNull(PlayerComponent)?.let { _ -> // only player can wall slide
                    val movingToWall =
                        (context.touchingLeftWall && (it.dashDirection.x < 0f || it.moveDirection.x < 0f || context.wallSlide)) || (context.touchingRightWall && (it.dashDirection.x > 0f || it.moveDirection.x > 0f || context.wallSlide))
                    val dashingToWall =
                        (context.touchingLeftWall && it.dashDirection.x < 0f) || (context.touchingRightWall && it.dashDirection.x > 0f)
                    val dashingFromWall =
                        (context.touchingLeftWall && it.dashDirection.x > 0f) || (context.touchingRightWall && it.dashDirection.x < 0f)
                    if (movingToWall && !dashingFromWall && (body.linearVelocityY > 0f || dashingToWall) && body.linearVelocityX == 0f) {
                        if (gameContext.gameState.airPearl) {
                            body.linearVelocityY = 1f
                            context.wallSlide = true
                            if (context.slidingSound == null) {
                                context.slidingSound =
                                    physicsComponent.playSoundAttached(gameContext.fmodAssets.wallSlideLoop)
                            }
                            if (dashingFromWall) {
                                it.forceStopAirDash = true
                            }
                        }
                    } else if (context.wallSlide) {
                        entity[JumpComponent].apply {
                            coyoteTimeInTicks =
                                JumpComponent.COYOTE_TICKS // just to allow jump off the wall without using double jump
                            wasJumping = true // just to force applying lower gravity
                        }
                        context.wallSlide = false
                        context.slidingSound?.apply {
                            physicsComponent.attachedSounds.removeAll { it.first === this }
                            stop(FMOD.FMOD_STUDIO_STOP_ALLOWFADEOUT)
                            release()
                            context.slidingSound = null
                        }
                        body.isAwake = true
                    }
                }
            }

            if (it.moveDirection.x > 0f || it.dashDirection.x > 0f) {
                context.facingRight = true
            } else if (it.moveDirection.x < 0f || it.dashDirection.x < 0f) {
                context.facingRight = false
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
                canJumpInAir = if (gameContext.gameState.airPearl) JumpComponent.MAX_AIR_JUMPS else 0
                wasJumping = false
            } else {
                coyoteTimeInTicks--
                canJumpFromGround = coyoteTimeInTicks > 0
            }
            physicsComponent.body.gravityScale = physicsComponent.gravityScaleOverride ?: when {
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

            if (!context.wallSlide && (move.dashDirection.x != 0f || move.dashDirection.y != 0f)) {
                // override everything we calculated so far!!
                physicsComponent.body.gravityScale = 0f
                physicsComponent.body.linearVelocityX = move.dashDirection.x
                physicsComponent.body.linearVelocityY = move.dashDirection.y
            }
        }

        if (physicsComponent.body.linearVelocityY > MAX_FALL_VELOCITY) {
            physicsComponent.body.applyImpulse(0f, MAX_FALL_VELOCITY - physicsComponent.body.linearVelocityY)
        }
        applyMomentaryForces(entity, physicsComponent) // they are also applied in the end of normal routine!
    }

    private fun applyMomentaryForces(
        entity: Entity, physicsComponent: Box2DPhysicsComponent
    ) {
        entity.getOrNull(MomentaryForceComponent)?.let {
            it.forces.forEach { force ->
                physicsComponent.body.applyImpulse(force.x, force.y)
            }
            it.forces.clear()
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
                        shape = CircleShape(0.25f).apply { p.set(-0.5f, 0f) }
                        userData = LeftHand(entity)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = PLAYER_HANDS_MASK
                            maskBits = PLAYER_LIMB_COLLISIONS
                        }
                        shape = CircleShape(0.25f).apply { p.set(0.5f, 0f) }
                        userData = RightHand(entity)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = PLAYER_HITBOX_MASK
                            maskBits = PLAYER_HITBOX_COLLISIONS
                        }
                        shape = PolygonShape().apply {
                            setAsBox( // SMALLER THAN THE MAIN BODY!!!
                                initialPlayerBounds.width * 0.5f * 0.4f, initialPlayerBounds.height * 0.5f * 0.7f
                            )
                        }
                        userData = entity
                    })
                },
                collisionMask = PLAYER_BODY_COLLISIONS,
            ).apply {
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = PLAYER_BODY_MASK
                        maskBits = PLAYER_BODY_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox( // SAME AS THE HITBOX!!!
                            initialPlayerBounds.width * 0.5f * 0.9f, initialPlayerBounds.height * 0.5f * 0.9f
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
                            initialPlayerBounds.width * 0.25f * 0.8f, // a bit shorter that body halfwidth
                            0.1f, // just a tiny block at the bottom
                            center = Vec2(-initialPlayerBounds.width * 0.25f, initialPlayerBounds.height * 0.5f),
                            angle = Angle.ZERO
                        )
                    }
                    userData = LeftFoot(entity)
                })!!
                body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = PLAYER_FOOT_MASK
                        maskBits = PLAYER_LIMB_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            initialPlayerBounds.width * 0.25f * 0.8f, // a bit shorter that body halfwidth
                            0.1f, // just a tiny block at the bottom
                            center = Vec2(initialPlayerBounds.width * 0.25f, initialPlayerBounds.height * 0.5f),
                            angle = Angle.ZERO
                        )
                    }
                    userData = RightFoot(entity)
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

    fun createPlayerAttackBody(
        entityCreateContext: EntityCreateContext,
        entity: Entity,
        x: Float,
        y: Float,
        radius: Float,
        damage: Float,
    ) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(x, y)
                    gravityScale = 0f
                }),
                collisionMask = PLAYER_ATTACK_COLLISIONS,
            ).apply {
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    isSensor = true
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = PLAYER_ATTACK_MASK
                        maskBits = PLAYER_ATTACK_COLLISIONS
                    }
                    shape = CircleShape(radius = radius)
                    userData = PlayerAttack(damage, x, y)
                })!!
                waterBodyFixture = landBodyFixture
            }
        }
    }

    fun createEnemyBody(
        entityCreateContext: EntityCreateContext,
        entity: Entity,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(x, y)
                    gravityScale = GRAVITY_IN_FALL
                }).apply {
                    isFixedRotation = false

                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_HANDS_MASK
                            maskBits = ENEMY_LIBS_COLLISIONS
                        }
                        shape = CircleShape(minOf(width, height) * 0.25f).apply { p.set(-width * 0.5f, 0f) }
                        userData = LeftHand(entity)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_HANDS_MASK
                            maskBits = ENEMY_LIBS_COLLISIONS
                        }
                        shape = CircleShape(minOf(width, height) * 0.25f).apply { p.set(width * 0.5f, 0f) }
                        userData = RightHand(entity)
                    })

                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_FOOT_MASK
                            maskBits = ENEMY_LIBS_COLLISIONS
                        }
                        shape = PolygonShape().apply {
                            setAsBox(
                                width * 0.25f * 0.8f, 0.1f, // just a tiny block at the bottom
                                center = Vec2(-width * 0.25f, height * 0.5f), angle = Angle.ZERO
                            )
                        }
                        userData = LeftFoot(entity)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_FOOT_MASK
                            maskBits = ENEMY_LIBS_COLLISIONS
                        }
                        shape = PolygonShape().apply {
                            setAsBox(
                                width * 0.25f * 0.8f, 0.1f, // just a tiny block at the bottom
                                center = Vec2(width * 0.5f, height * 0.5f), angle = Angle.ZERO
                            )
                        }
                        userData = RightFoot(entity)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_TORSO_MASK
                            maskBits = ENEMY_LIBS_COLLISIONS
                        }
                        shape = PolygonShape().apply { setAsBox(width * 0.5f, height * 0.5f) }
                        userData = Torso(position)
                    })
                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_VISION_MASK
                            maskBits = ENEMY_PLAYER_DETECTOR_COLLISIONS
                        }
                        shape = CircleShape(5f)
                        userData = Action(onTouch = {
                            entity[EnemyComponent].spottedPlayerPosition = it[Box2DPhysicsComponent].body.position
                        })
                    })

                    createFixture(FixtureDef().apply {
                        isSensor = true
                        filter = Filter().apply {
                            categoryBits = ENEMY_PROXIMITY_MASK
                            maskBits = ENEMY_PLAYER_DETECTOR_COLLISIONS
                        }
                        shape = CircleShape(2f)
                        userData = Action(onTouch = {
                            entity[EnemyComponent].nearPlayer = it[Box2DPhysicsComponent].body.position
                        })
                    })
                },
                collisionMask = ENEMY_BODY_COLLISION,
            ).apply {
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    friction = 0f
                    filter = Filter().apply {
                        categoryBits = ENEMY_BODY_MASK
                        maskBits = ENEMY_BODY_COLLISION
                    }
                    shape = PolygonShape().apply { setAsBox(width * 0.5f, height * 0.5f) }
                    userData = EnemyHazard(1f, body.position, entity)
                })!!
                waterBodyFixture = landBodyFixture
            }
        }
    }

    fun createCheckpoint(
        entityCreateContext: EntityCreateContext,
        entity: Entity,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        id: Int,
        onTouch: () -> Unit
    ) {
        entityCreateContext.apply {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.STATIC
                    position.set(x, y)
                }), collisionMask = CHECKPOINT_COLLISIONS
            ).apply {
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = CHECKPOINT_MASK
                        maskBits = CHECKPOINT_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            width * 0.48f, height * 0.48f
                        )
                    }
                    userData = Checkpoint(id, onTouch)
                })!!
                waterBodyFixture = landBodyFixture
            }
        }

    }

    fun createTriggerBody(
        entityCreateContext: EntityCreateContext,
        entity: Entity,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        onTouch: (otherEntity: Entity) -> Unit = {},
        onExit: (otherEntity: Entity) -> Unit = {},
    ) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(x, y)
                    gravityScale = 0f
                }).apply {
                    isFixedRotation = false
                },
                collisionMask = PICKUP_COLLISIONS,
                gravityScaleOverride = 0f,
            ).apply {
                // land body
                landBodyFixture = body.createFixture(FixtureDef().apply {
                    isSensor = true
                    filter = Filter().apply {
                        categoryBits = PICKUP_MASK
                        maskBits = PICKUP_COLLISIONS
                    }
                    shape = PolygonShape().apply {
                        setAsBox(
                            width * 0.48f, height * 0.48f
                        )
                    }
                    userData = Action(onTouch = { onTouch(it) }, onExit = { onExit(it) })
                })!!
                waterBodyFixture = landBodyFixture
            }
        }
    }


    fun createTemporarySpike(
        entityCreateContext: EntityCreateContext, entity: Entity,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ) {
        with(entityCreateContext) {
            entity += Box2DPhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    position.set(x + width * 0.5f, y + height * 0.5f)
                    fixedRotation = true
                }).apply {
                    createFixture(FixtureDef().apply {
                        filter = Filter().apply {
                            categoryBits = WALL_MASK
                        }
                        shape = PolygonShape().apply {
                            setAsBox(width * 0.5f, height * 0.5f)
                        }
                        this.userData = Wall
                    })
                },
                collisionMask = WALL_MASK,
            )
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

    fun createSpike(vertices: Array<Vec2>) {
        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                filter = Filter().apply {
                    categoryBits = SPIKE_MASK
                }
                shape = ChainShape().apply {
                    createLoop(vertices, vertices.size)
                }
                this.userData = Spike
            })
        }
    }

    fun teleport(entity: Entity, moveToPosition: Vec2f, physicsComponentFromPreviousRoom: Box2DPhysicsComponent) {
        entity[Box2DPhysicsComponent].apply {
            previousPosition.set(moveToPosition)
            body.setTransformDegrees(Vec2(moveToPosition.x, moveToPosition.y), 0f)
            body.linearVelocityX = physicsComponentFromPreviousRoom.body.linearVelocityX
            body.linearVelocityY = physicsComponentFromPreviousRoom.body.linearVelocityY
            attachedSounds.clear()
            attachedSounds.addAll(physicsComponentFromPreviousRoom.attachedSounds)

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
        contact.with<PlayerAttack> { other ->
            if (contact.isTouching) {
                when (other) {
                    is EnemyHazard -> {
                        other.entity.configure {
                            it += KnockbackComponent(atLeastForTicks = 20, ticksToWearOff = 40)
                        }
                        other.entity.getOrNull(HealthComponent)?.let {
                            it.health -= this.damage
                            if (it.health > 0f) {
                                other.entity[Box2DPhysicsComponent].playSound(gameContext.fmodAssets.hitEnemy)
                            } else {
                                other.entity[Box2DPhysicsComponent].playSound(gameContext.fmodAssets.enemyDefeated)
                            }
                        }
                        val body = other.entity[Box2DPhysicsComponent].body
                        body.linearVelocityY = 0f
                        body.linearVelocityX = 0f
                        val position = body.position
                        if (other.entity[ContextComponent].swimming) {
                            tempVec2f.set(position.x - other.bodyPosition.x, position.y - other.bodyPosition.y)
                                .setLength(5f)
                            other.entity[MomentaryForceComponent].forces += Vec2f(
                                tempVec2f.x,
                                tempVec2f.y,
                            )
                        } else {
                            other.entity[MomentaryForceComponent].forces += Vec2f(
                                7f * sign(position.x - this.x), -7f
                            )
                        }
                    }
                }
            }
        }
        contact.with<Entity> { other ->
            if (contact.isTouching) {
                when (other) {
                    is Spike -> { /* no-op, since it would allow to walk on spikes after, as they won't trigger beginContact anymore */
                    }

                    is Checkpoint -> {
                        this.getOrNull(HealthComponent)?.apply {
                            health = maxHealth
                        }
                        other.onTouch()
                    }

                    is Action -> other.onTouch(this)
                    is EnemyHazard -> {/* no-op, since it would allow to walk on spikes after, as they won't trigger beginContact anymore */
                    }
                }
            }

        }
    }

    private fun applyKnockback(
        entity: Entity,
        fromX: Float,
        fromY: Float,
    ) {
        entity.configure {
            it += KnockbackComponent()
        }
        val body = entity[Box2DPhysicsComponent].body
        //body.gravityScale = GRAVITY_IN_FALL
        body.linearVelocityY = 0f
        body.linearVelocityX = 0f
        val position = body.position
        if (entity[ContextComponent].swimming) {
            tempVec2f.set(position.x - fromX, position.y - fromY).setLength(10f)
            entity[MomentaryForceComponent].forces += Vec2f(
                tempVec2f.x,
                tempVec2f.y,
            )
        } else {
            entity[MomentaryForceComponent].forces += Vec2f(
                10f * sign(position.x - fromX), -10f
            )
        }
    }

    override fun endContact(contact: Contact) {
        contact.with<Entity> { other ->
            when (other) {
                is Action -> other.onExit?.invoke(this)
            }
        }
    }

    override fun postSolve(
        contact: Contact, impulse: ContactImpulse
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
    private object Spike
    private class LeftFoot(val entity: Entity)
    private class RightFoot(val entity: Entity)
    private class Torso(val bodyPosition: Vec2)
    private class LeftHand(val entity: Entity)
    private class RightHand(val entity: Entity)
    private class EnemyHazard(val damage: Float, val bodyPosition: Vec2, val entity: Entity)
    private class PlayerAttack(val damage: Float, val x: Float, val y: Float)
    private class Checkpoint(val id: Int, val onTouch: () -> Unit)
    private class Action(
        val onTouch: (otherEntity: Entity) -> Unit, val onExit: ((otherEntity: Entity) -> Unit)? = null
    )

    companion object {
        private val tempVec2 = Vec2()
        private val tempVec2f = MutableVec2f()

        private var SHIFT_INDEX = 0
        private val NEXT_MASK get() = 1 shl SHIFT_INDEX++
        private val WALL_MASK = NEXT_MASK
        private val ROOM_BOUNDS_MASK = NEXT_MASK
        private val SPIKE_MASK = NEXT_MASK

        //private val PLATFORM_MASK = NEXT_MASK
        private val WATER_MASK = NEXT_MASK
        private val PLAYER_BODY_MASK = NEXT_MASK
        private val PLAYER_FOOT_MASK = NEXT_MASK
        private val PLAYER_TORSO_MASK = NEXT_MASK
        private val PLAYER_HANDS_MASK = NEXT_MASK
        private val PLAYER_HITBOX_MASK = NEXT_MASK

        private val ENEMY_BODY_MASK = NEXT_MASK
        private val ENEMY_FOOT_MASK = NEXT_MASK
        private val ENEMY_HANDS_MASK = NEXT_MASK
        private val ENEMY_TORSO_MASK = NEXT_MASK
        private val PLAYER_ATTACK_MASK = NEXT_MASK
        private val ENEMY_VISION_MASK = NEXT_MASK
        private val ENEMY_PROXIMITY_MASK = NEXT_MASK

        private val CHECKPOINT_MASK = NEXT_MASK

        private val PICKUP_MASK = NEXT_MASK

        private val PLAYER_BODY_COLLISIONS =
            WALL_MASK or CHECKPOINT_MASK or PICKUP_MASK or SPIKE_MASK or ENEMY_VISION_MASK or ENEMY_PROXIMITY_MASK
        private val PLAYER_HITBOX_COLLISIONS = ENEMY_BODY_MASK or SPIKE_MASK
        private val PLAYER_LIMB_COLLISIONS = WALL_MASK or WATER_MASK or SPIKE_MASK or ENEMY_BODY_MASK
        private val ENEMY_BODY_COLLISION =
            WALL_MASK or ROOM_BOUNDS_MASK or PLAYER_HITBOX_MASK or PLAYER_TORSO_MASK or PLAYER_ATTACK_MASK or ENEMY_BODY_MASK or SPIKE_MASK or PLAYER_FOOT_MASK or ENEMY_HANDS_MASK
        private val ENEMY_LIBS_COLLISIONS = WALL_MASK or ROOM_BOUNDS_MASK or WATER_MASK or SPIKE_MASK or ENEMY_BODY_MASK
        private val CHECKPOINT_COLLISIONS = PLAYER_BODY_MASK
        private val PICKUP_COLLISIONS = PLAYER_BODY_MASK
        private val PLAYER_ATTACK_COLLISIONS = ENEMY_BODY_MASK
        private val ENEMY_PLAYER_DETECTOR_COLLISIONS = PLAYER_BODY_MASK

        private inline fun <reified T> Contact.with(crossinline action: T.(Any?) -> Unit) =
            (getFixtureA()?.userData as? T)?.action(getFixtureB()?.userData) ?: (getFixtureB()?.userData as? T)?.action(
                getFixtureA()?.userData
            )

        private inline fun <reified T, reified K> ContactEdge?.isTouching(): Boolean = this.touch<T, K>() != null

        private inline fun <reified T, reified K> ContactEdge?.touch(): Contact? {
            var edge = this
            while (edge != null) {
                edge.contact?.let {
                    if (it.isTouching && ((it.getFixtureA()?.userData is T && it.getFixtureB()?.userData is K) || (it.getFixtureB()?.userData is T && it.getFixtureA()?.userData is K))) {
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