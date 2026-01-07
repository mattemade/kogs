package com.littlekt.graphics.shader.generator.type.func

import com.littlekt.graphics.shader.generator.GlslGenerator
import com.littlekt.graphics.shader.generator.type.Func
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class Void(override val builder: GlslGenerator) : Func<Unit> {
    override val typeName: String = "void"
    override var value: String? = null
    override val type: KClass<Unit> = Unit::class
}