package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.math.MutableVec2f
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.SWIM_ACCELERATION
import net.mattemade.platformer.SWIM_VELOCITY
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PlayerComponent
import kotlin.math.absoluteValue
import kotlin.math.sign

class ControlsSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family {
    all(
        Box2DPhysicsComponent,
        MoveComponent,
        JumpComponent,
        AttackComponent,
        PlayerComponent
    )
}, interval = interval) {

    private val input = gameContext.gameInput

    override fun onTick() {
        gameContext.updateInputs() // TODO: how to ensure it's never called more than once?
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        if (input.restart.justPressed) {
            gameContext.load(reset = true)
            return
        } else if (input.respawn.justPressed) {
            gameContext.load(forceRestart = true)
            return
        }

        val context = entity[ContextComponent]

        if (context.swimming) {
            waterBasedControls(context, entity)
        } else {
            landBasedControls(context, entity)
        }
        if (gameContext.gameState.sword && input.attack.justPressed) {
            entity[AttackComponent].activated = true
        }
    }

    private fun landBasedControls(
        context: ContextComponent,
        entity: Entity
    ) {
        val horizontalSpeed = input.movement.x * WALK_VELOCITY
        val moveComponent = entity[MoveComponent]
        val dashIntent = gameContext.gameState.airPearl && input.dash.pressed
        moveComponent.forceStopAirDash = moveComponent.forceStopAirDash && dashIntent
        val dash = dashIntent && !moveComponent.forceStopAirDash

        entity[JumpComponent].apply {
            if (input.jump.justPressed && (canJumpFromGround || canJumpInAir > 0 || (gameContext.gameState.airPearl && context.wallSlide)) && !jumping) {
                if (input.movement.y > 0f) {
                    moveComponent.fallThrough = true
                } else {
                    executeJump(entity, wallJump = context.wallSlide)
                }
            } else if (!input.jump.pressed) {
                jumping = false
                jumpBuffer = 0
            } else { // jump is still pressed, do not double-jump automatically in this case, but jump when landed within buffered time
                if (input.movement.y > 0f) {
                    // no-op
                } else if (canJumpFromGround) {
                    if (!jumping && jumpBuffer < JumpComponent.BUFFER_TICKS) {
                        executeJump(entity)
                    }
                } else {
                    jumpBuffer++
                }
            }
        }


        moveComponent.apply {
            speed = 1f
            moveDirection.set(horizontalSpeed, 0f)

            if (dash) {
                dashDirection.set(
                if (context.dashing) {
                    if (moveComponent.ignoreNextDashDirection || horizontalSpeed == 0f) {
                        dashDirection.x
                    } else{
                        horizontalSpeed.sign * dashDirection.x.absoluteValue
                    }
                } else if (context.wallSlide) {
                    if (dashDirection.x != 0f) { // dashing into the wall
                        moveComponent.forceStopAirDash = true
                        context.dashing = false
                        0f
                    } else { // dashing from the wall
                        context.wallSlide = false
                        moveComponent.ignoreNextDashDirection = true
                        if (context.touchingLeftWall) {
                            WALK_VELOCITY * 3f
                        } else /*if (context.touchingRightWall)*/ {
                            -WALK_VELOCITY * 3f
                        }
                    }
                } else if (context.facingRight) {
                    WALK_VELOCITY * 3f
                } else {
                    -WALK_VELOCITY * 3f
                }, 0f)

                context.dashing = dashDirection.x != 0f
            } else {
                moveComponent.ignoreNextDashDirection = false
                context.dashing = false
                dashDirection.set(0f, 0f)
            }
        }
    }

    private fun waterBasedControls(
        context: ContextComponent,
        entity: Entity
    ) {
        val swimSpeedMultiplier = if (gameContext.gameState.waterPearl) 1.5f else 1f
        val horizontalSpeed = input.movement.x * SWIM_ACCELERATION * swimSpeedMultiplier
        val verticalSpeed =
            (input.movement.y - if (input.jump.pressed) 1f else 0f) * SWIM_ACCELERATION * swimSpeedMultiplier
        val moveComponent = entity[MoveComponent]
        val dashIntent = gameContext.gameState.waterPearl && input.dash.pressed
        moveComponent.forceStopWaterDash = moveComponent.forceStopWaterDash && dashIntent
        val dash = dashIntent && !moveComponent.forceStopWaterDash

        moveComponent.apply {
            speed = 1f
            moveDirection.set(horizontalSpeed, verticalSpeed)
            if (moveDirection.length() > SWIM_ACCELERATION) {
                moveDirection.setLength(SWIM_ACCELERATION)
            }
            if (dash) {
                dashDirection.set(moveDirection).norm().setLength(SWIM_VELOCITY * 3f)
            } else {
                dashDirection.set(0f, 0f)
            }
        }
    }

    private fun JumpComponent.executeJump(entity: Entity, wallJump: Boolean = false) {
        entity[Box2DPhysicsComponent].playSound(gameContext.fmodAssets.jump)

        jumping = true
        if (!canJumpFromGround && !wallJump) {
            canJumpInAir--
        }
        canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS
        jumpBuffer = 0
    }

    private companion object {
        val tempVec2f = MutableVec2f()
    }
}