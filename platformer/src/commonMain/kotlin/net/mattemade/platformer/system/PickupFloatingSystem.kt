package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.clamp
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PickupComponent
import kotlin.math.cos
import kotlin.math.sin

class PickupFloatingSystem(
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(family { all(PickupComponent, MoveComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val pickupComponent = entity[PickupComponent]
        val moveComponent = entity[MoveComponent]

        pickupComponent.floatingTime += deltaTime * 3f

        moveComponent.moveDirection.y = cos(pickupComponent.floatingTime) * 0.25f
    }
}