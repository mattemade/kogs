package com.littlekt

import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.littlekt.async.KtScope
import com.littlekt.async.MainDispatcher
import com.littlekt.async.mainThread
import com.littlekt.audio.AndroidAudioContext
import com.littlekt.file.AndroidVfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.internal.InternalResources
import com.littlekt.input.AndroidInput
import com.littlekt.log.Logger
import com.littlekt.util.fastForEach
import com.littlekt.util.internal.now
import com.littlekt.view.LittleKtSurfaceView
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AndroidContext(override val configuration: AndroidConfiguration) : Context() {
    override val stats: AppStats = AppStats()
    override val graphics: AndroidGraphics = AndroidGraphics(stats.engineStats)
    override val input: AndroidInput = AndroidInput(configuration.activity, graphics)
    override val logger: Logger = Logger(configuration.title)
    override val vfs: AndroidVfs =
        AndroidVfs(
            configuration.activity,
            configuration.activity.assets,
            configuration.activity.getPreferences(android.content.Context.MODE_PRIVATE),
            this, logger, "./.storage", "."
        )
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")
    override val platform: Platform = Platform.ANDROID
    override val clipboard: AndroidClipboard = AndroidClipboard(configuration.activity)

    val audioContext: AndroidAudioContext = AndroidAudioContext(configuration.activity)
    private var canUpdate = AtomicBoolean(false)

    init {
        KtScope.initiate()
        mainThread = Thread.currentThread()
        setupConfig()
    }

    private fun setupConfig() {
        val activity = configuration.activity
        val window = activity.window
        if (!configuration.showStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.decorView.systemUiVisibility = 0x1
            }
        }

        if (configuration.useWakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (configuration.useImmersiveMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
            } else {
                window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }

    }

    fun resume() {
        setupConfig()
        audioContext.resume()
    }

    fun pause() {
        audioContext.pause()
    }

    override fun start(build: (app: Context) -> ContextListener) {
        graphics.onCreate = {
            KtScope.launch {
                InternalResources.createInstance(this@AndroidContext)
                InternalResources.INSTANCE.load()

                gl.clearColor(configuration.backgroundColor)

                val size = Point()
                configuration.activity.windowManager.defaultDisplay.getSize(size)

                val listener = build(this@AndroidContext)
                listener.run {
                    start()
                    resizeCalls.forEach {
                        it.invoke(size.x, size.y)
                    }
                }
                canUpdate.set(true)
            }
        }
        graphics.onResize = { width, height ->
            resizeCalls.forEach {
                it.invoke(width, height)
            }
        }
        graphics.onDrawFrame = {
            calcFrameTimes(now().milliseconds)
            MainDispatcher.INSTANCE.executePending(available)
            if (canUpdate.get()) {
                update(dt)
            }
        }

        val surfaceView = LittleKtSurfaceView(configuration.activity).apply {
            setRenderer(graphics)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener(input)
            setOnKeyListener(input)
        }.also { graphics.surfaceView = it }
        configuration.activity.setContentView(surfaceView)
    }

    private fun update(dt: Duration) {

        stats.engineStats.resetPerFrameCounts()

        invokeAnyRunnable()

        input.update()
        stats.update(dt)
        renderCalls.fastForEach { render -> render(dt) }
        postRenderCalls.fastForEach { postRender -> postRender(dt) }

        input.reset()
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable ->
                postRunnable.invoke()
            }
            postRunnableCalls.clear()
        }
    }


    override fun close() {

    }

    override fun destroy() {
        KtScope.launch {
            disposeCalls.fastForEach { dispose -> dispose() }
            audioContext.release()
        }
    }

}