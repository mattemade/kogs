package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f

class MoveComponent(
    var speed: Float = 0f,
    val moveDirection: MutableVec2f = MutableVec2f(),
    val dashDirection: MutableVec2f = MutableVec2f(),
    var fallThrough: Boolean = false,
): Component<MoveComponent> {
    override fun type() = MoveComponent
    companion object: ComponentType<MoveComponent>()
}