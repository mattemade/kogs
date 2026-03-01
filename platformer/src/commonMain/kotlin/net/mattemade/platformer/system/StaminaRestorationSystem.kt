package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.StaminaComponent

class StaminaRestorationSystem(
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(StaminaComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        entity[StaminaComponent].apply {
            staminaPressure = false
            if (stamina < 0f) {
                stamina = 0f
            }
            if (restoreAfterTicks == 0) {
                if (stamina < maxStamina) {
                    stamina = minOf(maxStamina, stamina + restorationRate)
                }
            } else {
                restoreAfterTicks--
            }
        }
    }
}