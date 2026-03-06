package net.mattemade.platformer.input

import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f

class TouchButton(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Float,
    val activeColor: Float,
    var isDpad: Boolean = false,
    var isActive: Boolean = false,
    val stateChanged: (button: TouchButton) -> Unit
) {

    val center = Vec2f(x, y)
    val touchDirection = MutableVec2f()
    var trackingPointer = -1

    fun intersects(pointer: Vec2f, pointerIndex: Int): Boolean {
        val result = center.distance(pointer) <= radius || trackingPointer == pointerIndex
        if (result && isDpad) {
            trackingPointer = pointerIndex
            touchDirection.set(pointer).subtract(center)
            if (touchDirection.length() > radius) {
                touchDirection.setLength(radius)
            }
            stateChanged(this)
        }

        return result
    }

    fun update(active: Boolean) {
        if (isActive != active) {
            if (!active) {
                trackingPointer = -1
                touchDirection.set(0f, 0f)
            }
            isActive = active
            stateChanged(this)
        }
    }

}