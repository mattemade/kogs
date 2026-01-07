package net.mattemade.utils.drawing

import com.littlekt.graph.node.resource.HAlign
import com.littlekt.graph.node.resource.VAlign
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.toFloatBits

class MonoSpaceTextDrawer(
    private val typefaces: List<Typeface>,
    private val px: (Float) -> Float = { it },
) {

    private val letters = mutableMapOf<Char, Lettering>().apply {
        typefaces.forEach { typeface ->
            typeface.apply {
                var yPosition = pixelPadding
                alphabet.forEachIndexed { index, key ->
                    val xPosition = pixelPadding + index * (pixelCharWidth + pixelMargin)

                    put(
                        key, Lettering(
                            TextureSlice(
                                font,
                                xPosition + pixelDrawingOffsetX,
                                yPosition + pixelDrawingOffsetY,
                                pixelDrawingCharWidth,
                                pixelDrawingCharHeight,
                            ),
                            typeface,
                        )
                    )
                }
            }
        }
    }

    fun drawingWidth(line: String): Float =
        line.sumOf { letters[it]?.typeface?.charWidth ?: 0f }


    fun drawingHeight(line: String): Float = line.maxOfOrNull { letters[it]?.typeface?.charHeight ?: 0f } ?: 0f

    /** @return number of drawn characters: caller could use it to check if it is changed  */
    fun drawText(
        batch: Batch,
        lines: SmartLines,
        x: Float,
        y: Float,
        hAlign: HAlign = HAlign.LEFT,
        vAlign: VAlign = VAlign.TOP,
        characterLimit: Int = Int.MAX_VALUE,
        color: (Int) -> Color = { Color.WHITE },
    ): Int {
        if (lines.size == 0) {
            return 0
        }
        val textBoxWidth = lines.textBoxWidth
        val textBoxHeight = lines.textBoxHeight
        val startPositionX: Float = when (hAlign) {
            HAlign.RIGHT -> x - textBoxWidth
            HAlign.LEFT -> x// - textBoxWidth
            HAlign.CENTER -> x - textBoxWidth / 2f
            else -> throw IllegalArgumentException("Provide a correct hAlign instead of $hAlign")
        }
        var linePositionY: Float = when (vAlign) {
            VAlign.TOP -> y
            VAlign.BOTTOM -> y - textBoxHeight
            VAlign.CENTER -> y - textBoxHeight / 2f
            else -> throw IllegalArgumentException("Provide a correct vAlign instead of $vAlign")
        }
        var drawnCharacters = 0
        val rows = lines.size
        var maxOffset = 0f
        lines.forEachIndexed { index, line ->
            var linePositionX = startPositionX// + (textBoxWidth - line.drawingWidth) / 2f
            line.forEachIndexed { column, char ->
                if (/*char != ' ' && */drawnCharacters < characterLimit) {
                    letters[char]?.let { letter ->
                        val typeface = letter.typeface
                        batch.draw(
                            slice = letter.slice,
                            x = px(linePositionX + typeface.pixelDrawingOffsetX),
                            y = px(linePositionY + typeface.pixelDrawingOffsetY/*textBoxHeight - (rows - row) * (drawingLetterHeight + drawingVerticalSpacing)*/),
                            width = typeface.drawingCharWidth,
                            height = typeface.drawingCharHeight,
                            colorBits = color(drawnCharacters).toFloatBits(),
                        )
                        linePositionX += typeface.charWidth
                        maxOffset = maxOf(maxOffset, typeface.letterSpaceY)
                    }
                    drawnCharacters++
                }
            }
            linePositionY += lines.drawingHeight[index]
            maxOffset = 0f
        }
        return drawnCharacters
    }

    private inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
        var sum: Float = 0f
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    private inline fun CharSequence.sumOf(selector: (Char) -> Float): Float {
        var sum: Float = 0f
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    class Typeface(
        val font: TextureSlice,
        val alphabet: String,
        val pixelPadding: Int, // empty pixels from the slice edge
        val pixelMargin: Int, // empty pixels between letters
        val pixelCharWidth: Int = (font.width - pixelPadding * 2 - pixelMargin * (alphabet.length - 1)) / alphabet.length,
        val pixelCharHeight: Int = (font.height - pixelPadding * 2),
        val pixelDrawingOffsetX: Int = -1,
        val pixelDrawingOffsetY: Int = -1,
        val pixelDrawingCharWidth: Int = pixelCharWidth - pixelDrawingOffsetX * 2,
        val pixelDrawingCharHeight: Int = pixelCharHeight - pixelDrawingOffsetY * 2,
        val pixelLetterSpaceX: Int = 1,
        val pixelLetterSpaceY: Int = 1,
        val charWidth: Float = pixelCharWidth.toFloat() + pixelLetterSpaceX.toFloat(),
        val charHeight: Float = pixelCharHeight.toFloat() + pixelLetterSpaceY.toFloat(),
        val drawingCharWidth: Float = pixelDrawingCharWidth.toFloat(),
        val drawingCharHeight: Float = pixelDrawingCharHeight.toFloat(),
        val letterSpaceX: Float = pixelLetterSpaceX.toFloat(),
        val letterSpaceY: Float = pixelLetterSpaceY.toFloat(),

        )

    private class Lettering(val slice: TextureSlice, val typeface: Typeface)
}
