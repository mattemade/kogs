package net.mattemade.latencytest.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ControllableComponent(
    var pressed: Boolean = false,
): Component<ControllableComponent> {
    override fun type() = ControllableComponent
    companion object: ComponentType<ControllableComponent>()
}