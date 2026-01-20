package net.mattemade.utils.msdf

import com.littlekt.graphics.g2d.Batch

class MsdfFontRenderer(val font: MsdfFont) {

    inline fun drawAllTextAtOnce(batch: Batch, crossinline drawBlock: MsdfFont.() -> Unit) {
        val shader = batch.shader

        batch.shader = MsdfFontShader.program
        font.drawBlock()
        batch.shader = shader
    }

}
