package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class RotationComponent(
    var currentRotation: Float = 0f,
    var targetRotation: Float = 0f,
    var maxRotationVelocity: Float = 1f,
): Component<RotationComponent> {
    override fun type() = RotationComponent
    companion object: ComponentType<RotationComponent>()
}