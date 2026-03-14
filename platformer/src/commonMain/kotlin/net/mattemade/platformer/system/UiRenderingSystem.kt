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
import com.littlekt.math.Vec2f
import com.littlekt.math.floor
import com.littlekt.math.floorToInt
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.gui.api.math.Vec2
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
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.UiComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.scene.PlatformingScene
import net.mattemade.utils.msdf.MsdfFontRenderer

class UiRenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    private val worldArea: Rect,
    private val mapVisible: Boolean,
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
    private val fontRenderer = MsdfFontRenderer(gameContext.assets.font.verdanaBoldMsdf)
    private val heartEmpty = gameContext.assets.textureFiles.heartEmpty
    private val heartFull = gameContext.assets.textureFiles.heartFull
    private val heartSize =
        Vec2f(heartFull.width.toFloat() / PIXEL_PER_UNIT_FLOAT, heartFull.height.toFloat() / PIXEL_PER_UNIT_FLOAT)
    private val heartDistance = heartSize.x + UNITS_PER_PIXEL

    override fun onTick() {
        //context.gl.clearColor(Color.BLACK)
        //context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        val uiComponent = family.firstOrNull() { it.getOrNull(UiComponent) != null }?.get(UiComponent) ?: return
        val player = family.first { it.getOrNull(PlayerComponent) != null }

        viewport.apply(context)
        batch.begin(camera.viewProjection)
        renderUi(player, uiComponent)
        if (uiComponent.showMap) {
            renderMap(player)
            fontRenderer.drawAllTextAtOnce(batch) {

                if (collectionText == null) {
                    val swordText = if (gameContext.gameState.sword) "Sword      " else ""
                    val waterPearlText = if (gameContext.gameState.waterPearl) "Water Pearl      " else ""
                    val airPearlText = if (gameContext.gameState.airPearl) "Air Pearl      " else ""
                    collectionText =
                        "$swordText$waterPearlText$airPearlText${PlatformingScene.collectedPearls} / ${PlatformingScene.nextPearlId} pearls      ${PlatformingScene.visitedRooms * 100 / PlatformingScene.totalRooms}% explored".also {
                            measure(it, 0.5f, collectionTextPlacement)
                            collectionTextPlacement.x = HALF_WORLD_UNIT_WIDTH - collectionTextPlacement.x * 0.5f
                            collectionTextPlacement.y = WORLD_UNIT_HEIGHT - 1f
                        }
                }
                draw(collectionText!!, collectionTextPlacement.x, collectionTextPlacement.y, 0.5f, batch)
            }
        }
        batch.end()
    }

    private fun renderUi(player: Entity, uiComponent: UiComponent) {
        val (health, maxHealth) = player[HealthComponent]
        for (i in 0 until maxHealth.floorToInt()) {
            batch.draw(
                slice = if (i < health) heartFull else heartEmpty,
                x = 0.25f + i * heartDistance,
                y = 0.25f,
                width = heartSize.x,
                height = heartSize.y,
            )
        }
/*        */

        uiComponent.showTutorial?.let {
            fontRenderer.drawAllTextAtOnce(batch) {
                draw(it, 1f, 1f, 1f, batch)
            }
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
                mapScale =
                    (mapScale * PIXEL_PER_UNIT_FLOAT).floor() * UNITS_PER_PIXEL // to maintain pixel-perfect integer map scaling
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

            if (mapVisible) {
                shapeRenderer.filledRectangle(rect = roomPlacementOnMap, color = roomBackgroundColor)
                batch.draw(
                    texture,
                    x = mapPlacement.x,
                    y = mapPlacement.y,
                    width = mapPlacement.width,
                    height = mapPlacement.height,
                    flipY = true
                )
            }


            player.getOrNull(PositionComponent)?.let { position ->
                shapeRenderer.filledRectangle(
                    x = (roomPlacementOnMap.x + (position.position.x - 2f) * mapScale).px,
                    y = (roomPlacementOnMap.y + (position.position.y - 2f) * mapScale).px,
                    width = mapScale * 4f,
                    height = mapScale * 4f,
                    color = playerColor
                )
            }
        }
    }

    private val Float.xOnMap: Float get() = mapPlacement.x + (this - gameContext.worldSize.x) * mapScale
    private val Float.yOnMap: Float get() = mapPlacement.y + (this - gameContext.worldSize.y) * mapScale

    companion object {
        private val tempVec2 = Vec2.borrow()
        private val mapBackgroundColor = Color.BLACK.toMutableColor().apply { a = 0.5f }.toFloatBits()
        private val roomBackgroundColor =
            Color.GREEN.toMutableColor().apply { a = 0.75f }.toFloatBits()
        private val playerColor = Color.RED.toFloatBits()
        var collectionText: String? = null
        private val collectionTextPlacement = Vec2.borrow()
    }

}