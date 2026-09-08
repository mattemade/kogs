package net.mattemade.gfxtest

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import com.littlekt.graphics.toFloatBits
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.gfxtest.shader.MsdfEffectShader
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import kotlin.time.Duration

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

        MsdfEffectShader.prepare(context)
        val msdfShader = MsdfEffectShader.program

        val denpa = vfs["texture/denpawarmup.png"].readTexture(minFilter = TexMinFilter.LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = false)
        val denpaHeight = 900f
        val denpaWidth = 900f / denpa.height * denpa.width
        val denpaXPos = 1600f - denpaWidth + 400f

        val denpaMsdf = vfs["texture/denpawarmup-msdf.png"].readTexture(minFilter = TexMinFilter.LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = false)
        var denpaMsdfScale = 2.25f
        val denpaMsdfWidth = denpaMsdf.width * denpaMsdfScale
        val denpaMsdfHeight = denpaMsdf.height * denpaMsdfScale

        fun preRender(dt: Duration, camera: Camera) {
            camera.position.set(800f, 450f, 0f)
        }

        fun renderCall(dt: Duration, camera: Camera, batch: Batch, shapeRenderer: ShapeRenderer) {
            shapeRenderer.circle(x = 800f, y = 450f, radius = 200f)
            batch.draw(denpa, x = denpaXPos, y = 0f, width = denpaWidth, height = denpaHeight)

            shapeRenderer.filledRectangle(x = 0f, y = 0f, width = denpaMsdfWidth, height = 900f, color = Color.WHITE.toFloatBits())

            val oldShader = batch.shader
            batch.shader = msdfShader
            batch.draw(denpaMsdf, x = 0f, y = 100f, width = denpaMsdfWidth, height =  denpaMsdfHeight, colorBits = Color.BLACK.toFloatBits())
            batch.shader = oldShader
        }

        val pixelRender = PixelRender(context, targetWidth = 1920, targetHeight = 1080, virtualWidth = 1600f, virtualHeight = 900f, preRenderCall = ::preRender, renderCall = ::renderCall)


        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        onRender { dt ->
            pixelRender.render(dt)

            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.GRAY)

            viewport.apply(context)
            batch.begin(camera.viewProjection)
            batch.draw(pixelRender.texture, x = 0f, y = 0f, width = 1600f, height = 900f, flipY = true)
            batch.end()
        }

        onDispose(::release)
    }

}
