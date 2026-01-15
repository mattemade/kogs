package net.mattemade.gui.api

sealed interface GuiInputEvent {

    data class TouchDown(val x: Float, val y: Float, val pointer: Int) : GuiInputEvent
    data class TouchUp(val x: Float, val y: Float, val pointer: Int) : GuiInputEvent
    data class TouchMove(val x: Float, val y: Float, val pointer: Int) : GuiInputEvent
    data class CursorMove(val x: Float, val y: Float, val pointer: Int) : GuiInputEvent
    data class Scroll(val dx: Float, val dy: Float) : GuiInputEvent
    data class KeyPressed(val key: Int) : GuiInputEvent
    data class KeyReleased(val key: Int) : GuiInputEvent
    data class WindowMoved(val x: Int, val y: Int) : GuiInputEvent
    data class WindowResized(val width: Int, val height: Int) : GuiInputEvent

    companion object {

    }
}