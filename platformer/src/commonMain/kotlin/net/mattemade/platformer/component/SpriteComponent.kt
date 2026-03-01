package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect

data class SpriteComponent(
    val sprite: TextureSlice,
    val bounds: Rect,
    val tint: Float = Color.RED.toFloatBits(),
): Component<SpriteComponent> {
    override fun type() = SpriteComponent
    companion object: ComponentType<SpriteComponent>()
}