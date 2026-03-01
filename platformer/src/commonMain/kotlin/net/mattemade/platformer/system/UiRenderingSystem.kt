package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import com.littlekt.math.floor
import com.littlekt.math.floorToInt
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PIXEL_PER_UNIT_FLOAT
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.UNITS_PER_PIXEL
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.UiComponent
import net.mattemade.platformer.px

class UiRenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    private val worldArea: Rect,
    private val mapTexture: () -> Texture?,
) : IteratingSystem(family = family { any(UiComponent, PlayerComponent) }) {

    private var mapScale = 0f
    private var mapUnitPerPixel = 0f
    private val mapPlacement = Rect()
    private val roomPlacementOnMap = Rect()
    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH,
        virtualHeight = WORLD_UNIT_HEIGHT
    )
    private val camera = viewport.camera.apply {
        position.set(HALF_WORLD_UNIT_WIDTH, HALF_WORLD_UNIT_HEIGHT, 0f)
    }
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)

    override fun onTick() {
        //context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        //context.gl.clearColor(Color.BLACK)

        val uiComponent = family.first { it.getOrNull(UiComponent) != null }[UiComponent]
        val player = family.first { it.getOrNull(PlayerComponent) != null }

        viewport.apply(context)
        batch.begin(camera.viewProjection)
        renderUi(player)
        if (uiComponent.showMap) {
            renderMap(player)
        }
        batch.end()
    }

    private fun renderUi(player: Entity) {
        for (i in 0 until player[HealthComponent].health.floorToInt()) {
            shapeRenderer.filledRectangle(x = 0.25f + i * 0.6f, y = 0.25f, width = 0.5f, height = 0.5f, color = Color.RED.toFloatBits())
        }
    }

    override fun onTickEntity(entity: Entity) {
        // no-op
    }

    private fun renderMap(player: Entity) {
        shapeRenderer.filledRectangle(
            x = 0f,
            y = 0f,
            width = WORLD_UNIT_WIDTH,
            height = WORLD_UNIT_HEIGHT,
            color = mapBackgroundColor
        )
        mapTexture()?.let { texture ->
            if (mapScale == 0f) {
                val offset = 1f
                val doubleOffset = offset * 2f
                mapScale = minOf(
                    (WORLD_UNIT_WIDTH - doubleOffset) / texture.width,
                    (WORLD_UNIT_HEIGHT - doubleOffset) / texture.height
                )
                mapScale = (mapScale * PIXEL_PER_UNIT_FLOAT).floor() * UNITS_PER_PIXEL // to maintain pixel-perfect integer map scaling
                val width = texture.width * mapScale
                val height = texture.height * mapScale
                val horizontalOffset = (WORLD_UNIT_WIDTH - width) * 0.5f
                val verticalOffset = (WORLD_UNIT_HEIGHT - height) * 0.5f
                mapUnitPerPixel = (width * PIXEL_PER_UNIT_FLOAT) / texture.width.toFloat()
                mapPlacement.set(horizontalOffset.px, verticalOffset.px, width.px, height.px)

                roomPlacementOnMap.set(
                    worldArea.x.xOnMap,
                    worldArea.y.yOnMap,
                    worldArea.width * mapScale,
                    worldArea.height * mapScale
                )
            }
            shapeRenderer.filledRectangle(rect = roomPlacementOnMap, color = roomBackgroundColor)
            batch.draw(
                texture,
                x = mapPlacement.x,
                y = mapPlacement.y,
                width = mapPlacement.width,
                height = mapPlacement.height,
                flipY = true
            )

            player.getOrNull(PositionComponent)?.let { position ->
                shapeRenderer.filledRectangle(
                    x = (roomPlacementOnMap.x + (position.position.x - 0.5f) * mapScale).px,
                    y = (roomPlacementOnMap.y + (position.position.y - 1f) * mapScale).px,
                    width = mapScale,
                    height = mapScale * 2f,
                    color = playerColor
                )
            }
        }
    }

    private val Float.xOnMap: Float get() = mapPlacement.x + (this - gameContext.worldSize.x) * mapScale
    private val Float.yOnMap: Float get() = mapPlacement.y + (this - gameContext.worldSize.y) * mapScale

    companion object {
        private val mapBackgroundColor = Color.BLACK.toMutableColor().apply { a = 0.5f }.toFloatBits()
        private val roomBackgroundColor = Color.BLUE.toMutableColor().apply { a = 0.5f }.toFloatBits()
        private val playerColor = Color.RED.toFloatBits()
    }

}