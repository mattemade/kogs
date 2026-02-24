package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2i
import com.littlekt.util.seconds
import korlibs.time.TimeSpan
import korlibs.time.blockingSleep
import net.mattemade.fmod.systemCreate
import net.mattemade.fmod.systemRelease
import net.mattemade.platformer.scene.PlatformingScene
import net.mattemade.platformer.scene.Scene
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import net.mattemade.utils.render.PixelRender
import net.mattemade.utils.util.FpsCounter
import kotlin.time.Duration

class PlatformerGame(
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
                systemRelease(systemCreate())

                context.audio.resume()
            } else if (field && !value) {
                context.audio.suspend()
            }
            field = value
        }
    private var audioReady: Boolean = false
    private var assetsReady: Boolean = false
    private val gameContext =
        PlatformerGameContext(context, log, encodeUrlComponent, getBlocking, overrideResourcesFrom)
    private val pixelRender =
        PixelRender(
            context,
            targetWidth = 1, // it will be resized from parameters
            targetHeight = 1,
            preRenderCall = ::update,
            blending = true,
            renderCall = { duration: Duration, camera: Camera, batch: Batch, renderer: ShapeRenderer -> },
            shapeRendererUpdate = { ShapeRenderer(it, gameContext.assets.textureFiles.whitePixel) })

    private val directRender =
        DirectRender(
            context,
            width = WORLD_WIDTH,
            height = WORLD_HEIGHT,
            ::finalUpdate,
            ::finalRender,
            shapeRendererUpdate = { ShapeRenderer(it, gameContext.assets.textureFiles.whitePixel) })

    private val screenSize = MutableVec2i(WORLD_WIDTH, WORLD_HEIGHT)

    private val fpsCounter = FpsCounter()


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

    private var scene: Scene? = null
        set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                oldValue?.release()
            }
        }

    override suspend fun Context.start() {
        gameContext.log("start")
        input.catchKeys.add(Key.TAB)
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
            screenSize.x = width
            screenSize.y = height
            directRender.resize(width, height, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
            directRender.camera.position.set(WORLD_WIDTH_FLOAT * 0.5f, WORLD_HEIGHT_FLOAT * 0.5f, 0f)
        }

        onRender { dt ->
            //gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            //gl.clearColor(Color.BLACK)
            //fpsCounter.update(dt.seconds)

            if (!audioReady) {
                audioReady = audio.isReady()
            }
            if (!assetsReady) {
                assetsReady = audioReady && gameContext.assets.isLoaded
                if (assetsReady) {
                    // parameters could be refreshed at that point, need to resize the viewport to match them
                    directRender.resize(screenSize.x, screenSize.y, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
                    directRender.camera.position.set(WORLD_WIDTH_FLOAT * 0.5f, WORLD_HEIGHT_FLOAT * 0.5f, 0f)
                    pixelRender.resize(WORLD_WIDTH, WORLD_HEIGHT)

                    directRender.updateShapeRenderer()
                    pixelRender.updateShapeRenderer()
                    scene = PlatformingScene(gameContext)
                }
            }

            if (focused && assetsReady) {
                pixelRender.render(dt)
                directRender.render(dt)
            }

            //blockingSleep(TimeSpan(2.0))
        }

        onDispose(::release)
    }


    private fun update(duration: Duration, camera: Camera) {
        scene?.update(duration.seconds)
    }

    private fun render(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {

    }

    private fun finalUpdate(duration: Duration, camera: Camera) {

    }

    private fun finalRender(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            pixelRender.texture,
            x = 0f,
            y = 0f,
            width = WORLD_WIDTH_FLOAT,
            height = WORLD_HEIGHT_FLOAT,
            flipY = true
        )
    }


    fun onCanvasZoomChanged(zoom: Float) {
        gameContext.canvasZoom = zoom
    }

    companion object {
        const val TITLE = "littlekt-lg-ex game template"
    }
}
