package net.mattemade.platformer.scene

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.gui.api.math.Vec2
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.utils.msdf.MsdfFontRenderer
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import kotlin.math.sin

class SplashScene(val gameContext: PlatformerGameContext) : Scene, Releasing by Self() {

    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH,
        virtualHeight = WORLD_UNIT_HEIGHT
    )
    private val camera = viewport.camera
    private val playerLightPosition = MutableVec2f()
    private val batch = SpriteBatch(gameContext.context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val fontRenderer = MsdfFontRenderer(gameContext.assets.font.fredokaMsdf)
    private var touched: Boolean = false

    private var time: Float = 0f

    override fun update(seconds: Float) {
        time += seconds * 4f
        gameContext.context.gl.clearColor(Color.BLACK)
        gameContext.context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        camera.position.set(
            HALF_WORLD_UNIT_WIDTH,
            HALF_WORLD_UNIT_HEIGHT,
            0f
        )

        viewport.apply(gameContext.context)
        batch.begin(camera.viewProjection)

        fontRenderer.drawAllTextAtOnce(batch) {
            val scale = 1f + sin(time) * 0.1f
            measure(clickToStartText, scale, tempVec2)
            draw(
                clickToStartText, HALF_WORLD_UNIT_WIDTH - tempVec2.x * 0.5f, 2f - tempVec2.y * 0.5f, scale = scale, batch
            )
        }

        batch.end()

        if (gameContext.context.input.isTouching) {
            gameContext.startGame()
        }
    }

    override fun render(
        batch: Batch, shapeRenderer: ShapeRenderer
    ) {

    }

    companion object {
        private val clickToStartText = "Click to start"
        private val tempVec2f = MutableVec2f()
        private val tempVec2 = Vec2.borrow()
        private val mapColor = Color.WHITE.toMutableColor().apply { a = 1f }.toFloatBits()
    }
}
