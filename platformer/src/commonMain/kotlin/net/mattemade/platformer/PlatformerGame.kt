package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec2i
import com.littlekt.util.seconds
import korlibs.time.TimeSpan
import korlibs.time.blockingSleep
import net.mattemade.platformer.input.ControllerInput
import net.mattemade.platformer.input.TouchButton
import net.mattemade.platformer.scene.PlatformingScene
import net.mattemade.platformer.scene.Scene
import net.mattemade.platformer.scene.SplashScene
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import net.mattemade.utils.render.PixelRender
import net.mattemade.utils.util.FpsCounter
import kotlin.math.sin
import kotlin.math.sqrt
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
    private val fmodFolderPrefix: String,
    private val fmodLiveUpdate: Boolean,
    private val inkStoryFactory: net.mattemade.platformer.ink.InkStoryFactory,
) : ContextListener(context),
    Releasing by Self() {

    var focused = true
        set(value) {
            if (!field && value) {
                // TODO: do something for FMOD?
               // context.audio.resume()
            } else if (field && !value) {
               // context.audio.suspend()
            }
            field = value
        }
    private var error: Boolean = false

    private var audioReady: Boolean = false
    private var assetsReady: Boolean = false
    private var fmodAssetsReady: Boolean = false
    private var gameStarted: Boolean = false
    private val gameContext =
        PlatformerGameContext(context, log, encodeUrlComponent, getBlocking, overrideResourcesFrom, fmodFolderPrefix, fmodLiveUpdate, ::restartScene, ::startGame, inkStoryFactory)
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
    private val gameSize = MutableVec2f(WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
    private val gameOffset = MutableVec2f(0f, 0f)
    private var landscape: Boolean = true
    private var inverseScale: Float = 1f
    private val activePointers = Array(20) { false }
    private val touchPointers = Array(20) { MutableVec2f() }
    private val touchButtons = mutableListOf<TouchButton>()

    private val fpsCounter = FpsCounter()


    fun blur() {
        //focused = false
        gameContext.paused = true
        gameContext.log("blur")
    }

    fun focus() {
        //focused = true
        gameContext.log("focus")
    }

    fun destroy() {
        gameContext.log("exit")
    }

    fun pointerLockReleased() {
        context.releaseCursor()
    }

    fun error(line: String) {
        gameContext.log("error|$line")
        error = true
    }

    private val platformingScene: PlatformingScene by lazy { PlatformingScene(gameContext) }
    private var scene: Scene? = null
        set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                oldValue?.release()
            }
        }

    private fun startGame() {
        gameContext.log("click")
        gameStarted = true
        scene = platformingScene
    }

    private fun restartScene() {
        (scene as? PlatformingScene)?.reset()
//        scene?.release()
//        scene = PlatformingScene(gameContext)
    }

    private lateinit var loadingTexture: Texture
    private lateinit var errorTexture: Texture
    private lateinit var fmodTexture: Texture

    override suspend fun Context.start() {
        gameContext.log("start")

        loadingTexture = context.vfs["texture/loading.png"].readTexture(mipmaps = false)
        errorTexture = context.vfs["texture/error.png"].readTexture(mipmaps = false)
        fmodTexture = context.vfs["texture/FMOD Logo White - Transparent Background.png"].readTexture(minFilter = TexMinFilter.LINEAR_MIPMAP_LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = true)

        input.catchKeys.add(Key.TAB)
        input.addActiveInputProcessor(object : InputProcessor {
            override fun keyDown(key: Key): Boolean {
                if (!focused) {
                    focused = true
                }
                return false
            }

            override fun mouseMoved(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float
            ): Boolean {
                if (movementX != 0f && movementY != 0f) {
                    gameContext.gameInput.mouseDetected = true
                }
                return false
            }

            override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                if (!focused) {
                    focused = true
                }
                if (!audioReady && assetsReady) {
                    audioReady = true
                    gameContext.assets.fmod.system.mixerSuspend()
                    gameContext.assets.fmod.system.mixerResume()
                }
                updatePointer(pointer, screenX, screenY, false)
                return false
            }

            override fun touchDown(
                screenX: Float,
                screenY: Float,
                pointer: Pointer
            ): Boolean {
                gameContext.gameInput.touchInput = !gameContext.gameInput.mouseDetected
                updatePointer(pointer, screenX, screenY, true)
                return false
            }

            override fun touchDragged(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float,
                pointer: Pointer
            ): Boolean {
                updatePointer(pointer, screenX, screenY, true)
                return false
            }

            private fun updatePointer(pointer: Pointer, screenX: Float, screenY: Float, isActive: Boolean) {
                if (gameContext.gameInput.touchInput) {
                    touchPointers[pointer.index].set(screenX, screenY).scale(inverseScale * gameContext.canvasInverseZoom)
                    activePointers[pointer.index] = isActive
                }
            }
        })

        onResize { width, height ->
            //focused = false

            if (width == 0 || height == 0) {
                // collapsed the mobile session
                gameContext.paused = true
                return@onResize
            }

            // resizing to a higher resolution than was before -> maybe going fullscreen, maybe just zoom changed
            if (width > directRender.viewport.virtualWidth || height > directRender.viewport.virtualHeight) {

            }
            screenSize.x = width
            screenSize.y = height
            resizeFinalRender(width, height)
            gameContext.storyDisplayService.onResize(width, height)
        }

        onQuickUpdate {
            if (assetsReady) {
                gameContext.assets.fmod.studioSystem.update()
                if (!fmodAssetsReady) {
                    fmodAssetsReady = gameContext.fmodAssets.isLoaded
                    if (fmodAssetsReady) {
                        // parameters could be refreshed at that point, need to resize the viewport to match them
                        resizeFinalRender(screenSize.x, screenSize.y)
                        pixelRender.resize(WORLD_WIDTH, WORLD_HEIGHT)
                        pixelRender.targetCamera.position.set(WORLD_WIDTH_FLOAT * 0.5f, WORLD_HEIGHT_FLOAT * 0.5f, 0f)

                        directRender.updateShapeRenderer()
                        pixelRender.updateShapeRenderer()

                        gameContext.log("loaded")
                        gameContext.load()
                        scene = SplashScene(gameContext)
                        platformingScene // just to initialize it
                    }
                }
            }
        }

        onRender { dt ->
            //gl.clearColor(Color.BLACK)
            //gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
//            fpsCounter.update(dt.seconds)

            if (!assetsReady) {
                assetsReady = gameContext.assets.isLoaded
            }

            //if (focused && assetsReady && fmodAssetsReady) {
                gameContext.update(dt)
                pixelRender.render(dt)
                directRender.render(dt)
                // why is it indented? uhm, whatever.
                gameContext.storyDisplayService.render(dt.seconds)
            //}

            // unpause in the end of onRender, to let the previous update with huuuuuge dt to not affect the game
            gameContext.paused = false

            //blockingSleep(TimeSpan(1000.0 / 15))
        }

        onDispose(::release)
    }

    private fun resizeFinalRender(width: Int, height: Int) {
        val screenAspect = width.toFloat() / height.toFloat()
        val worldAspect = WORLD_WIDTH_FLOAT / WORLD_HEIGHT_FLOAT
        landscape = screenAspect > worldAspect

        val fullWidth: Float
        val fullHeight: Float
        if (landscape) {
            fullWidth = WORLD_HEIGHT_FLOAT * screenAspect
            fullHeight = WORLD_HEIGHT_FLOAT
            gameOffset.set(fullWidth - WORLD_WIDTH_FLOAT, fullHeight - WORLD_HEIGHT_FLOAT).scale(0.5f)
        } else {
            fullWidth = WORLD_WIDTH_FLOAT
            fullHeight = WORLD_WIDTH_FLOAT / screenAspect
            gameOffset.set(fullWidth - WORLD_WIDTH_FLOAT, 0f).scale(0.5f)
        }

        gameSize.set(fullWidth, fullHeight)
        inverseScale = minOf(fullWidth / width, fullHeight / height)
        directRender.resize(width, height, fullWidth, fullHeight)
        directRender.camera.position.set(fullWidth * 0.5f, fullHeight * 0.5f, 0f)
        layoutButtons()
    }

    private fun layoutButtons() {
        touchButtons.clear()
        val actionButtonRadius: Float = 50f
        val actionButtonMargin: Float = 35f
        val actionButtonDistance: Float = actionButtonRadius + actionButtonMargin
        val stickRadius: Float = 80f
        val stickX: Float =  20f + stickRadius
        val stickY: Float = gameSize.y - stickRadius * 1.2f

        val buttonsX = gameSize.x - actionButtonRadius - actionButtonDistance * 0.5f
        val buttonsY = gameSize.y - actionButtonRadius
        val buttonsTopY = buttonsY - sqrt(3f) * actionButtonDistance * 0.5f // height of equilateral triangle

        val inverseAdjustedStickRadius = 1f / (stickRadius - 10f)
        touchButtons += TouchButton(stickX, stickY, stickRadius, isDpad = true, color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits(), activeColor = Color.WHITE.toFloatBits()) {
            if (it.isDpad) {
                if (it.isActive) {
                    gameContext.gameInput.movement.set(it.touchDirection).scale(inverseAdjustedStickRadius)
                } else {
                    gameContext.gameInput.movement.set(0f, 0f)
                }
            }
        }

        touchButtons += TouchButton(buttonsX + actionButtonDistance * 0.5f, buttonsY, actionButtonRadius, color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits(), activeColor = Color.WHITE.toFloatBits()) {
            gameContext.gameInput.touchButtonStates[ControllerInput.JUMP.ordinal] = it.isActive
        }
        touchButtons += TouchButton(buttonsX - actionButtonDistance * 0.5f, buttonsY, actionButtonRadius, color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits(), activeColor = Color.WHITE.toFloatBits()) {
            gameContext.gameInput.touchButtonStates[ControllerInput.ATTACK.ordinal] = it.isActive
        }
        touchButtons += TouchButton(buttonsX, buttonsTopY, actionButtonRadius, color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits(), activeColor = Color.WHITE.toFloatBits()) {
            gameContext.gameInput.touchButtonStates[ControllerInput.DASH.ordinal] = it.isActive
        }
        touchButtons += TouchButton(buttonsX, actionButtonDistance, actionButtonRadius, color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits(), activeColor = Color.WHITE.toFloatBits()) {
            gameContext.gameInput.touchButtonStates[ControllerInput.MAP.ordinal] = it.isActive
        }
    }


    private fun update(duration: Duration, camera: Camera) {
        scene?.update(duration.seconds)
    }

    private fun render(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        scene?.render(batch, shapeRenderer)
    }

    private var time: Float = 0f

    private fun finalUpdate(duration: Duration, camera: Camera) {
        time += duration.seconds
        if (gameContext.gameInput.touchInput) {
            touchButtons.forEach { button ->
                if (button.isDpad && button.trackingPointer >= 0) {
                    if (button.intersects(touchPointers[button.trackingPointer], button.trackingPointer)) {
                        button.update(activePointers[button.trackingPointer])
                    }
                } else {
                    for (i in 0..19) {
                        if (activePointers[i]) {
                            if (button.intersects(touchPointers[i], i)) {
                                button.update(active = true)
                                return@forEach
                            }
                        }
                    }
                    button.update(active = false)
                }
            }

        }
    }

    private fun finalRender(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        context.gl.clearColor(Color.BLACK)
        context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        batch.shader = gameContext.assets.shaders.postShader
        batch.draw(
            pixelRender.texture,
            x = gameOffset.x,
            y = gameOffset.y,
            width = WORLD_WIDTH_FLOAT,
            height = WORLD_HEIGHT_FLOAT,
            flipY = true
        )

        if (!gameStarted) {
            batch.draw(fmodTexture, x = gameOffset.x + 100f, y = 400f, scaleX = 0.25f, scaleY = 0.25f)
        }

        if (!assetsReady || !fmodAssetsReady) {
            val scale = 1f + sin(time) * 0.05f
            batch.draw(loadingTexture, x = gameOffset.x, y = 0f, scaleX = scale, scaleY = scale)
        }

        if (error) {
            batch.draw(errorTexture, x = gameOffset.x, y = 0f)
        }

        if (gameContext.gameInput.touchInput) {
            batch.useDefaultShader()
            touchButtons.forEach { button ->
                if (button.isDpad) {
                    shapeRenderer.filledCircle(
                        center = button.center,
                        radius = button.radius - 10f,
                        color = button.color,
                    )
                    if (button.isActive) {
                        shapeRenderer.filledCircle(
                            x = button.center.x + button.touchDirection.x,
                            y = button.center.y + button.touchDirection.y,
                            radius = 20f,
                            color = button.activeColor,
                        )
                    }
                } else {
                    shapeRenderer.filledCircle(
                        center = button.center,
                        radius = button.radius,
                        color = if (button.isActive) button.activeColor else button.color,
                    )
                }
            }
        }
    }


    fun onCanvasZoomChanged(zoom: Float) {
        gameContext.canvasZoom = zoom
        gameContext.canvasInverseZoom = 1f / zoom
    }

    companion object {
        const val TITLE = "Magical Girl Metroidvania"
    }
}
