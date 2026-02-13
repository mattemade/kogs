package net.mattemade.bigmode.stuff

import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.Vec2f
import net.mattemade.bigmode.RESTAURANT_HEIGHT_REVERSE_RATIO

class TakeawayArea(val x: Float, val y: Float, val width: Float, val height: Float, val station: Int, var item: Pair<String, Sprite>? = null) {

    val x2 = x + width
    val y2 = y + height

    val visiblePosition = Vec2f(x, y * RESTAURANT_HEIGHT_REVERSE_RATIO)
    val visibleSize = Vec2f(width, height * RESTAURANT_HEIGHT_REVERSE_RATIO)
    val cx: Float = x + width * 0.5f
    val cy: Float = (y + width * 0.5f) * RESTAURANT_HEIGHT_REVERSE_RATIO - STATION_HEIGHT

    fun render(batch: Batch) {
        item?.second?.let { sprite ->
            sprite.visiblePosition.set(cx + if (station == 0) -ITEM_OFFSET else ITEM_OFFSET, cy)
            sprite.render(batch)
        }
    }

    companion object {
        private const val STATION_HEIGHT = 20f
        private const val ITEM_OFFSET = 15f
    }

}