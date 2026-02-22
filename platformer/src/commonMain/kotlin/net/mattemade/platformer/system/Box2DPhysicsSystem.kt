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
import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World as B2dWorld

class Box2DPhysicsSystem(
    //private val physics: B2dWorld = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(Box2DPhysicsComponent, PositionComponent) }, interval = interval), Releasing by Self() {

    private val physics: B2dWorld = B2dWorld().rememberTo {
        var body = it.bodyList
        while (body != null) {
            it.destroyBody(body)
            body = body.getNext()
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
                    createFixture(FixtureDef().apply {
                        friction = 0f
                        shape = PolygonShape().apply {
                            setAsBox(
                                initialPlayerBounds.width * 0.5f * 0.9f,
                                initialPlayerBounds.height * 0.5f * 0.9f
                            )
                        }
                    })
                },
            )
        }
    }

    fun createWall(vertices: Array<Vec2>) {
        physics.createBody(BodyDef()).apply {
            createFixture(FixtureDef().apply {
                shape = ChainShape().apply {
                    createLoop(vertices, vertices.size)
                }
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

    companion object {
        private val tempVec2 = Vec2()
    }

}