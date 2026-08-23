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

class ControlsSystem(
    private val context: Context = inject(),
) : IteratingSystem(family {
    all(ControllableComponent)
}) {

    var isPressed = false

    init {
        context.input.addActiveInputProcessor(object: InputProcessor {
            override fun keyDown(key: Key): Boolean {
                isPressed = true
                return super.keyDown(key)
            }

            override fun keyUp(key: Key): Boolean {
                isPressed = false
                return super.keyUp(key)
            }
        })
    }

    override fun onTickEntity(entity: Entity) {
        entity[ControllableComponent].pressed = isPressed
    }
}