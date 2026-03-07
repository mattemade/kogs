package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f

data class CheckpointComponent(
    var id: Int,
    var isActivated: Boolean = false,
): Component<CheckpointComponent> {
    override fun type() = CheckpointComponent
    companion object: ComponentType<CheckpointComponent>()
}