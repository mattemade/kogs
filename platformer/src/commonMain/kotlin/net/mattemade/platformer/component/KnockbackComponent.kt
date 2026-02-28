package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Disabled controls while on knockback
class KnockbackComponent(
    var atLeastForTicks: Int = 10,
    var ticksToWearOff: Int = 50,
    var canStop: Boolean = false,
): Component<KnockbackComponent> {
    override fun type() = KnockbackComponent
    companion object: ComponentType<KnockbackComponent>()
}