package net.mattemade.gfxtest

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self

class GfxTest(
    context: Context,
) : ContextListener(context),
    Releasing by Self() {


    private val viewport = ScalingViewport(
        scaler = Scaler.Fit(),
        width = 1920,
        height = 1080,
        virtualWidth = 1600f,
        virtualHeight = 900f
    )
    private val camera = viewport.camera.apply { position.set(800f, 450f, 0f) }
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch)


    override suspend fun Context.start() {

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.GRAY)

            viewport.apply(context)
            batch.begin(camera.viewProjection)

            shapeRenderer.circle(x = 800f, y = 450f, radius = 200f)

            batch.end()
        }

        onDispose(::release)
    }

}
