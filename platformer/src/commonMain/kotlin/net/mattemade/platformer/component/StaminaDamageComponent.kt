package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class StaminaDamageComponent(
    var damage: Float = 1f,
    var everyTicks: Int = 100,
    var currentTick: Int = 0,
): Component<StaminaDamageComponent> {
    override fun type() = StaminaDamageComponent
    companion object: ComponentType<StaminaDamageComponent>()


}