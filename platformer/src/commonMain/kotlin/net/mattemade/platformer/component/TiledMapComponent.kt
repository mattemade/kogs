package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap

class TiledMapComponent(
    val map: TiledMap,
): Component<TiledMapComponent> {
    override fun type() = TiledMapComponent
    companion object: ComponentType<TiledMapComponent>()
}