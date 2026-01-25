package net.mattemade.utils

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import net.mattemade.gui.api.GuiColor
import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Vec2
import net.mattemade.utils.msdf.MsdfFont
import net.mattemade.utils.msdf.MsdfFontRenderer

class GuiRenderer(
    private val shapeRenderer: ShapeRenderer,
    private val msdfFont: MsdfFont,
) : GuiRenderer {

    private val scale = 3f
    private val msdfFontRenderer = MsdfFontRenderer(msdfFont)
    private val textToDraw = AllocatedMutableBuffer<TextToDraw>(100) { TextToDraw("", 0f, 0f) }

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
        textToDraw.mutate().apply {
            line = text
            this.x = x
            this.y = y
        }
    }

    override fun measureText(text: String, into: Vec2) {
        msdfFont.measure(text, scale, into)
    }

    fun flushText(batch: Batch) {
        msdfFontRenderer.drawAllTextAtOnce(batch) {
            while (textToDraw.hasNext) {
                textToDraw.pop?.let {
                    draw(it.line, it.x, it.y, scale = scale, batch)
                }
            }
        }
    }

    private class TextToDraw(var line: String, var x: Float, var y: Float)

    private class AllocatedMutableBuffer<T>(initialCapacity: Int, private val allocate: () -> T) {
        private val buffer = ArrayList<T>(initialCapacity)
        private var position = -1

        init {
            repeat(initialCapacity) {
                buffer += allocate()
            }
        }

        fun mutate(): T {
            if (++position > buffer.size) {
                buffer += allocate()
            }
            return buffer[++position]
        }

        val hasNext: Boolean get() = position >= 0

        val pop: T?
            get() = buffer.getOrNull(position--)
    }

    companion object {
        private val coloMapping = mapOf(
            GuiColor.CONTAINER_BACKGROUND to Color.LIGHT_GRAY,
            GuiColor.WIDGET_BACKGROUND to Color.DARK_GRAY,
            GuiColor.ON_CONTAINER to Color.BLACK,
        )

        private fun GuiColor.toFloatBits(): Float = coloMapping[this]?.toFloatBits() ?: 0f
        private fun GuiColor.toColor(): Color = coloMapping[this] ?: Color.BLACK
    }
}