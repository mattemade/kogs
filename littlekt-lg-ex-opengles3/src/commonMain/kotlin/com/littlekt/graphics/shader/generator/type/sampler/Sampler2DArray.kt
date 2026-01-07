package com.littlekt.graphics.shader.generator.type.sampler

import com.littlekt.graphics.shader.generator.GlslGenerator
import com.littlekt.graphics.shader.generator.type.Variable

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
class Sampler2DArray(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "sampler2DArray"
    override var value: String? = null
}