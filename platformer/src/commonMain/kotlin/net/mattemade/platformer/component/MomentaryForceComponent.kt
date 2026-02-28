package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f

class MomentaryForceComponent(
    val forces: MutableList<Vec2f> = mutableListOf(),
): Component<MomentaryForceComponent> {
    override fun type() = MomentaryForceComponent
    companion object: ComponentType<MomentaryForceComponent>()
}