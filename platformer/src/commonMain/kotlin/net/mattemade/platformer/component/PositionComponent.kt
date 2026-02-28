package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f

data class PositionComponent(
    val position: MutableVec2f = MutableVec2f(),
): Component<PositionComponent> {
    override fun type() = PositionComponent
    companion object: ComponentType<PositionComponent>()
}