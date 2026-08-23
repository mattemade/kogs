package net.mattemade.latencytest

import com.github.quillraven.fleks.configureWorld
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.util.Scaler
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.concurrent.MessageChannel
import net.mattemade.latencytest.asset.Assets
import net.mattemade.latencytest.asset.FmodAssets
import net.mattemade.latencytest.ecs.ControllableComponent
import net.mattemade.latencytest.ecs.ControlsSystem
import net.mattemade.latencytest.ecs.FakePhysicsSystem
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import kotlin.math.sin

class Game(
    context: Context,
    fmodFolderPrefix: String,
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
    private val assets = Assets(context, fmodFolderPrefix)
    private val fmodAssets by lazy { FmodAssets(context, fmodFolderPrefix, assets.fmod) }
    private var assetsLoaded = false
    private var fmodAssetsLoaded = false


    private var pressed = false
    private val ecs = configureWorld {
        injectables {
            add(context)
        }
        systems {
            add(ControlsSystem())
//            add(FakePhysicsSystem())
        }
    }
    private val entity = ecs.entity {
        it += ControllableComponent()
    }

    override suspend fun Context.start() {

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }
        input.addActiveInputProcessor(object: InputProcessor {
            override fun keyDown(key: Key): Boolean {
                pressed = true
                playNormalSound()
                playFmodSound()
                return super.keyDown(key)
            }

            override fun keyUp(key: Key): Boolean {
                pressed = false
                return super.keyUp(key)
            }
        })

        onQuickUpdate {
            if (assetsLoaded) {
                assets.fmod.studioSystem.update()
            }
        }

        println(audio.sampleRate())

        onRender { dt ->
            if (!assetsLoaded) {
                assetsLoaded = assets.isLoaded
            }

            if (assetsLoaded) {
                if (!fmodAssetsLoaded) {
                    fmodAssetsLoaded = fmodAssets.isLoaded
                    if (fmodAssetsLoaded) {
                        fmodAssets.music.createInstance().apply {
                            start()
                            release()
                        }
                    }
                }

                if (fmodAssetsLoaded) {
                    gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
                    gl.clearColor(Color.BLACK)

                    viewport.apply(context)
                    batch.begin(camera.viewProjection)

                    ecs.update(dt)

                    shapeRenderer.filledRectangle(10f, 10f, 10f, 10f, color = if (pressed) pressedColor else idleColor)

                    with(ecs) {
                        update(dt)
                        shapeRenderer.filledRectangle(30f, 10f, 10f, 10f, color = if (entity[ControllableComponent].pressed) pressedColor else idleColor)
                    }

                    batch.end()
                }

            }


        }

        onDispose(::release)
    }

    fun playNormalSound() {
//        assets.sound.pluck.play(positionX = -100f, rolloffFactor = 0f, referenceDistance = 10000f)
    }

    fun playFmodSound() {
        fmodAssets.pluck.createInstance().apply {
            start()
            release()
        }
    }

    companion object {
        private val pressedColor = Color.GREEN.toFloatBits()
        private val idleColor = Color.WHITE.toFloatBits()
    }

}
