package com.littlekt.graphics

import com.littlekt.OffscreenCanvas
import com.littlekt.OffscreenCanvasRenderingContext2D
import com.littlekt.Releasable
import com.littlekt.file.ByteBufferImpl
import com.littlekt.graphics.gl.TextureFormat
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.ImageData

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor actual constructor(
    actual val pixmap: Pixmap,
    actual val xHotspot: Int,
    actual val yHotSpot: Int
) : Releasable {
    val cssCursorProperty: String

    init {
        check(pixmap.glFormat == TextureFormat.RGBA) { "Cursor image pixmap is not in RGBA8888 format." }
        check((pixmap.width and (pixmap.width - 1)) == 0) {
            "Cursor image pixmap width of ${pixmap.width} is not a power-of-two greater than zero."
        }
        check((pixmap.height and (pixmap.height - 1)) == 0) {
            "Cursor image pixmap height of ${pixmap.height} is not a power-of-two greater than zero."
        }
        check(xHotspot > 0 && xHotspot < pixmap.width) {
            "xHotspot coordinate of $xHotspot is not within image width bounds: [0, ${pixmap.width})."
        }
        check(yHotSpot > 0 && yHotSpot < pixmap.height) {
            "yHotSpot coordinate of $yHotSpot is not within image width bounds: [0, ${pixmap.height})."
        }
        val canvas = OffscreenCanvas(pixmap.width, pixmap.height)
        val canvasCtx = canvas.getContext("2d") as OffscreenCanvasRenderingContext2D
        canvasCtx.putImageData(
            ImageData(
                Uint8ClampedArray((pixmap.pixels as ByteBufferImpl).buffer.buffer),
                pixmap.width,
                pixmap.height
            ), 0.0, 0.0
        )
        val dataUrl = canvas.toDataURL("image/png")
        cssCursorProperty = "url('${dataUrl}')$xHotspot $yHotSpot,auto"
    }

    actual override fun release() = Unit

}