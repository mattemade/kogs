package net.mattemade.utils.render

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.FrameBuffer
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.BlendFactor
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.gl.State
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import kotlin.time.Duration

class PixelRender(
    private val context: Context,
    targetWidth: Int,
    targetHeight: Int,
    virtualWidth: Float = targetWidth.toFloat(),
    virtualHeight: Float = targetHeight.toFloat(),
    private val preRenderCall: (dt: Duration, camera: Camera) -> Unit,
    private val renderCall: (dt: Duration, camera: Camera, batch: Batch, shapeRenderer: ShapeRenderer) -> Unit,
    private val clear: Boolean = false,
    private val blending: Boolean = false,
    private val allowFiltering: Boolean = false,
    private val shapeRendererUpdate: (Batch) -> ShapeRenderer = { ShapeRenderer(it) }
) : Releasing by Self() {

    private val batch = SpriteBatch(context).releasing()
    private var shapeRenderer = ShapeRenderer(batch)
    private val targetViewport = ScalingViewport(scaler = Scaler.Fit(), targetWidth, targetHeight, virtualWidth, virtualHeight)
    val targetCamera = targetViewport.camera
    private var defaultShader: ShaderProgram<*, *> = batch.shader
    private var shader: ShaderProgram<*, *> = defaultShader
    private lateinit var target: FrameBuffer
    lateinit var texture: Texture

    init {
        resize(targetWidth, targetHeight)
    }

    fun render(dt: Duration) {
        preRenderCall(dt, targetCamera)
        target.begin()
        if (clear) {
            context.gl.clearColor(Color.CLEAR)
            context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        }
        targetViewport.apply(context)
        batch.begin(targetCamera.viewProjection)
        if (blending) {
            context.gl.enable(State.BLEND)
            batch.setBlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)
        }
        if (shader != defaultShader) {
            batch.shader = shader
            shader.bind()
        }
        renderCall(dt, targetCamera, batch, shapeRenderer)
        if (shader != defaultShader) {
            batch.shader = defaultShader
            defaultShader.bind()
        }
        if (blending) {
            batch.setToPreviousBlendFunction()
            context.gl.disable(State.BLEND)
        }
        batch.end()
        target.end()
    }

    fun resize(width: Int, height: Int) {
        target = context.createPixelFrameBuffer(width, height, allowFiltering = allowFiltering)
        targetViewport.update(width, height, context, true)
        texture = target.textures[0]
    }

    fun setShader(shader: ShaderProgram<*, *>) {
        this.shader = shader
    }

    fun updateWorldSize(width: Float, height: Float) {
        targetViewport.virtualWidth = width
        targetViewport.virtualHeight = height
        targetViewport.update(targetViewport.width, targetViewport.height, context)
    }

    fun updateShapeRenderer() {
        shapeRenderer = shapeRendererUpdate(batch)
    }
}