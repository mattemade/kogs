package net.mattemade.game

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.util.Scaler
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.concurrent.MessageChannel
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import kotlin.math.sin

class Game(
    context: Context,
    private val  externalMessages: MessageChannel,
) : ContextListener(context),
    Releasing by Self() {


    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = 100,
        height = 100,
        virtualWidth = 100f,
        virtualHeight = 100f
    )
    private val camera = viewport.camera.apply { position.set(50f, 50f, 0f) }
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch)

    private var time = 0f
    private var frame = 0

    override suspend fun Context.start() {

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        onRender { dt ->
            time += dt.seconds
            frame++
            externalMessages.write(frame)

            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.BLACK)

            viewport.apply(context)
            batch.begin(camera.viewProjection)

            shapeRenderer.filledRectangle(50f + sin(time) * 25f, 50f, 10f, 10f,)

            batch.end()
        }

        onDispose(::release)
    }

}
