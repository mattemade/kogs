package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.UiComponent

class UiControlsSystem(
    private val context: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family { all(UiComponent) }, interval = interval) {

    private val input = context.gameInput

    override fun onTickEntity(entity: Entity) {
        entity[UiComponent].showMap = input.map.pressed
    }
}
