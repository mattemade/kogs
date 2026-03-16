package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class HealthComponent(
    var health: Float = 5f,
    var maxHealth: Float = 5f,
): Component<HealthComponent> {
    override fun type() = HealthComponent
    companion object: ComponentType<HealthComponent>()
}