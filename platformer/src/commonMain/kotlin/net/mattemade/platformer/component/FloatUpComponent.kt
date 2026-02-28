package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class FloatUpComponent(
    var floatUpVelocity: Float = 0f,
    var floatUpAcceleration: Float = 0f,
    var floatUpVelocityLimit: Float = 1f,
): Component<FloatUpComponent> {
    override fun type() = FloatUpComponent
    companion object: ComponentType<FloatUpComponent>()
}