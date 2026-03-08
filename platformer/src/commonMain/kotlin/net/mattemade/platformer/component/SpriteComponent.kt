package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.graphics.Color
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import net.mattemade.platformer.resources.AnimationWithOffset

data class SpriteComponent(
    val idleAnimation: AnimationWithOffset,
    val walkAnimation: AnimationWithOffset = idleAnimation,
    val jumpAnimation: AnimationWithOffset = idleAnimation,
    val fallAnimation: AnimationWithOffset = idleAnimation,
    val swimAnimation: AnimationWithOffset = idleAnimation,
    val wallSlideAnimation: AnimationWithOffset = idleAnimation,
    val swimIdleAnimation: AnimationWithOffset = idleAnimation,
    val swimDashAnimation: AnimationWithOffset = idleAnimation,
    val airDashAnimation: AnimationWithOffset = idleAnimation,
    val hurtAnimation: AnimationWithOffset = idleAnimation,
    val animationEventCallback: (String, Box2DPhysicsComponent) -> Unit,
    val bounds: Rect,
    var tint: Float = Color.RED.toFloatBits(),
    val priority: Int = 0,
    var visible: Boolean = true,
) : Component<SpriteComponent> {

    var currentAnimation = idleAnimation
        set(value) {
            if (field != value) {
                field = value
                value.animation.restart()
            }
        }

    override fun type() = SpriteComponent

    companion object : ComponentType<SpriteComponent>()
}