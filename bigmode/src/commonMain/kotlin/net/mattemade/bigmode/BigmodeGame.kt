package net.mattemade.bigmode

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
import com.littlekt.util.seconds
import net.mattemade.bigmode.scene.CutsceneScene
import net.mattemade.bigmode.scene.RollerRestarauntScene
import net.mattemade.bigmode.scene.Scene
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import kotlin.time.Duration

class BigmodeGame(
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
    private var loadAssets: Boolean = false
    private var assetsReady: Boolean = false
    private var openWhenAssetsAreReady: Scene.Type? = null
    private val directRender =
        DirectRender(
            context,
            width = WORLD_WIDTH,
            height = WORLD_HEIGHT,
            ::update,
            ::render,
            shapeRendererUpdate = ::createdShapeRenderer)
    private val curtainsRender =
        DirectRender(
            context,
            width = WORLD_WIDTH,
            height = WORLD_HEIGHT,
            ::updateCurtains,
            ::renderCurtains,
            shapeRendererUpdate = ::createdShapeRenderer)

    private fun createdShapeRenderer(batch: Batch): ShapeRenderer = ShapeRenderer(batch, gameContext.assets.textureFiles.whitePixel)
    private val gameContext =
        BigmodeGameContext(context, log, encodeUrlComponent, getBlocking, overrideResourcesFrom, directRender.camera, ::openScene, ::sceneReady)
    private var scaleFactor: Float = 1f
    private val renderSize = MutableVec2f()
    private val offset = MutableVec2f()

    fun blur() {
        focused = false
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
    private lateinit var curtain: Texture
    private lateinit var logo: Texture
    private var logoHeight: Float = 0f
    private var logoWidth: Float  = 0f
    private var logoScale: Float  = 0.08f

    private fun openScene(type: Scene.Type) {
        if (curtainClosePosition == 0f) {
            gameContext.inputEnabled = false
            gameContext.scheduler.schedule().then(1f) {
                curtainClosePosition = it
            }.then {
                openScene(type)
                gameContext.scheduler.schedule().then(1f) {
                    logoPosition = it
                }
            }
            return
        }

        when(type) {
            is Scene.Type.Cutscene -> scene = CutsceneScene(gameContext, type.level)
            is Scene.Type.Restaurant -> {
                if (assetsReady) {
                    scene = RollerRestarauntScene(gameContext, gameContext.assets.resourceSheet.levelById[type.level + 1]!!)
                } else {
                    openWhenAssetsAreReady = type
                }
            }
        }
    }

    private fun sceneReady() {
        gameContext.scheduler.schedule().then(maxOf(0f, 2f - curtainsAreClosedFor)).then(1f) {
            logoPosition = 1f - it
        }.then(1f) {
            curtainClosePosition = 1f - it
        }.then {
            loadAssets = true
            gameContext.inputEnabled = true
        }
    }

    override suspend fun Context.start() {
        curtain = context.vfs["texture/certain.png"].readTexture(minFilter = TexMinFilter.LINEAR_MIPMAP_LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = true)
        logo = context.vfs["texture/LOGO_Skates_n_Plates.png"].readTexture(minFilter = TexMinFilter.LINEAR_MIPMAP_LINEAR, magFilter = TexMagFilter.LINEAR, mipmaps = true)
        logoHeight = logo.height * logoScale
        logoWidth = logo.width * logoScale
        input.addInputProcessor(object : InputProcessor {
            private var usingMouse = false

            override fun keyDown(key: Key): Boolean {
                if (!focused) {
                    focused = true
                }
                when (key) {
                    /*Key.SHIFT_LEFT,
                    Key.SHIFT_RIGHT -> {
                        gameContext.rocketPressed = true
                        }*/
                    Key.SPACE -> gameContext.brakePressed = true

                    else -> {}
                }
                return false
            }

            override fun keyUp(key: Key): Boolean {
                when (key) {
                    /*Key.SHIFT_LEFT,
                    Key.SHIFT_RIGHT -> {
                        gameContext.rocketPressed = false
                    }*/
                    Key.SPACE -> gameContext.brakePressed = false

                    else -> {}
                }
                return false
            }

            private val touchingIndicesStack = mutableListOf<Int>()

            override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                if (!focused) {
                    focused = true
                }
                touchingIndicesStack.remove(pointer.index)
                if (!usingMouse && touchingIndicesStack.size == 1) {
                    gameContext.mousePressed = false
                }
                return false
            }

            override fun mouseMoved(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float
            ): Boolean {
                usingMouse = true
                gameContext.usingKeyPosition = false
                updateMouseCursorPosition(screenX, screenY)
                return false
            }

            override fun touchDown(
                screenX: Float,
                screenY: Float,
                pointer: Pointer
            ): Boolean {
                touchingIndicesStack += pointer.index
                val size = touchingIndicesStack.size
                if (size == 2) {
                    gameContext.buttonWasPressed = true
                    gameContext.mousePressed = true
                    gameContext.usingKeyPosition = false
                } else if (size == 1) {
                    updateMouseCursorPosition(screenX, screenY)
                }
                return false
            }

            override fun touchDragged(
                screenX: Float,
                screenY: Float,
                movementX: Float,
                movementY: Float,
                pointer: Pointer
            ): Boolean {
                if (usingMouse || touchingIndicesStack.first() == pointer.index) {
                    updateMouseCursorPosition(screenX, screenY)
                }
                return false
            }

            private fun updateMouseCursorPosition(screenX: Float, screenY: Float) {
                gameContext.cursorPositionInWorldCoordinates.set(screenX, screenY).scale(1f / gameContext.canvasZoom)
                    .subtract(offset)
                    .scale(WORLD_WIDTH_FLOAT / renderSize.x, WORLD_HEIGHT_FLOAT / renderSize.y)
            }

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                if (amountY < 0f) {
                    RollerRestarauntScene.MAGIC_NUMBER += 1f
                } else if (amountY > 0f) {
                    RollerRestarauntScene.MAGIC_NUMBER = maxOf(1f, RollerRestarauntScene.MAGIC_NUMBER - 1f)
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
            scaleFactor = minOf(width / WORLD_WIDTH_FLOAT, height / WORLD_HEIGHT_FLOAT)
            renderSize.set(WORLD_WIDTH_FLOAT * scaleFactor, WORLD_HEIGHT_FLOAT * scaleFactor)
            offset.set(width.toFloat(), height.toFloat()).subtract(renderSize).scale(0.5f)

            directRender.resize(width, height, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
            curtainsRender.resize(width, height, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
        }

        onRender { dt ->
            //gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            //gl.clearColor(Color.CLEAR)

            if (!audioReady) {
                audioReady = audio.isReady()
            }
            if (loadAssets && !assetsReady) {
                assetsReady = /*audioReady &&*/ gameContext.assets.isLoaded
                if (assetsReady) {
                    updateParameters()
                    directRender.updateShapeRenderer()
                    openWhenAssetsAreReady?.let {
                        openScene(it)
                        openWhenAssetsAreReady = null
                    }
                }
            }


            directRender.render(dt)
            curtainsRender.render(dt)
        }

        onDispose(::release)

        openScene(Scene.Type.Cutscene(level = 0))
    }

    private fun updateParameters() {
        with(gameContext.assets.resourceSheet.parametersByName) {
            get("Start from level")?.value?.toInt()?.takeIf { it > 1 }?.let {
                openScene(Scene.Type.Cutscene(level = it -1))
            }
            get("Min restaurant width factor")?.value?.let { MIN_RESTAURANT_WIDTH_RATIO = it }
            get("Max restaurant width factor")?.value?.let { RESTAURANT_WIDTH_RATIO = it }
            get("Music volume")?.value?.let { MUSIC_VOLUME = it }
        }
    }

    private fun update(duration: Duration, camera: Camera) {
        if (focused) {
            val dt = duration.seconds
            gameContext.update(dt)
            scene.update(dt)
        }
    }

    private fun render(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        scene.render(batch, shapeRenderer)
    }

    private var curtainClosePosition = 1f
    private var logoPosition = 1f
    private var curtainsAreClosedFor = 0f

    private fun updateCurtains(duration: Duration, camera: Camera) {
        if (curtainClosePosition == 1f) {
            curtainsAreClosedFor += duration.seconds
        } else {
            curtainsAreClosedFor = 0f
        }
        camera.position.set(HALF_WORLD_WIDTH_FLOAT, HALF_WORLD_HEIGHT_FLOAT, 0f)
    }

    private fun renderCurtains(duration: Duration, batch: Batch, shapeRenderer: ShapeRenderer) {
        if (curtainClosePosition > 0f) {
            fadeColor.a = maxOf(0f, curtainClosePosition * 2f - 1f)
            shapeRenderer.filledRectangle(
                x = -1f,
                y = -1f,
                width = WORLD_WIDTH_FLOAT + 2f,
                height = WORLD_HEIGHT_FLOAT + 2f,
                color = fadeColor.toFloatBits()
            )
            batch.draw(
                curtain,
                x = (curtainClosePosition - 1f) * CURTAINS_WIDTH,
                y = 0f,
                width = CURTAINS_WIDTH,
                height = CURTAINS_HEIGHT
            )
            batch.draw(
                curtain,
                x = RIGHT_CURTAIN_START - (curtainClosePosition - 1f) * CURTAINS_WIDTH,
                y = 0f,
                width = CURTAINS_WIDTH,
                height = CURTAINS_HEIGHT,
                flipX = true
            )
        }
        if (!focused) {
            shapeRenderer.filledRectangle(
                x = -1f,
                y = -1f,
                width = WORLD_WIDTH_FLOAT + 2f,
                height = WORLD_HEIGHT_FLOAT + 2f,
                color = pauseColor
            )
        }
        if (logoPosition > 0f || !focused) {
            logoColor.a = if (!focused) 1f else logoPosition
            batch.draw(
                logo,
                x = 0f,
                y = 0f,
                width = logoWidth,
                height = logoHeight,
                colorBits = logoColor.toFloatBits()
            )
        }
    }

    fun onCanvasZoomChanged(zoom: Float) {
        gameContext.canvasZoom = zoom
    }

    companion object {
        const val TITLE = "Bigmode 2026"
        private val pauseColor = Color.BLACK.toMutableColor().apply { a = 0.5f }.toFloatBits()
        private val logoColor = Color.WHITE.toMutableColor()
        private val fadeColor = Color.BLACK.toMutableColor()
    }
}
