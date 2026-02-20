package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.Key
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PhysicsComponent

class ControlsSystem(
    private val context: Context = inject(),
    interval: Interval = Fixed(1 / 200f),
    ): IteratingSystem(family { all(PhysicsComponent, MoveComponent, JumpComponent)}, interval = interval) {

    private val input = context.input

    override fun onTickEntity(entity: Entity) {
        var horizontalSpeed = 0f
        var verticalSpeed = 0f
        if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            horizontalSpeed += 8f
        }
        if (input.isKeyPressed(Key.ARROW_LEFT)) {
            horizontalSpeed -= 8f
        }

        entity[JumpComponent].apply {
            if (input.isKeyJustPressed(Key.SPACE) && (canJumpFromGround || canJumpInAir > 0) && !jumping) {
                executeJump()
            } else if (!input.isKeyPressed(Key.SPACE)) {
                jumping = false
                jumpBuffer = 0
            } else { // jump is still pressed, do not double-jump automatically in this case, but jump when landed within buffered time
                if (canJumpFromGround) {
                    if (!jumping && jumpBuffer < JumpComponent.BUFFER_TICKS) {
                        executeJump()
                    }
                } else {
                    jumpBuffer++
                }
            }
        }

        entity[MoveComponent].apply {
            speed = 1f
            direction.set(horizontalSpeed, verticalSpeed)
        }
    }

    private fun JumpComponent.executeJump() {
        jumping = true
        if (!canJumpFromGround) {
            canJumpInAir--
        }
        canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS
        jumpBuffer = 0
    }
}