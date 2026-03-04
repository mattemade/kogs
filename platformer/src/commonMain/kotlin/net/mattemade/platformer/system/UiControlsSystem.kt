package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.Key
import net.mattemade.platformer.component.UiComponent

class UiControlsSystem(
    private val context: Context = inject(),
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family { all(UiComponent) }, interval = interval) {

    private val input = context.input
    private var jumpPressed = false

    override fun onTickEntity(entity: Entity) {
        entity[UiComponent].showMap = input.isKeyPressed(Key.TAB)
    }
}
