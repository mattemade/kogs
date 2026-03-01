package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.StaminaDamageComponent

class StaminaBreathingSystem(
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(StaminaComponent, FloatUpComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        if (entity[FloatUpComponent].floatUpAcceleration != 0f) {
            entity[StaminaComponent].apply {
                stamina -= 0.0025f
                staminaPressure = true
                restoreAfterTicks = 100
            }
        }
    }
}