package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.Key
import com.littlekt.math.MutableVec2f
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.FMOD.STUDIO_EVENT_CALLBACK_SOUND_STOPPED
import net.mattemade.fmod.FmodCallback
import net.mattemade.fmod.FmodCallbackExternal
import net.mattemade.fmod.FmodCallbackType
import net.mattemade.fmod.FmodResult
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.SWIM_ACCELERATION
import net.mattemade.platformer.SWIM_VELOCITY
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.PlayerComponent
import kotlin.math.sign
import kotlin.random.Random

class ControlsSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f),
    ): IteratingSystem(family { all(Box2DPhysicsComponent, MoveComponent, JumpComponent, PlayerComponent)}, interval = interval) {

    private val input = context.input
    private var jumpPressed = false

    override fun onTickEntity(entity: Entity) {
        if (input.isKeyPressed(Key.R)) {
            gameContext.load(reset = true)
            return
        } else if (input.isKeyPressed(Key.L)) {
            gameContext.load(forceRestart = true)
            return
        }

        val context = entity[ContextComponent]

        if (context.swimming) {
            waterBasedControls(context, entity)
        } else {
            landBasedControls(context, entity)
        }
    }

    private fun landBasedControls(
        context: ContextComponent,
        entity: Entity
    ) {
        var horizontalSpeed = 0f
        var verticalSpeed = 0f
        val dash = input.isKeyPressed(Key.SHIFT_LEFT) && !context.touchingWalls

        if (input.isKeyPressed(Key.ARROW_RIGHT) || input.isKeyPressed(Key.D)) {
            horizontalSpeed += WALK_VELOCITY
        }
        if (input.isKeyPressed(Key.ARROW_LEFT) || input.isKeyPressed(Key.A)) {
            horizontalSpeed -= WALK_VELOCITY
        }


        entity[JumpComponent].apply {
            val jumpCurrentlyPressed = input.isKeyPressed(Key.SPACE) || input.isTouching
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

    private fun waterBasedControls(
        context: ContextComponent,
        entity: Entity
    ) {
        var horizontalSpeed = 0f
        var verticalSpeed = 0f
        val dash = input.isKeyPressed(Key.SHIFT_LEFT)

        if (input.isKeyPressed(Key.ARROW_RIGHT) || input.isKeyPressed(Key.D)) {
            horizontalSpeed += SWIM_ACCELERATION
        }
        if (input.isKeyPressed(Key.ARROW_LEFT) || input.isKeyPressed(Key.A)) {
            horizontalSpeed -= SWIM_ACCELERATION
        }
        if (input.isKeyPressed(Key.ARROW_UP) || input.isKeyPressed(Key.W) || input.isKeyPressed(Key.SPACE)) {
            verticalSpeed -= SWIM_ACCELERATION
        }
        if (input.isKeyPressed(Key.ARROW_DOWN) || input.isKeyPressed(Key.S)) {
            verticalSpeed += SWIM_ACCELERATION
        }

        entity[MoveComponent].apply {
            speed = 1f
            moveDirection.set(horizontalSpeed, verticalSpeed)
            if (moveDirection.length() > SWIM_ACCELERATION) {
                moveDirection.setLength(SWIM_ACCELERATION)
            }
            if (dash) {
                if (dashDirection.x != 0f || dashDirection.y != 0f) {
                    dashDirection.set(dashDirection.x, dashDirection.y)
                } else {
                    val body = entity[Box2DPhysicsComponent].body
                    tempVec2f.set(body.linearVelocityX, body.linearVelocityY).norm().setLength(SWIM_VELOCITY * 3f)
                    dashDirection.set(tempVec2f.x, tempVec2f.y)
                }
            } else {
                dashDirection.set(0f, 0f)
            }
        }
    }

    private val testParametedId by lazy { gameContext.fmodAssets.eventDescription.getParameterDescriptionByName("bassy").id }
    private fun JumpComponent.executeJump(wallJump: Boolean = false) {
        val instance = gameContext.fmodAssets.eventDescription.createInstance()
        instance.setCallback(FmodCallback { type, event, parameters ->
            println("jump sound stopped")
            FMOD.OK
        }, callbackMask = STUDIO_EVENT_CALLBACK_SOUND_STOPPED)
        val value = Random.nextInt(3).toFloat()
        println("start jump sound with bassy of $value")
        instance.setParameterByID(testParametedId, value, 1)
        instance.start()

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