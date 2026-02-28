package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Just a marker!
class PlayerComponent(
    
): Component<PlayerComponent> {
    override fun type() = PlayerComponent
    companion object: ComponentType<PlayerComponent>()
}