package net.mattemade.gametemplate

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.util.seconds
import net.mattemade.gametemplate.scene.Scene
import net.mattemade.gametemplate.scene.TemplateScene
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import kotlin.time.Duration

class TemplateGame(
    context: Context,
    private val zoomCanvasIn: () -> Unit,
    private val log: (String) -> Unit,
    private val encodeUrlComponent: (String) -> String,
    private val getRequest: (url: String, callback: (List<String>?) -> Unit) -> Unit,
    private val getBlocking: (url: String) -> List<String>?,
    private val postRequest: (url: String, body: String, callback: (List<String>?) -> Unit) -> Unit,
    private val connect: (url: String, callback: (SocketMessage) -> Unit) -> SocketConnection,
    private val overrideResourcesFrom: String? = null,
) : ContextListener(context),
    Releasing by Self() {

    var focused = true
        set(value) {
            if (!field && value) {
                context.audio.resume()
            } else if (field && !value) {
                context.audio.suspend()
            }
            field = value
        }
    private var audioReady: Boolean = false
    private var assetsReady: Boolean = false
    private val gameContext =
        TemplateGameContext(context, log, encodeUrlComponent, getBlocking, overrideResourcesFrom)
    private val directRender =
        DirectRender(
            context,
            width = WORLD_WIDTH,
            height = WORLD_HEIGHT,
            ::update,
            ::render,
            shapeRendererUpdate = { ShapeRenderer(it, gameContext.assets.textureFiles.whitePixel) })


    fun blur() {
        //focused = false
        gameContext.log("blur")
    }

    fun focus() {
        focused = true
        gameContext.log("focus")
    }

    fun destroy() {
        gameContext.log("exit")
    }

    fun pointerLockReleased() {
        context.releaseCursor()
    }

    private lateinit var scene: Scene

    override suspend fun Context.start() {
        input.addInputProcessor(object : InputProcessor {
            override fun keyDown(key: Key): Boolean {
                if (!focused) {
                    focused = true
                }
                return false
            }

            override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                if (!focused) {
                    focused = true
                }
                return false
            }
        })

        onResize { width, height ->
            //focused = false

            if (width == 0 || height == 0) {
                // collapsed the mobile session
                return@onResize
            }

            // resizing to a higher resolution than was before -> maybe going fullscreen, maybe just zoom changed
            if (width > directRender.viewport.virtualWidth || height > directRender.viewport.virtualHeight) {

            }
            directRender.resize(width, height, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
        }

        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.CLEAR)

            if (!audioReady) {
                audioReady = audio.isReady()
            }
            if (!assetsReady) {
                assetsReady = audioReady && gameContext.assets.isLoaded
                if (assetsReady) {
                    directRender.updateShapeRenderer()
                    scene = TemplateScene(gameContext)
                }
            }

            if (focused && assetsReady) {
                directRender.render(dt)
            }
        }

        onDispose(::release)
    }


    private fun update(duration: Duration, camera: Camera) {
        scene.update(duration.seconds)
    }

    private fun render(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        scene.render(batch, shapeRenderer)
    }

    fun onCanvasZoomChanged(zoom: Float) {
        gameContext.canvasZoom = zoom
    }

    companion object {
        const val TITLE = "littlekt-lg-ex game template"
    }
}
