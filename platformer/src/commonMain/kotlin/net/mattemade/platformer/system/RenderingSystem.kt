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
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.Triangulator
import com.littlekt.util.Scaler
import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.px

class RenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    val map: TiledMap = inject(),
    val testPolygons: List<FloatArrayList>,
) : IteratingSystem(family = family { all(PositionComponent, SpriteComponent) }) {

    val triangulator = Triangulator()
    val testPolygonTrinalges by lazy { testPolygons.map { it.toFloatArray() to triangulator.computeTriangles(it).toShortArray() } }

    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH.toFloat(),
        virtualHeight = WORLD_UNIT_HEIGHT.toFloat()
    )
    private val camera = viewport.camera
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val mapScale = 1f / map.tileWidth
    private val playerLayerIndex = map.layers.indexOfFirst { it.name == "player-spawn" }
    private val mapLayers = map.layers.size

    private val minCameraPosition = Vec2f(
        if (map.width >= WORLD_UNIT_WIDTH) HALF_WORLD_UNIT_WIDTH else (WORLD_UNIT_WIDTH - map.width) * 0.5f,
        if (map.height >= WORLD_UNIT_HEIGHT) HALF_WORLD_UNIT_HEIGHT else (WORLD_UNIT_HEIGHT - map.height) * 0.5f,
    )
    private val maxCameraPosition = Vec2f(
        if (map.width >= WORLD_UNIT_WIDTH) map.width - HALF_WORLD_UNIT_WIDTH else minCameraPosition.x,
        if (map.height >= WORLD_UNIT_HEIGHT) map.height - HALF_WORLD_UNIT_HEIGHT else minCameraPosition.y,
    )

    override fun onTick() {
        context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        context.gl.clearColor(Color.BLACK)

        // TODO: there will be other creatues in the family, we should specify to pick the Player
        val playerPosition = family.first()[PositionComponent].position
        camera.position.set(playerPosition.x.clamp(minCameraPosition.x, maxCameraPosition.x).px, playerPosition.y.clamp(minCameraPosition.y, maxCameraPosition.y).px, 0f)
        //camera.position.set(WORLD_UNIT_WIDTH * 0.5f, WORLD_UNIT_HEIGHT * 0.5f, 0f)
        viewport.apply(context)
        batch.begin(camera.viewProjection)
        renderMap(from = 0, to = playerLayerIndex)
        super.onTick() // tick to render entities
        renderMap(from = playerLayerIndex+1, to = mapLayers)

        testPolygonTrinalges.forEach {
            shapeRenderer.filledPolygon(it.first, it.second)
        }

        batch.end()
    }

    private fun renderMap(from: Int, to: Int) {
        for (i in from until to) {
            renderLayer(i)
        }
    }

    private fun renderLayer(i: Int) {
        val layer = map.layers[i]
        val xOffset = (1f - layer.parallaxFactor.x) * (camera.position.x - minCameraPosition.x)
        val yOffset = (1f - layer.parallaxFactor.y) * (camera.position.y - minCameraPosition.y)
        layer.render(batch, camera = camera, x = xOffset.px, y = yOffset.px, scale = mapScale, displayObjects = true)
    }

    override fun onTickEntity(entity: Entity) {
        val bounds = entity[SpriteComponent].bounds
        val position = entity[PositionComponent].position

        shapeRenderer.filledRectangle(
            x = (position.x + bounds.x).px,
            y = (position.y + bounds.y).px,
            width = bounds.width,
            height = bounds.height,
            color = Color.RED.toFloatBits()
        )
    }

}