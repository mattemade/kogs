package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.input.Key
import com.littlekt.math.MutableVec2f
import net.mattemade.platformer.SWIM_ACCELERATION
import net.mattemade.platformer.SWIM_VELOCITY
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.UiComponent
import kotlin.math.sign

class UiControlsSystem(
    private val context: Context = inject(),
    interval: Interval = Fixed(1 / 200f),
    ): IteratingSystem(family { all(UiComponent)}, interval = interval) {

    private val input = context.input
    private var jumpPressed = false

    override fun onTickEntity(entity: Entity) {
        entity[UiComponent].showMap = input.isKeyPressed(Key.TAB)
    }
}
