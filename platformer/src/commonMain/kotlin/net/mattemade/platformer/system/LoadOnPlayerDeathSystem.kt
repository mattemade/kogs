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

class LoadOnPlayerDeathSystem(
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(HealthComponent, PlayerComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        if (entity[HealthComponent].health <= 0f) {
            gameContext.load(forceRestart = true)
        }
    }
}