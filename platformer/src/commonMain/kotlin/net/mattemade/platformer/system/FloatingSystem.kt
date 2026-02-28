package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.clamp
import net.mattemade.platformer.component.FloatUpComponent

class FloatingSystem(
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(FloatUpComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val (speed, acceleration, limit) = entity[FloatUpComponent]
        entity[FloatUpComponent].floatUpVelocity = if (acceleration != 0f) {
            (speed + acceleration).clamp(-limit, limit)
        } else{
            0f
        }
    }
}