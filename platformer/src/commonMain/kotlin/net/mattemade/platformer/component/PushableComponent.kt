package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Just a marker!
class PushableComponent(
    
): Component<PushableComponent> {
    override fun type() = PushableComponent
    companion object: ComponentType<PushableComponent>()
}