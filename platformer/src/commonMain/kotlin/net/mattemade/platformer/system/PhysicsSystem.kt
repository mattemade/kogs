package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PhysicsComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.utils.math.lerp
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World as B2dWorld

class PhysicsSystem(
    private val physics: B2dWorld = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(PhysicsComponent, PositionComponent) }, interval = interval) {

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
        val physicsComponent = entity[PhysicsComponent].apply {
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
                if (canHoldJumpForTicks-- > 0) {
                    tempVec2.set(0f, JUMP_VELOCITY - physicsComponent.body.linearVelocityY)
                        .mulLocal(physicsComponent.body.getMass()) // so the applied velocity won't depend on mass
                    physicsComponent.body.applyLinearImpulse(tempVec2, physicsComponent.body.worldCenter, wake = true)
                } else {
                    jumping = false
                }
            }
            physicsComponent.body.gravityScale = if (jumping) GRAVITY_IN_JUMP else GRAVITY_IN_FALL

            // TODO nononono, make a better check with a ground sensor
            if (physicsComponent.body.linearVelocityY == 0f) {
                coyoteTimeInTicks = JumpComponent.COYOTE_TICKS
                canJumpFromGround = true
                canJumpInAir = 2
            } else {
                coyoteTimeInTicks--
                canJumpFromGround = coyoteTimeInTicks > 0
            }
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
        val physicsComponent = entity[PhysicsComponent]

        positionComponent.position.set(
            lerp(physicsComponent.previousPosition.x, physicsComponent.body.position.x, alpha),
            lerp(physicsComponent.previousPosition.y, physicsComponent.body.position.y, alpha),
        )
    }

    companion object {
        private val tempVec2 = Vec2()
        private const val JUMP_VELOCITY = -12f
        private const val MAX_FALL_VELOCITY = 30f
        private const val GRAVITY_IN_JUMP = 2f
        private const val GRAVITY_IN_FALL = 10f
    }

}