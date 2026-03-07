package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class InvincibilityComponent(
    var timeLeft: Float = 1.5f,
): Component<InvincibilityComponent> {
    override fun type() = InvincibilityComponent
    companion object: ComponentType<InvincibilityComponent>()
}