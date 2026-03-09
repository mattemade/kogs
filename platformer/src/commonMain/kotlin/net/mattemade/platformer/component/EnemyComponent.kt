package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import net.mattemade.platformer.resources.ResourceEnemy
import net.mattemade.platformer.system.EnemyBehaviourSystem

class EnemyComponent(
    val enemy: ResourceEnemy,
    var currentIntent: EnemyBehaviourSystem.Intent? = null,
): Component<EnemyComponent> {
    override fun type() = EnemyComponent
    companion object: ComponentType<EnemyComponent>()
}