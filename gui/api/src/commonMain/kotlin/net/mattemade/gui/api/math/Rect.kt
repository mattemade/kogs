package net.mattemade.gui.api.math

import net.mattemade.gui.api.memory.Pool

class Rect private constructor(var x: Float = 0f, var y: Float = 0f, var width: Float = 0f, var height: Float = 0f) {
    val x2: Float get() = x + width
    val y2: Float get() = y + height

    fun set(x: Float, y: Float, width: Float, height: Float): Rect {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }

    fun set(rect: Rect): Rect {
        this.x = rect.x
        this.y = rect.y
        this.width = rect.width
        this.height = rect.height
        return this
    }

    override fun equals(other: Any?): Boolean =
        other != null && other is Rect && x == other.x && y == other.y && width == other.width && height == other.height

    override fun hashCode(): Int =
        (x + y * 32f + width * 1024f + height * 32768f).toInt()


    companion object : Pool<Rect>(::Rect)
}