package net.mattemade.platformer.input

import com.littlekt.Context
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import kotlin.time.Duration

class GameInput(
    private val context: Context,
    private val input: InputMapController<ControllerInput>
) {

    val previousMovement = MutableVec2f()
    val movement = MutableVec2f()

    private var states = mutableListOf<State>()

    val touchButtonStates = Array(ControllerInput.entries.size) { false }

    val jump = stateOf(ControllerInput.JUMP)
    val attack = stateOf(ControllerInput.ATTACK)
    val dash = stateOf(ControllerInput.DASH)
    val map = stateOf(ControllerInput.MAP)
    val pause = stateOf(ControllerInput.PAUSE)
    val restart = stateOf(ControllerInput.RESTART)
    val respawn = stateOf(ControllerInput.RESPAWN)


    private fun stateOf(type: ControllerInput): State = State(input, touchButtonStates, type).also { states += it }

    var mouseDetected: Boolean = false
    var keyboardInput: Boolean = false
    var touchInput: Boolean = false
    var deadzone = 0.25f
    var gamepadInput: Boolean = false
        set(value) {
            field = value
            deadzone = if (gamepadInput) 0.02f else 0.25f // gamepad stick vs touch controls
        }

    fun update(
        controlsActive: Boolean,
    ) {
        val gamepadMoveHorizontal = input.axis(ControllerInput.GAMEPAD_HORIZONTAL)
        val gamepadMoveVertical = input.axis(ControllerInput.GAMEPAD_HORIZONTAL)

        if (!keyboardInput && input.down(ControllerInput.ANY_KEYBOARD)) {
            keyboardInput = true
            gamepadInput = false
            touchInput = false
        } else if (!gamepadInput && (input.down(ControllerInput.ANY_GAMEPAD) || gamepadMoveHorizontal != 0f || gamepadMoveVertical != 0f)) {
            gamepadInput = true
            keyboardInput = false
            touchInput = false
        }

        if (!touchInput) {
            previousMovement.set(movement)
            movement
                .set(
                    input.axis(ControllerInput.MOVE_HORIZONTAL),
                    input.axis(ControllerInput.MOVE_VERTICAL)
                )
                .limit(1f)

            if (!controlsActive || movement.length() < deadzone) {
                movement.set(0f, 0f)
            }
        }

        states.forEach { it.update(controlsActive) }
    }

    class State(private val input: InputMapController<ControllerInput>, private val touchStates: Array<Boolean>, val type: ControllerInput, private val tag: String? = null) {
        var pressed = false
        var justPressed = false
        var justReleased = false

        fun update(isActive: Boolean) {
            if (isActive && (input.down(type) || touchStates[type.ordinal])) {
                if (justPressed) {
                    justPressed = false
                } else if (!pressed) {
                    justPressed = true
                    pressed = true
                }
            } else if (pressed) {
                justReleased = true
                justPressed = false
                pressed = false
            } else if (justReleased) {
                justReleased = false
            }
        }
    }

    private fun MutableVec2f.limit(maxLength: Float) {
        if (length() > maxLength) {
            setLength(maxLength)
        }
    }
}