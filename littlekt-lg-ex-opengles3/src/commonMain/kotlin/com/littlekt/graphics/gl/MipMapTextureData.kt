package com.littlekt.graphics.gl

import com.littlekt.Context
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.TextureData
import com.littlekt.graphics.uploadImageData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class MipMapTextureData(vararg mipMapData: TextureData) : TextureData {
    val mips = arrayOf(*mipMapData)

    override val format: Pixmap.Format = mips[0].format
    override val width: Int = mips[0].width
    override val height: Int = mips[0].height
    override val useMipMaps: Boolean = false
    override val isPrepared: Boolean = true
    override val isCustom: Boolean = true

    override fun prepare() = Unit

    override fun consumePixmap(): Pixmap {
        throw RuntimeException("It's compressed, use the compressed method")
    }

    override fun consumeCustomData(context: Context, target: TextureTarget) {
        mips.forEachIndexed { index, mip ->
            uploadImageData(context, target, mip, index)
        }
    }
}