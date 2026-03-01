package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.StaminaDamageComponent

class LowStaminaDamageSystem(
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(StaminaComponent, HealthComponent, StaminaDamageComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val healthComponent = entity[HealthComponent]
        val damageComponent = entity[StaminaDamageComponent]
        val staminaComponent = entity[StaminaComponent]

        if (staminaComponent.stamina <= 0f && staminaComponent.staminaPressure) {
            damageComponent.currentTick++
            while (damageComponent.currentTick >= damageComponent.everyTicks) {
                healthComponent.health -= damageComponent.damage
                damageComponent.currentTick -= damageComponent.everyTicks
            }
        } else {
            damageComponent.currentTick = 0
        }
    }
}