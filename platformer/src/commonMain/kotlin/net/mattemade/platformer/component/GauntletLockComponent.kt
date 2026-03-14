package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Just a marker!
class GauntletLockComponent(
    
): Component<GauntletLockComponent> {
    override fun type() = GauntletLockComponent
    companion object: ComponentType<GauntletLockComponent>()
}