package net.mattemade.gui.api.math

import net.mattemade.gui.api.memory.Pool

class Vec2 private constructor(var x: Float = 0f, var y: Float = 0f) {
    fun set(x: Float, y: Float): Vec2 {
        this.x = x
        this.y = y
        return this
    }

    companion object: Pool<Vec2>(::Vec2)
}