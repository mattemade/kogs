package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f

class MomentaryForceComponent(
    val force: MutableVec2f = MutableVec2f(), // maybe it should be a mutable list?
): Component<MomentaryForceComponent> {
    override fun type() = MomentaryForceComponent
    companion object: ComponentType<MomentaryForceComponent>()
}