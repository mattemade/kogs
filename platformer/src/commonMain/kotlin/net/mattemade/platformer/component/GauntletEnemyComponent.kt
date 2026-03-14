package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Just a marker!
class GauntletEnemyComponent(
    
): Component<GauntletEnemyComponent> {
    override fun type() = GauntletEnemyComponent
    companion object: ComponentType<GauntletEnemyComponent>()
}