package net.mattemade.bigmode.resources

import com.littlekt.graphics.Color

class PowerUpSpec(
    val availableFrom: Int,
    val sprite: String,
    val tint: Color,
    val activeTimer: Float,
    val speedBoost: Boolean,
    val stickyFingers: Boolean,
    val calmDown: Boolean,
    val stackStabilizer: Boolean,
    val cleanup: Boolean
)