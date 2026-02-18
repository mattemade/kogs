package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class JumpComponent(
    var jumping: Boolean = false,
    var canJump: Boolean = false,
    var canHoldJumpForTicks: Int = MAX_JUMP_TICKS,
    var coyoteTimeInTicks: Int = COYOTE_TICKS,
    var jumpBuffer: Int = 0,
): Component<JumpComponent> {
    override fun type() = JumpComponent
    companion object: ComponentType<JumpComponent>() {
        const val MAX_JUMP_TICKS = 23
        const val COYOTE_TICKS = 10
        const val BUFFER_TICKS = 10
    }
}