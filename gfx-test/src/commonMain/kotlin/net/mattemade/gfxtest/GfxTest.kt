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
import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec3f
import com.littlekt.math.geom.radians
import com.littlekt.util.Scaler
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.gfxtest.shader.CrtVhsShader
import net.mattemade.gfxtest.shader.MsdfBevelEffectShader
import net.mattemade.gfxtest.shader.MsdfEffectShader
import net.mattemade.utils.msdf.MsdfFont
import net.mattemade.utils.msdf.MsdfFontRenderer
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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

    private var time = 0f
    private var tempLightDirection = MutableVec2f(0f, 0f)
    private var lightDirection = MutableVec3f(0f, 0f, 0f)
    private var screenSize = MutableVec2f(0f, 0f)

    private fun updateLightDirection(screenX: Float, screenY: Float) {
        lightDirection.set(
            (screenX - screenSize.x * 0.5f) * 1600f / screenSize.x,
            (screenY - screenSize.y * 0.5f) * 900f / screenSize.y,
            0f
        )
        lightDirection.z = screenSize.y * 0.5f - abs(lightDirection.length())
    }

    override suspend fun Context.start() {

        input.addActiveInputProcessor(object: InputProcessor {
            override fun mouseMoved(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float
            ): Boolean {
                updateLightDirection(screenX, screenY)
                return super.mouseMoved(screenX, screenY, movementX, movementY)
            }

            override fun touchDown(
                screenX: Float,
                screenY: Float,
                pointer: Pointer
            ): Boolean {
                updateLightDirection(screenX, screenY)
                return super.touchDown(screenX, screenY, pointer)
            }

            override fun touchDragged(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float,
                pointer: Pointer
            ): Boolean {
                updateLightDirection(screenX, screenY)
                return super.touchDragged(screenX, screenY, movementX, movementY, pointer)
            }

        })

        val jbMonoFontTexture = vfs["font/jbmono.png"].readTexture(minFilter = TexMinFilter.LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = false)
        val jbMonoFontSpec = vfs["font/jbmono.csv"].readLines()
        val jbMonoFont = MsdfFont(jbMonoFontTexture, lineHeight = 1.32f, descender = 0.299999f, csvSpecs = jbMonoFontSpec)

        MsdfEffectShader.prepare(context)
        val msdfShader = MsdfEffectShader.program

        MsdfBevelEffectShader.prepare(context)
        val bevelShader = MsdfBevelEffectShader.program

        CrtVhsShader.prepare(context)
        val crtVhsShader = CrtVhsShader.program

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
//            batch.shader = msdfShader
//            msdfShader.fragmentShader.uTime.apply(msdfShader, time * 0.001f)
            batch.shader = bevelShader
            bevelShader.fragmentShader.uTime.apply(bevelShader, sin(time * 1f) * 0.01f)
            bevelShader.fragmentShader.uLightPos.apply(bevelShader, lightDirection.x, lightDirection.y, lightDirection.z)
            bevelShader.fragmentShader.uBaseColor.apply(bevelShader, 0.6f, 0.6f, 0f)
//            bevelShader.fragmentShader.uOutlineStyle.apply(bevelShader, 0f, 0f, 0f, 0f)
            batch.draw(denpaMsdf, x = 0f, y = 100f, width = denpaMsdfWidth, height =  denpaMsdfHeight, colorBits = Color.BLACK.toFloatBits())
            jbMonoFont.draw("Hello world!", x = 0f, y = 0f, scale = 100f, batch)
            batch.shader = oldShader
        }

        val pixelRender = PixelRender(context, targetWidth = 1920, targetHeight = 1080, virtualWidth = 1600f, virtualHeight = 900f, preRenderCall = ::preRender, renderCall = ::renderCall)

        var skipTimeIncrement = false

        onResize { width, height ->
            viewport.update(width, height, context, false)
            screenSize.set(width.toFloat(), height.toFloat())
            time = 0f
            skipTimeIncrement = true
        }

        onRender { dt ->
            if (skipTimeIncrement) {
                skipTimeIncrement = false
            } else {
                time += dt.seconds
            }

            tempLightDirection.set(screenSize.x * 0.5f, 0f).rotate(time.radians).add(screenSize.x * 0.5f, screenSize.y * 0.5f)
            updateLightDirection(tempLightDirection.x, tempLightDirection.y)

            pixelRender.render(dt)

            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.GRAY)

            viewport.apply(context)
            batch.begin(camera.viewProjection)

            val oldShader = batch.shader
            batch.shader = crtVhsShader
            crtVhsShader.fragmentShader.uTime.apply(crtVhsShader, time)
//            crtVhsShader.fragmentShader.uStrength.apply(crtVhsShader, 10f)
            crtVhsShader.fragmentShader.uStrength.apply(crtVhsShader, sin(time * 0.125f) * 10f)
            crtVhsShader.fragmentShader.uResolution.apply(crtVhsShader, 1920f, 1080f)
            batch.draw(pixelRender.texture, x = 0f, y = 0f, width = 1600f, height = 900f, flipY = true)


            batch.shader = bevelShader
            bevelShader.fragmentShader.uTime.apply(bevelShader, sin(time * 0.5f) * 0.005f)
//            bevelShader.fragmentShader.uTime.apply(bevelShader, sin(time * 0.5f) * 0.005f)
            bevelShader.fragmentShader.uLightPos.apply(bevelShader, lightDirection.x, lightDirection.y, lightDirection.z)
            bevelShader.fragmentShader.uBaseColor.apply(bevelShader, 0f, 1f, 1f)
            bevelShader.fragmentShader.uOutlineStyle.apply(bevelShader, 1f, 1f, 1f, 0.2f)
            bevelShader.fragmentShader.uBevelStyle.apply(bevelShader, 0.6f, 0.5f)
            jbMonoFont.draw("text on top\nof CRT", x = 40f, y = 400f, scale = 200f, batch)

            batch.shader = oldShader


            batch.end()
        }

        onDispose(::release)
    }

}
