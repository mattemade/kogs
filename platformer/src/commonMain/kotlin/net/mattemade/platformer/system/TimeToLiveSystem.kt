package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import net.mattemade.platformer.component.TimeToLiveComponent

class TimeToLiveSystem(
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(TimeToLiveComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val time = entity[TimeToLiveComponent]
        time.timeToLive -= deltaTime
        if (time.timeToLive <= 0f) {
            entity.remove()
        }
    }
}