package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f

data class MascotComponent(
    val offset: Vec2f,
    val playerEntity: Entity,
    val targetPosition: MutableVec2f = MutableVec2f()
): Component<MascotComponent> {
    override fun type() = MascotComponent
    companion object: ComponentType<MascotComponent>()
}