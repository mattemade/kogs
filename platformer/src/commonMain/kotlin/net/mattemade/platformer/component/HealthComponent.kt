package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class HealthComponent(
    var health: Float = 3f,
    var maxHealth: Float = 3f,
): Component<HealthComponent> {
    override fun type() = HealthComponent
    companion object: ComponentType<HealthComponent>()
}