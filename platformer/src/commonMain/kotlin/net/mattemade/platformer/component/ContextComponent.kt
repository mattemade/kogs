package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ContextComponent(
    var touchingLeftWall: Boolean = false,
    var touchingRightWall: Boolean = false,
    var facingRight: Boolean = false,
    var standing: Boolean = false,
    var wallSlide: Boolean = false,
    var swimming: Boolean = false,
    var dashing: Boolean = false,
): Component<ContextComponent> {
    override fun type() = ContextComponent
    companion object: ComponentType<ContextComponent>()
}