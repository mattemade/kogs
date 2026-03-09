package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import net.mattemade.fmod.FmodEventInstance

class ContextComponent(
    var touchingLeftWall: Boolean = false,
    var touchingRightWall: Boolean = false,
    var facingRight: Boolean = false,
    var standing: Boolean = false,
    var standingLeftFoot: Boolean = false,
    var standingRightFoot: Boolean = false,
    var wallSlide: Boolean = false,
    var swimming: Boolean = false,
    var dashing: Boolean = false,
    var swimmingSound: FmodEventInstance? = null,
    var dashingSound: FmodEventInstance? = null,
    var slidingSound: FmodEventInstance? = null,
): Component<ContextComponent> {
    override fun type() = ContextComponent
    companion object: ComponentType<ContextComponent>()
}