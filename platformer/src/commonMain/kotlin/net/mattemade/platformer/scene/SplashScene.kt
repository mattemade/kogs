package net.mattemade.platformer.scene

import com.github.quillraven.fleks.Entity
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.util.Scaler
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.platformer.FIRST_LEVEL_NAME
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.StaminaDamageComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.world.Room
import net.mattemade.utils.msdf.MsdfFontRenderer
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import net.mattemade.utils.tiled.BoundsListener
import org.jbox2d.common.Vec2
import kotlin.math.roundToInt

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

    override fun update(seconds: Float) {
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
            draw(
                """Click or touch to start""".trimIndent(), 1f, 1f, 1f, batch
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
        private val tempVec2f = MutableVec2f()
        private val mapColor = Color.WHITE.toMutableColor().apply { a = 1f }.toFloatBits()
    }
}
