package com.littlekt

import com.littlekt.audio.globalAudioContext
import com.littlekt.graphics.Color
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

/** Properties related to creating a [LittleKtApp] */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var assetsDir: String = "./"
    var backgroundColor: Color = Color.CLEAR
}

/**
 * Creates a new [LittleKtApp] containing [LittleKtProps] as the [ContextConfiguration] for building
 * a [Context].
 */
actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    props.action()

    // trigger the initialization of AudioContext to enable sound in certain web browsers
    val userInteractionEventNames = listOf(
        "click",
        "contextmenu",
        "auxclick",
        "doubleclick",
        "mousedown",
        "mouseup",
        "pointerup",
        "touchstart",
        "touchend",
        "keydown"
    )
    val listener = object : EventListener {
        override fun handleEvent(event: Event) {
            val audioContext = globalAudioContext
            if (audioContext != null) {
                if (audioContext.state != "running") {
                    audioContext.resume()
                }
                userInteractionEventNames.forEach {
                    document.removeEventListener(it, this)
                }
            }

        }
    }
    userInteractionEventNames.forEach { eventName ->
        document.addEventListener(eventName, listener)
    }

    return LittleKtApp(
        WebGLContext(
            JsConfiguration(props.title, props.canvasId, props.assetsDir, props.backgroundColor)
        )
    )
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class JsConfiguration(
    override val title: String = "LittleKt - JS",
    val canvasId: String = "canvas",
    val rootPath: String = "./",
    val backgroundColor: Color = Color.CLEAR,
) : ContextConfiguration()

val PowerPreference.nativeFlag: String
    get() =
        when (this) {
            PowerPreference.LOW_POWER -> "low-power"
            PowerPreference.HIGH_POWER -> "high-performance"
        }
