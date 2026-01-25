package net.mattemade.utils.msdf

import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.slice
import com.littlekt.graphics.sliceByBounds
import com.littlekt.math.ceilToInt
import com.littlekt.math.floorToInt
import net.mattemade.gui.api.math.Vec2

class MsdfFont(
    val lineHeight: Float,
    val descender: Float,
    val specs: Map<Char, CharacterSpec>
) {

    constructor(atlas: Texture, lineHeight: Float, descender: Float, csvSpecs: List<String>) : this(
        lineHeight = lineHeight,
        descender = descender,
        specs = csvSpecs
            .asSequence()
            .map { it.split(CSV_SEPARATOR) }
            .filter { it.size == 10 }
            .map { CharacterSpec(atlas, it) }
            .associateBy { it.character })


    fun draw(text: String, x: Float, y: Float, scale: Float, batch: Batch) {
        var cursorX = x
        var cursorY = y + (lineHeight - descender) * scale
        text.forEach {
            if (it == '\n') {
                cursorX = x
                cursorY += lineHeight * scale
            } else {
                specs[it]?.let { spec ->
                    batch.draw(
                        slice = spec.slice,
                        x = cursorX + spec.quadLeftBound * scale,
                        y = cursorY + spec.quadBottomBound * scale,
                        width = spec.quadWidth * scale,
                        height = spec.quadHeight * scale,
                    )
                    cursorX += spec.horizontalAdvance * scale
                }
            }
        }
    }

    fun measure(text: String, scale: Float, to: Vec2) {
        var cursorX = 0f
        var lines = 1
        to.x = 0f
        text.forEach {
            if (it == '\n') {
                to.x = maxOf( to.x, cursorX)
                cursorX = 0f
                lines++
            } else {
                specs[it]?.let { spec ->
                    cursorX += spec.horizontalAdvance * scale
                }
            }
        }
        to.x = maxOf( to.x, cursorX)
        to.y = lines * lineHeight * scale
    }

    /**
     * CSV columns
     * * If there are multiple input fonts (-and parameter), the first column is the font index, otherwise it is skipped.
     * * Character Unicode value or glyph index, depending on whether character set or glyph set mode is used.
     * * Horizontal advance in em's.
     * * The next 4 columns are the glyph quad's bounds in em's relative to the baseline and cursor. Depending on the -yorigin setting, this is either left, bottom, right, top (bottom-up Y) or left, top, right, bottom (top-down Y).
     * * The last 4 columns the the glyph's bounds in the atlas in pixels. Depending on the -yorigin setting, this is either left, bottom, right, top (bottom-up Y) or left, top, right, bottom (top-down Y).
     */
    class CharacterSpec(
        val character: Char,
        val horizontalAdvance: Float,
        val quadLeftBound: Float,
        val quadBottomBound: Float,
        val quadRightBound: Float,
        val quadTopBound: Float,
        val slice: TextureSlice,
    ) {

        val quadWidth = quadRightBound - quadLeftBound
        val quadHeight = quadTopBound - quadBottomBound

        constructor(atlas: Texture, csvLineSplit: List<String>) : this(
            character = csvLineSplit[0].toInt().toChar(),
            horizontalAdvance = csvLineSplit[1].toFloat(),
            quadLeftBound = csvLineSplit[2].toFloat(),
            quadBottomBound = csvLineSplit[3].toFloat(),
            quadRightBound = csvLineSplit[4].toFloat(),
            quadTopBound = csvLineSplit[5].toFloat(),
            slice = atlas.slice().apply {
                setSlice(
                    u = csvLineSplit[6].toFloat() / atlas.width,
                    v2 = csvLineSplit[7].toFloat()  / atlas.height,
                    u2 = csvLineSplit[8].toFloat()  / atlas.width,
                    v = csvLineSplit[9].toFloat() / atlas.height,
                )
            }
        )
    }

    companion object {
        private const val CSV_SEPARATOR = ','
    }
}