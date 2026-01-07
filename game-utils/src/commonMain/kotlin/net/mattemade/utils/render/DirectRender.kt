package net.mattemade.utils.render

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import kotlin.time.Duration

class DirectRender(
    private val context: Context,
    width: Int,
    height: Int,
    private val updateCall: (dt: Duration, camera: Camera) -> Unit,
    private val renderCall: (dt: Duration, batch: Batch, shapeRenderer: ShapeRenderer) -> Unit,
    private val shapeRendererUpdate: (Batch) -> ShapeRenderer = { ShapeRenderer(it) },
    private val scaler: Scaler = Scaler.Fit(),
): Releasing by Self() {

    val viewport = ScalingViewport(scaler = scaler, width, height)
    val camera = viewport.camera
    val batch = SpriteBatch(context).releasing()
    var shapeRenderer = ShapeRenderer(batch)

    fun resize(width: Int, height: Int, worldWidth: Float = width.toFloat(), worldHeight: Float = height.toFloat()) {
        viewport.virtualWidth = worldWidth
        viewport.virtualHeight = worldHeight
        viewport.update(width, height, context)
    }

    fun updateWorldSize( worldWidth: Float, worldHeight: Float) {
        viewport.virtualWidth = worldWidth
        viewport.virtualHeight = worldHeight
        viewport.update(viewport.width, viewport.height, context)
    }

    fun render(dt: Duration) {
        updateCall(dt, camera)
        viewport.apply(context)
        camera.update()
        batch.begin(camera.viewProjection)
        renderCall(dt, batch, shapeRenderer)
        batch.end()
    }

    fun updateShapeRenderer() {
        shapeRenderer = shapeRendererUpdate(batch)
    }

}