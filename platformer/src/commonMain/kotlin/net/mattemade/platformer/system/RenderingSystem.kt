package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.toFloatBits
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent

class RenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    val map: TiledMap = inject(),
) : IteratingSystem(family = family { all(PositionComponent, SpriteComponent) }) {


    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH.toFloat(),
        virtualHeight = WORLD_UNIT_HEIGHT.toFloat()
    )
    private val camera = viewport.camera
    private val batch = SpriteBatch(context)
    private val shapeRenderingSystem = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val mapScale = 1f / map.tileWidth

    override fun onTick() {
        camera.position.set(WORLD_UNIT_WIDTH * 0.5f, WORLD_UNIT_HEIGHT * 0.5f, 0f)
        viewport.apply(context)
        batch.begin(camera.viewProjection)
        map.render(batch, camera, scale = mapScale)

        super.onTick() // tick entities

        batch.end()
    }

    override fun onTickEntity(entity: Entity) {
        val bounds = entity[SpriteComponent].bounds
        val position = entity[PositionComponent].position
        camera.position.set(position.x, position.y, 0f)
        camera.update()
        shapeRenderingSystem.filledRectangle(
            x = position.x,
            y = position.y,
            width = bounds.width,
            height = bounds.height,
            color = Color.RED.toFloatBits()
        )
    }

}