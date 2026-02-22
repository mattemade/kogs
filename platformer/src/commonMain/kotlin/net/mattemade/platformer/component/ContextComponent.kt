package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class ContextComponent(
    var touchingWalls: Boolean = false,
    var facingRight: Boolean = false,
    var standing: Boolean = false,
    var wallSlide: Boolean = false,
): Component<ContextComponent> {
    override fun type() = ContextComponent
    companion object: ComponentType<ContextComponent>()
}