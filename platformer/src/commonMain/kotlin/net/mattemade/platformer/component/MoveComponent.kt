package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.MutableVec2f

class MoveComponent(
    val maxMoveSpeed: Float = 1f,
    var speed: Float = 1f,
    val moveDirection: MutableVec2f = MutableVec2f(),
    val dashDirection: MutableVec2f = MutableVec2f(),
    var fallThrough: Boolean = false,
    var forceStopAirDash: Boolean = false,
    var forceStopWaterDash: Boolean = false,
    var ignoreNextDashDirection: Boolean = false,
): Component<MoveComponent> {
    override fun type() = MoveComponent
    companion object: ComponentType<MoveComponent>()
}