package com.littlekt.input

import com.littlekt.input.internal.InternalInputEvent
import com.littlekt.input.internal.InternalInputEventType
import com.littlekt.util.datastructure.Pool
import com.littlekt.util.fastForEach

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputQueueProcessor(private val activeInputProcessors: List<InputProcessor>) {

    val currentEventTime get() = _currentEventTime

    private val eventsPool = Pool(reset = { it.reset() }, preallocate = 25, gen = { InternalInputEvent() })

    private val queue = mutableListOf<InternalInputEvent>()
    private val processingQueue = mutableListOf<InternalInputEvent>()
    private var _currentEventTime = 0L

    fun drain(processors: List<InputProcessor>) {
        if (processors.isEmpty()) {
            eventsPool.free(queue)
            queue.clear()
            return
        }
        processingQueue.addAll(queue)
        queue.clear()

        processingQueue.forEach {
            notifyProcessors(it, processors)
        }
        eventsPool.free(processingQueue)
        processingQueue.clear()
    }

    private fun notifyProcessors(
        event: InternalInputEvent,
        processors: List<InputProcessor>
    ) {
        _currentEventTime = event.queueTime
        when (event.type) {
            InternalInputEventType.KEY_DOWN -> processors.keyDown(key = event.key)
            InternalInputEventType.KEY_UP -> processors.keyUp(key = event.key)
            InternalInputEventType.KEY_REPEAT -> processors.keyRepeat(key = event.key)
            InternalInputEventType.CHAR_TYPED -> processors.charTyped(character = event.typedChar)
            InternalInputEventType.TOUCH_DOWN -> processors.touchDown(
                event.x,
                event.y,
                event.pointer
            )

            InternalInputEventType.TOUCH_UP -> processors.touchUp(event.x, event.y, event.pointer)
            InternalInputEventType.TOUCH_DRAGGED -> processors.touchDragged(
                event.x,
                event.y,
                event.moveX,
                event.moveY,
                event.pointer
            )

            InternalInputEventType.MOUSE_MOVED -> processors.mouseMoved(
                event.x,
                event.y,
                event.moveX,
                event.moveY
            )

            InternalInputEventType.SCROLLED -> processors.scrolled(event.x, event.y)
            InternalInputEventType.GAMEPAD_BUTTON_DOWN -> processors.gamepadButtonPressed(
                event.gamepadButton,
                event.gamepadButtonPressure,
                event.gamepad.index
            )

            InternalInputEventType.GAMEPAD_BUTTON_UP -> processors.gamepadButtonReleased(
                event.gamepadButton,
                event.gamepad.index
            )

            InternalInputEventType.GAMEPAD_JOYSTICK_MOVED -> processors.gamepadJoystickMoved(
                event.gamepadStick,
                event.x,
                event.y,
                event.gamepad.index
            )

            InternalInputEventType.GAMEPAD_TRIGGER_CHANGED -> processors.gamepadTriggerChanged(
                event.gamepadButton,
                event.gamepadButtonPressure,
                event.gamepad.index
            )
        }
    }

    fun keyDown(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_DOWN
                this.key = key
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun keyUp(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_UP
                this.key = key
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun keyRepeat(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_REPEAT
                this.key = key
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun charTyped(character: Char, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.CHAR_TYPED
                typedChar = character
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun touchDown(screenX: Float, screenY: Float, movementX: Float, movementY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DOWN
                x = screenX
                y = screenY
                moveX = movementX
                moveY = movementY
                queueTime = time
                this.pointer = pointer
            }
        }.also { saveEvent(it) }
    }

    fun touchUp(screenX: Float, screenY: Float, movementX: Float, movementY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_UP
                x = screenX
                y = screenY
                moveX = movementX
                moveY = movementY
                queueTime = time
                this.pointer = pointer
            }
        }.also { saveEvent(it) }
    }

    fun touchDragged(screenX: Float, screenY: Float, movementX: Float, movementY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DRAGGED
                x = screenX
                y = screenY
                moveX = movementX
                moveY = movementY
                queueTime = time
                this.pointer = pointer
            }
        }.also { saveEvent(it) }
    }

    fun mouseMoved(screenX: Float, screenY: Float, movementX: Float, movementY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.MOUSE_MOVED
                x = screenX
                y = screenY
                moveX = movementX
                moveY = movementY
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun scrolled(amountX: Float, amountY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.SCROLLED
                x = amountX
                y = amountY
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun gamepadButtonDown(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_BUTTON_DOWN
                this.gamepadButton = button
                this.gamepadButtonPressure = pressure
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun gamepadButtonUp(button: GameButton, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_BUTTON_UP
                this.gamepadButton = button
                this.gamepadButtonPressure = 0f
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun gamepadJoystickMoved(stick: GameStick, x: Float, y: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_JOYSTICK_MOVED
                this.gamepadStick = stick
                this.x = x
                this.y = y
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { saveEvent(it) }
    }

    fun gamepadTriggerMoved(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_TRIGGER_CHANGED
                this.gamepadButton = button
                this.gamepadButtonPressure = pressure
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { saveEvent(it) }
    }
    
    private fun saveEvent(event: InternalInputEvent) {
        queue.add(event)
        notifyProcessors(event, activeInputProcessors)
    }

    private fun List<InputProcessor>.keyDown(key: Key) {
        fastForEach { if (it.keyDown(key)) return }
    }

    private fun List<InputProcessor>.keyUp(key: Key) {
        fastForEach { if (it.keyUp(key)) return }
    }

    private fun List<InputProcessor>.keyRepeat(key: Key) {
        fastForEach { if (it.keyRepeat(key)) return }
    }

    private fun List<InputProcessor>.charTyped(character: Char) {
        fastForEach { if (it.charTyped(character)) return }
    }

    private fun List<InputProcessor>.touchDown(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { if (it.touchDown(screenX, screenY, pointer)) return }
    }

    private fun List<InputProcessor>.touchUp(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { if (it.touchUp(screenX, screenY, pointer)) return }
    }

    private fun List<InputProcessor>.touchDragged(screenX: Float, screenY: Float, movementX: Float, movementY: Float, pointer: Pointer) {
        fastForEach { if (it.touchDragged(screenX, screenY, movementX, movementY, pointer)) return }
    }

    private fun List<InputProcessor>.mouseMoved(screenX: Float, screenY: Float, movementX: Float, movementY: Float) {
        fastForEach { if (it.mouseMoved(screenX, screenY, movementX, movementY)) return }
    }

    private fun List<InputProcessor>.scrolled(amountX: Float, amountY: Float) {
        fastForEach { if (it.scrolled(amountX, amountY)) return }
    }

    private fun List<InputProcessor>.gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int) {
        fastForEach { if (it.gamepadButtonPressed(button, pressure, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadButtonReleased(button: GameButton, gamepad: Int) {
        fastForEach { if (it.gamepadButtonReleased(button, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int) {
        fastForEach { if (it.gamepadJoystickMoved(stick, xAxis, yAxis, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int) {
        fastForEach { if (it.gamepadTriggerChanged(button, pressure, gamepad)) return }
    }
}