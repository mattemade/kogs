package net.mattemade.platformer.input

import com.littlekt.Context
import com.littlekt.input.GameAxis
import com.littlekt.input.GameButton
import com.littlekt.input.InputMapController
import com.littlekt.input.Key

enum class ControllerInput {
    MOVE_LEFT, MOVE_RIGHT, MOVE_HORIZONTAL,
    MOVE_UP, MOVE_DOWN, MOVE_VERTICAL,

    GAMEPAD_LEFT, GAMEPAD_RIGHT, GAMEPAD_HORIZONTAL,
    GAMEPAD_UP, GAMEPAD_DOWN, GAMEPAD_VERTICAL,

    JUMP, ATTACK, DASH, MAP, PAUSE,
    RESTART, RESPAWN,

    ANY_KEYBOARD,
    ANY_GAMEPAD,
    ANY,
}

fun Context.bindInputs(): InputMapController<ControllerInput> =
    InputMapController<ControllerInput>(input).apply {
        // the 'A' and 'left arrow' keys and the 'x-axis of the left stick' with trigger the 'MOVE_LEFT' input type
        val anyKey = mutableListOf<Key>()
        val anyActionKey = mutableListOf<Key>()
        val anyButton = mutableListOf<GameButton>()
        val anyActionButton = mutableListOf<GameButton>()
        fun List<Key>.any(): List<Key> = this.also { anyKey.addAll(this) }
        fun List<Key>.action(): List<Key> = this.also { anyActionKey.addAll(this) }
        fun List<GameButton>.any(): List<GameButton> = this.also { anyButton.addAll(this) }
        fun List<GameButton>.action(): List<GameButton> = this.also { anyActionButton.addAll(this) }

        addBinding(
            ControllerInput.MOVE_RIGHT,
            listOf(Key.D, Key.ARROW_RIGHT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.RIGHT).any()
        )
        addBinding(
            ControllerInput.MOVE_LEFT,
            listOf(Key.A, Key.ARROW_LEFT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.LEFT).any()
        )
        addAxis(
            ControllerInput.MOVE_HORIZONTAL,
            ControllerInput.MOVE_RIGHT,
            ControllerInput.MOVE_LEFT
        )


        addBinding(
            ControllerInput.MOVE_UP,
            listOf(Key.W, Key.ARROW_UP).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.UP).any()
        )
        addBinding(
            ControllerInput.MOVE_DOWN,
            listOf(Key.S, Key.ARROW_DOWN).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.DOWN).any()
        )
        addAxis(ControllerInput.MOVE_VERTICAL, ControllerInput.MOVE_DOWN, ControllerInput.MOVE_UP)


        addBinding(
            ControllerInput.GAMEPAD_RIGHT,
            axes = listOf(GameAxis.LX, GameAxis.RX),
        )
        addBinding(
            ControllerInput.GAMEPAD_LEFT,
            axes = listOf(GameAxis.LX, GameAxis.RX),
        )
        addAxis(
            ControllerInput.GAMEPAD_HORIZONTAL,
            ControllerInput.GAMEPAD_RIGHT,
            ControllerInput.GAMEPAD_LEFT
        )

        addBinding(
            ControllerInput.GAMEPAD_UP,
            axes = listOf(GameAxis.LY, GameAxis.RY),
        )
        addBinding(
            ControllerInput.GAMEPAD_DOWN,
            axes = listOf(GameAxis.LY, GameAxis.RY),
        )
        addAxis(
            ControllerInput.GAMEPAD_VERTICAL,
            ControllerInput.GAMEPAD_DOWN,
            ControllerInput.GAMEPAD_UP
        )

        addBinding(
            ControllerInput.JUMP,
            listOf(Key.J, Key.Z, Key.SPACE).any().action(),
            buttons = listOf(GameButton.XBOX_A, GameButton.XBOX_Y).any().action(),
        )

        addBinding(
            ControllerInput.ATTACK,
            listOf(Key.K, Key.X).any().action(),
            buttons = listOf(GameButton.XBOX_X, GameButton.XBOX_B).any().action(),
        )

        addBinding(
            ControllerInput.DASH,
            listOf(Key.SHIFT_LEFT, Key.SHIFT_RIGHT).any().action(),
            buttons = listOf(GameButton.L1, GameButton.R1, GameButton.L2, GameButton.R2).any().action(),
        )

        addBinding(
            ControllerInput.MAP,
            listOf(Key.TAB).any(),
            buttons = listOf(GameButton.SELECT).any(),
        )

        addBinding(
            ControllerInput.PAUSE,
            listOf(Key.ENTER, Key.ESCAPE, Key.P).any(),
            buttons = listOf(GameButton.START).any(),
        )

        addBinding(
            ControllerInput.RESTART,
            listOf(Key.NUM0).any(),
        )

        addBinding(
            ControllerInput.RESPAWN,
            listOf(Key.NUM5).any(),
        )

        addBinding(ControllerInput.ANY_KEYBOARD, anyKey)
        addBinding(ControllerInput.ANY_GAMEPAD, buttons = anyButton)
        addBinding(ControllerInput.ANY, anyKey, buttons = anyButton)

        mode = InputMapController.InputMode.GAMEPAD

        input.addInputProcessor(this)
    }