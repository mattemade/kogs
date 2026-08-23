package net.mattemade.latencytest.ecs

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key

class FakePhysicsSystem(
    private val context: Context = inject(),
) : IteratingSystem(family {
    all(ControllableComponent)
}, interval = Fixed(1 / 50f)) {

    override fun onTickEntity(entity: Entity) {
        entity[ControllableComponent].pressed = false
    }
}