package com.littlekt.graphics.gl

import com.littlekt.Context
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.TextureData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class GLTextureData(
    override val width: Int,
    override val height: Int,
    val mipMapLevel: Int,
    val internalGlFormat: TextureFormat,
    val glFormat: TextureFormat,
    val type: DataType
) : TextureData {
    override val format: Pixmap.Format = Pixmap.Format.RGBA8888
    override val useMipMaps: Boolean = false
    override var isPrepared: Boolean = false
    override val isCustom: Boolean = true

    override fun prepare() {
        if (isPrepared) {
            throw RuntimeException("Already prepared!")
        }
        isPrepared = true
    }

    override fun consumePixmap(): Pixmap {
        throw RuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun consumeCustomData(context: Context, target: TextureTarget) {
        val gl = context.graphics.gl
        gl.texImage2D(target, mipMapLevel, internalGlFormat, glFormat, width, height, type)
    }
}