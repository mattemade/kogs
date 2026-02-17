package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.Rect

class SpriteComponent(
    val sprite: TextureSlice,
    val bounds: Rect,
): Component<SpriteComponent> {
    override fun type() = SpriteComponent
    companion object: ComponentType<SpriteComponent>()
}