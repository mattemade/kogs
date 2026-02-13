package net.mattemade.bigmode.stuff

import com.littlekt.math.Vec2f
import net.mattemade.bigmode.RESTAURANT_HEIGHT_REVERSE_RATIO

class Obstacle(val position: Vec2f, val radius: Float, var type: Type) {

    val visiblePosition = Vec2f(position.x * 1f, position.y * RESTAURANT_HEIGHT_REVERSE_RATIO)
    val ry = radius * RESTAURANT_HEIGHT_REVERSE_RATIO


    enum class Type {
        CHAIR,
        TABLE,
        SNAKE_TABLE,
    }
}