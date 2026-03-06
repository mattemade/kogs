package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Just a marker!
class EnemyComponent(
    
): Component<EnemyComponent> {
    override fun type() = EnemyComponent
    companion object: ComponentType<EnemyComponent>()
}