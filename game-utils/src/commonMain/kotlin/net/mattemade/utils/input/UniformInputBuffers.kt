package net.mattemade.utils.input

import com.littlekt.Context
import com.littlekt.input.GameButton
import com.littlekt.input.GameStick
import com.littlekt.input.Input
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer

class UniformInputBuffers(private val delegate: InputProcessor, private val input: Input) {



    private val size = 100
    private val eventBuffer = ArrayQueue(-1) { Array(size) { 0 } }
    private val floatBuffer = ArrayQueue(-1f) { Array(size * 2) { 0f } }
    private val intArgBuffer = ArrayQueue(-1) { Array(size) { 0 } }
    private val timestampBuffer = ArrayQueue(-1L) { Array(size) { 0L } }

    init {
        input.addActiveInputProcessor(object: InputProcessor {
            override fun keyDown(key: Key): Boolean {
                intArgBuffer.put(key.ordinal)
                timestampBuffer.put(input.currentEventTime)
                eventBuffer.put(UniformInputEventCode.KEY_DOWN)
                return true
            }

            override fun keyUp(key: Key): Boolean {
                intArgBuffer.put(key.ordinal)
                eventBuffer.put(UniformInputEventCode.KEY_UP)
                return true
            }

            override fun keyRepeat(key: Key): Boolean {
                return true
            }

            override fun charTyped(character: Char): Boolean {
                return true
            }

            override fun touchDown(
                screenX: Float,
                screenY: Float,
                pointer: Pointer
            ): Boolean {
                timestampBuffer.put(input.currentEventTime)
                eventBuffer.put(UniformInputEventCode.KEY_DOWN)
                return true
            }

            override fun touchUp(
                screenX: Float,
                screenY: Float,
                pointer: Pointer
            ): Boolean {
                return true
            }

            override fun touchDragged(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float,
                pointer: Pointer
            ): Boolean {
                return true
            }

            override fun mouseMoved(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float
            ): Boolean {
                return true
            }

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                return true
            }

            override fun gamepadButtonPressed(
                button: GameButton,
                pressure: Float,
                gamepad: Int
            ): Boolean {
                return true
            }

            override fun gamepadButtonReleased(
                button: GameButton,
                gamepad: Int
            ): Boolean {
                return true
            }

            override fun gamepadJoystickMoved(
                stick: GameStick,
                xAxis: Float,
                yAxis: Float,
                gamepad: Int
            ): Boolean {
                return true
            }

            override fun gamepadTriggerChanged(
                button: GameButton,
                pressure: Float,
                gamepad: Int
            ): Boolean {
                return true
            }
        })
    }

    fun drain() {
        while (eventBuffer.hasNext) {
            when (eventBuffer.next) {
                UniformInputEventCode.KEY_DOWN -> delegate.keyDown(Key.entries[intArgBuffer.next])
                UniformInputEventCode.KEY_UP -> delegate.keyDown(Key.entries[intArgBuffer.next])
            }
        }
    }


    companion object {

    }
}

private class ArrayQueue<T>(
    private val buffer: Array<T>, // can be used concurrently
    val noValue: T,
) {

    constructor(noValue: T, constructor: () -> Array<T>) : this(constructor(), noValue)

    private val size = buffer.size
    private val positions = IntArray(2) // [ writePosition, readPosition ], can be used concurrently

    private var writePosition: Int
        get() = positions[0]
        set(value) { positions[0] = value }

    private var readPosition: Int
        get() = positions[1]
        set(value) { positions[1] = value }

    fun put(value: T) {
        buffer[writePosition] = value
        writePosition = (writePosition + 1) % size
    }

    val hasNext: Boolean get() = readPosition != writePosition

    val next: T
        get() =
            if (readPosition == writePosition) {
                noValue
            } else {
                val result = buffer[readPosition]
                readPosition = (readPosition + 1) % size
                result
            }
}

object UniformInputEventCode {
    const val MOUSE_MOVE = 1 // move coordinates and pointer ID
    const val POINTER_DOWN = 2 // pointer ID
    const val POINTER_UP = 3 // pointer ID
    const val SCROLL = 4 //
    const val KEY_DOWN = 5 // key code
    const val KEY_UP = 6 // key code
}