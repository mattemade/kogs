package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.Key
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import kotlin.math.sign

class ControlsSystem(
    private val context: Context = inject(),
    interval: Interval = Fixed(1 / 200f),
    ): IteratingSystem(family { all(Box2DPhysicsComponent, MoveComponent, JumpComponent)}, interval = interval) {

    private val input = context.input
    private var jumpPressed = false

    override fun onTickEntity(entity: Entity) {
        var horizontalSpeed = 0f
        var verticalSpeed = 0f
        val context = entity[ContextComponent]
        val dash = input.isKeyPressed(Key.SHIFT_LEFT) && !context.touchingWalls

        if (input.isKeyPressed(Key.ARROW_RIGHT) || input.isKeyPressed(Key.D)) {
            horizontalSpeed += WALK_VELOCITY
        }
        if (input.isKeyPressed(Key.ARROW_LEFT) || input.isKeyPressed(Key.A)) {
            horizontalSpeed -= WALK_VELOCITY
        }


        entity[JumpComponent].apply {
            val jumpCurrentlyPressed = input.isKeyPressed(Key.SPACE)
            val jumpJustPressed = jumpCurrentlyPressed && !jumpPressed
            jumpPressed = jumpCurrentlyPressed
            if (jumpJustPressed && (canJumpFromGround || canJumpInAir > 0 || context.touchingWalls) && !jumping) {
                if (input.isKeyPressed(Key.ARROW_DOWN) || input.isKeyPressed(Key.S)) {
                    entity[MoveComponent].fallThrough = true
                } else {
                    executeJump(wallJump = context.touchingWalls)
                }
            } else if (!jumpCurrentlyPressed) {
                jumping = false
                jumpBuffer = 0
            } else { // jump is still pressed, do not double-jump automatically in this case, but jump when landed within buffered time
                if (input.isKeyPressed(Key.ARROW_DOWN) || input.isKeyPressed(Key.S)) {
                    // no-op
                } else if (canJumpFromGround) {
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
            moveDirection.set(horizontalSpeed, verticalSpeed)
            if (dash) {
                if (dashDirection.x != 0f) {
                    dashDirection.set(dashDirection.x, 0f)
                } else {
                    dashDirection.set(horizontalSpeed.sign * WALK_VELOCITY * 3f, 0f)
                }
            } else {
                dashDirection.set(0f, 0f)
            }
        }
    }

    private fun JumpComponent.executeJump(wallJump: Boolean = false) {
        jumping = true
        if (!canJumpFromGround && !wallJump) {
            canJumpInAir--
        }
        canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS
        jumpBuffer = 0
    }
}