package net.mattemade.gfxtest

import com.littlekt.Context
import com.littlekt.RemoveContextCallback
import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

private val CANVAS_ID = "canvas"

private lateinit var game: GfxTest
fun main() {
    createLittleKtApp {
        width = 960
        height = 540
        title = "game"
        canvas = document.getElementById(CANVAS_ID) as HTMLCanvasElement
    }.start {
        Logger.setLevels(Logger.Level.NONE)

        scheduleCanvasResize(it)
        game = GfxTest(
            context = it,
        )
        game
    }
}


private var zoom = 1f
private var zoomFactor = 1f
private fun scheduleCanvasResize(context: Context) {
    var removeContextCallback: RemoveContextCallback? = null
    removeContextCallback = context.onRender {
        zoom = (1 / window.devicePixelRatio).toFloat()
        //game.onCanvasZoomChanged(zoom) // to update ahead of resize
        // resize the canvas to fit max available space
        val canvas = document.getElementById(CANVAS_ID) as HTMLCanvasElement
        canvas.style.apply {
            display = "block"
            position = "absolute"
            top = "0"
            bottom = "0"
            left = "0"
            right = "0"
            width = "100%"
            height = "100%"
            // scale the canvas take all the available device pixel of hi-DPI display
            this.asDynamic().zoom =
                "$zoom" // TODO: makes better pixels but impacts performance in firefox
        }
        //canvas.getContext("webgl2").asDynamic().translate(0.5f, 0.5f)
        removeContextCallback?.invoke()
        removeContextCallback = null
    }
}