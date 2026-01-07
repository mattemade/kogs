package com.littlekt.graphics.shader.generator.delegate

import com.littlekt.graphics.shader.generator.Instruction
import com.littlekt.graphics.shader.generator.GlslGenerator
import com.littlekt.graphics.shader.generator.type.scalar.GLInt
import com.littlekt.graphics.shader.generator.type.vec.Vec4
import kotlin.reflect.KProperty

class BuiltinIntDelegate {
    private lateinit var i: GLInt

    operator fun provideDelegate(
        thisRef: GlslGenerator,
        property: KProperty<*>
    ): BuiltinIntDelegate {
        i = GLInt(thisRef, property.name)
        return this
    }

    operator fun getValue(thisRef: GlslGenerator, property: KProperty<*>): GLInt {
        return i
    }

    operator fun setValue(thisRef: GlslGenerator, property: KProperty<*>, value: GLInt) {
        thisRef.instructions.add(Instruction.assign(property.name, value.value))
    }
}