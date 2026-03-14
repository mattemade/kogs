package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class StaminaComponent(
    var stamina: Float = 1f,
    var maxStamina: Float = 1f,
    var restoreAfterTicks: Int = 0,
    var restorationRate: Float = 0.005f,
    var staminaPressure: Boolean = false,
    var keepVisibleForExtraTime: Float = 0f,
): Component<StaminaComponent> {
    override fun type() = StaminaComponent
    companion object: ComponentType<StaminaComponent>()


}