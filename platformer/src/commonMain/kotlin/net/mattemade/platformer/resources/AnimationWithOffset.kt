package net.mattemade.platformer.resources

import com.littlekt.math.Vec2f
import net.mattemade.utils.animation.SignallingAnimationPlayer

data class AnimationWithOffset(val animation: SignallingAnimationPlayer, val offset: Vec2f, val scale: Float,)