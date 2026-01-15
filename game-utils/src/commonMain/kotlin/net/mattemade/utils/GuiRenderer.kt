package net.mattemade.utils

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.BitmapFontCache
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import net.mattemade.gui.api.GuiColor
import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Vec2

class GuiRenderer(
    private val batch: Batch,
    private val shapeRenderer: ShapeRenderer,
    private val bitmapFont: BitmapFont
) : GuiRenderer {



    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: GuiColor
    ) {
        shapeRenderer.filledRectangle(x = x, y = x, width = width, height = height, color = color.toFloatBits())
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: GuiColor
    ) {
        bitmapCaches[text]?.let {
            it.setPosition(x, y)
            it.tint(color.toColor())
            it.draw(batch)
        }
    }

    override fun measureText(text: String, into: Vec2) {
        val scale = 0.1f
        val cache = bitmapCaches.getOrPut(
            text,
            { BitmapFontCache(bitmapFont).apply { setText(text, x = 0f, y = 0f, scaleX = scale, scaleY = scale) } })
        into.x = cache.width
        into.y = bitmapFont.lineHeight * scale
    }

    companion object {
        private val bitmapCaches = mutableMapOf<String, BitmapFontCache>()
        private val coloMapping = mapOf(
            GuiColor.CONTAINER_BACKGROUND to Color.LIGHT_GRAY,
            GuiColor.WIDGET_BACKGROUND to Color.DARK_GRAY,
            GuiColor.ON_CONTAINER to Color.BLACK,
        )

        private fun GuiColor.toFloatBits(): Float = coloMapping[this]?.toFloatBits() ?: 0f
        private fun GuiColor.toColor(): Color = coloMapping[this] ?: Color.BLACK
    }
}