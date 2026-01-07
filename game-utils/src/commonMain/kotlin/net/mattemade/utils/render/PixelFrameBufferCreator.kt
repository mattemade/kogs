package net.mattemade.utils.render

import com.littlekt.Context
import com.littlekt.graphics.FrameBuffer
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter

fun Context.createPixelFrameBuffer(width: Int, height: Int, allowFiltering: Boolean = false) =
    FrameBuffer(
        width,
        height,
        listOf(
            FrameBuffer.TextureAttachment(
                minFilter = if (allowFiltering) TexMinFilter.LINEAR else TexMinFilter.NEAREST,
                magFilter = if (allowFiltering) TexMagFilter.LINEAR else TexMagFilter.NEAREST,
            )
        )
    ).also {
        it.prepare(this)
    }