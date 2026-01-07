package net.mattemade.gametemplate

import com.littlekt.Context
import com.littlekt.RemoveContextCallback
import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.WebSocket
import org.w3c.fetch.RequestInit

private const val CANVAS_ID = "canvas"
val canvas = document.getElementById(CANVAS_ID) as HTMLCanvasElement

external fun encodeURIComponent(encodedURI: String): String
external fun decodeURIComponent(encodedURI: String): String

private lateinit var game: TemplateGame
fun main() {
    var sheetId: String? = null
    window.location.href.takeIf { it.contains('?') }
        ?.substringAfter('?')
        ?.split("&")
        ?.asSequence()
        ?.forEach {
            val split = it.split("=")
            val key = split[0]
            val value = split.getOrNull(1)?.let { decodeURIComponent(it) }
            when (key) {
                "sheet" -> sheetId = value
            }
        }

    createLittleKtApp {
        width = 960
        height = 540
        title = TemplateGame.TITLE
        canvasId = CANVAS_ID
    }.start {
        Logger.setLevels(Logger.Level.NONE)

        scheduleCanvasResize(it)
        game = TemplateGame(
            context = it,
            zoomCanvasIn = ::zoomCanvasIn,
            log = ::sendLog,
            encodeUrlComponent = ::encodeURIComponent,
            getRequest = ::getRequest,
            getBlocking = { null },
            postRequest = ::postRequest,
            connect = ::createSocketConnection,
            overrideResourcesFrom = sheetId,
        )
        window.addEventListener("blur", { game.blur() })
        window.addEventListener("focus", { game.focus() })
        window.addEventListener("beforeunload ", { game.destroy() })
        document.addEventListener("pointerlockchange", {
            if (document.asDynamic().pointerLockElement !== canvas) {
                game.pointerLockReleased()
            }
        })
        game
    }
}

private var zoom = 1f
private var zoomFactor = 1f
private fun scheduleCanvasResize(context: Context) {
    var removeContextCallback: RemoveContextCallback? = null
    removeContextCallback = context.onRender {
        zoom = (1 / window.devicePixelRatio).toFloat()
        game.onCanvasZoomChanged(zoom) // to update ahead of resize
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

private fun zoomCanvasIn() {
    zoomFactor += 1f
    val setZoom = zoom * zoomFactor
    game.onCanvasZoomChanged(setZoom) // to update ahead of resize
    canvas.style.asDynamic().zoom = "$setZoom"
}


private var sendingLogs = true
private fun sendLog(text: String) {
    if (sendingLogs) {
        postRequest("https://demo.mattemade.net/data", text) {
            sendingLogs = it != null
        }
    }
}

private fun getRequest(url: String, callback: (List<String>?) -> Unit) {
    request(url, "GET", null, callback)
}

private fun postRequest(url: String, body: String, callback: (List<String>?) -> Unit) {
    request(url, "POST", body, callback)
}

private fun request(
    url: String,
    method: String,
    body: String? = null,
    callback: (List<String>?) -> Unit
) {
    window.fetch(
        url, init = RequestInit(
            method = method,
            body = body
        )
    )
        .then { it.text() }
        .then(onFulfilled = {
            callback(it.lines())
        }, onRejected = {
            callback.invoke(null)
        })
}

private fun createSocketConnection(
    url: String,
    callback: (SocketMessage) -> Unit,
): SocketConnection {
    lateinit var connection: SocketConnection

    var socket: WebSocket? = null

    fun createSocket() {
        socket?.onmessage = null
        socket?.onerror = null
        socket?.onclose = null
        socket?.close()
        val newSocket = WebSocket(url)
        newSocket.onmessage = {
            /*val arrayBuffer = it.data as? ArrayBuffer
            if (arrayBuffer != null) {
                callback(SocketMessage.Bytes(arrayBuffer))
            } else {*/
            it.data?.toString()?.let { text ->
                callback(SocketMessage.Text(text))
            }
            //}
        }
        newSocket.onopen = {
            callback(SocketMessage.Connected)
        }
        newSocket.onerror = {
            callback(SocketMessage.Error)
        }
        newSocket.onclose = {
            callback(SocketMessage.Disconnected)
        }
        socket = newSocket
    }

    createSocket()

    connection = object : SocketConnection {
        override val isConnecting: Boolean
            get() = socket?.readyState == WebSocket.CONNECTING
        override val isConnected: Boolean
            get() = socket?.readyState == WebSocket.OPEN

        override fun reconnect() {
            createSocket()
        }

        override fun disconnect() {
            socket?.close()
        }

        override fun send(message: String) {
            if (isConnected) {
                socket?.send(message)
            }
        }

        override fun send(message: Array<Byte>) {
            if (isConnected) {
                socket?.send(Int8Array(message))
            }
        }
    }
    return connection
}