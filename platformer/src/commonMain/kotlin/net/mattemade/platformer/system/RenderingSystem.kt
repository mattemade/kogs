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
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.px
import net.mattemade.utils.msdf.MsdfFontRenderer

class RenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    val map: TiledMap = inject(),
) : IteratingSystem(family = family { all(PositionComponent, RotationComponent, SpriteComponent) }) {

    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH,
        virtualHeight = WORLD_UNIT_HEIGHT
    )
    private val camera = viewport.camera
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val mapScale = 1f / map.tileWidth
    private val playerLayerIndex = map.layers.indexOfFirst { it.name == "player-spawn" }
    private val mapLayers = map.layers.size

    private val mapFillsWidth = map.width >= WORLD_UNIT_WIDTH
    private val mapFillsHeight = map.height >= WORLD_UNIT_HEIGHT
    private val minCameraPosition = Vec2f(
        if (mapFillsWidth) HALF_WORLD_UNIT_WIDTH else map.width * 0.5f,
        if (mapFillsHeight) HALF_WORLD_UNIT_HEIGHT else map.height * 0.5f,
    )
    private val maxCameraPosition = Vec2f(
        if (mapFillsWidth) map.width - HALF_WORLD_UNIT_WIDTH else minCameraPosition.x,
        if (mapFillsHeight) map.height - HALF_WORLD_UNIT_HEIGHT else minCameraPosition.y,
    )
    private val fontRenderer = MsdfFontRenderer(gameContext.assets.font.fredokaMsdf)
    private val showTutorial = map.layers.any { it.name == "tutorial" }

    override fun onTick() {
        context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        context.gl.clearColor(Color.BLACK)

        val playerPosition = family.first { it.getOrNull(PlayerComponent) != null }[PositionComponent].position
        camera.position.set(
            playerPosition.x.clamp(minCameraPosition.x, maxCameraPosition.x).px,
            playerPosition.y.clamp(minCameraPosition.y, maxCameraPosition.y).px,
            0f
        )
        //camera.position.set(WORLD_UNIT_WIDTH * 0.5f, WORLD_UNIT_HEIGHT * 0.5f, 0f)
        viewport.apply(context)
        batch.begin(camera.viewProjection)
        renderLevel(from = 0, to = playerLayerIndex)
        super.onTick() // tick to render entities
        renderLevel(from = playerLayerIndex + 1, to = mapLayers)
        renderSideBars() // to cover any sprite that goes out-of-bounds

        if (showTutorial) {
            fontRenderer.drawAllTextAtOnce(batch) {
                draw(
                    """
                    arrows/WASD - walk/swim
                    Space - jump / double jump
                    down/S + Space - drop from platform
                    
                    Shift + move (land/air/water) -
                            dash while holding Shift
                    
                    Fall by wall to slide
                    
                    hold Tab to see the map
                    """.trimIndent(), 1f, 1f, 1f, batch
                )
            }
        }


        batch.end()
    }

    private fun renderLevel(from: Int, to: Int) {
        for (i in from until to) {
            renderLevelLayer(i)
        }
    }

    private fun renderLevelLayer(i: Int) {
        val layer = map.layers[i]
        val xOffset = (1f - layer.parallaxFactor.x) * (camera.position.x - minCameraPosition.x)
        val yOffset = (1f - layer.parallaxFactor.y) * (camera.position.y - minCameraPosition.y)
        layer.render(batch, camera = camera, x = xOffset.px, y = yOffset.px, scale = mapScale, displayObjects = true)
    }

    private fun renderSideBars() {
        if (!mapFillsWidth) { // fill black to left and right of the map
            shapeRenderer.filledRectangle(
                x = minCameraPosition.x - map.width * 0.5f,
                y = camera.position.y - HALF_WORLD_UNIT_HEIGHT,
                width = -WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
            shapeRenderer.filledRectangle(
                x = minCameraPosition.x + map.width * 0.5f,
                y = camera.position.y - HALF_WORLD_UNIT_HEIGHT,
                width = WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
        }
        if (!mapFillsHeight) { // fill black above and below of the map
            shapeRenderer.filledRectangle(
                x = camera.position.x - HALF_WORLD_UNIT_WIDTH,
                y = minCameraPosition.y - map.height * 0.5f,
                width = WORLD_UNIT_WIDTH,
                height = -WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
            shapeRenderer.filledRectangle(
                x = camera.position.x - HALF_WORLD_UNIT_WIDTH,
                y = minCameraPosition.y + map.height * 0.5f,
                width = WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
        }
    }

    override fun onTickEntity(entity: Entity) {
        val (sprite, bounds, tint) = entity[SpriteComponent]
        val (position) = entity[PositionComponent]
        val (rotation) = entity[RotationComponent]

        val angle = rotation.radians
        tempVec2f.set(bounds.x, bounds.y).rotate(angle)
        shapeRenderer.filledRectangle(
            x = (position.x + bounds.x).px,
            y = (position.y + bounds.y).px,
            width = bounds.width,
            height = bounds.height,
            color = bottomColor,
            color2 = bottomColor,
            color3 = tint,
            color4 = tint,
            rotation = angle
        )
    }

    companion object {
        private val tempVec2f = MutableVec2f()
        private val sideBarColor = Color.BLACK.toFloatBits()
        private val topColor = Color.RED.toFloatBits()
        private val bottomColor = Color.WHITE.toFloatBits()
    }

}