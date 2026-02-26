package net.mattemade.platformer

import com.littlekt.createLittleKtApp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import net.mattemade.utils.network.SocketConnection
import net.mattemade.utils.network.SocketMessage

fun main() {
    createLittleKtApp {
        width = 1920
        height = 1080
        title = PlatformerGame.TITLE
        //vSync = false
    }.start {
        PlatformerGame(
            it,
            zoomCanvasIn = {},
            log = ::println,
            encodeUrlComponent = { it },
            getRequest = { url: String, function: (List<String>?) -> Unit ->
                println("GET $url")
            },
            getBlocking = { url: String ->
                println("GET $url")
                runBlocking {
                    client.get(url).bodyAsText().lines()
                }
            },
            postRequest = { url: String, body: String, function: (List<String>?) -> Unit ->
                println("POST $url $body")
            },
            connect = ::createDummySocket,
//            fmodFolderPrefix = "",
            fmodFolderPrefix = "src/commonMain/resources/",
            fmodLiveUpdate = true,
            //overrideResourcesFrom = "1FpyVhINl7oAzrfB_t-r-wL9SKcYUfyZjVM-FYrejAuY"
        ).also { it.focus() }
    }
}


private val client = HttpClient(CIO) {
    install(WebSockets)
}


fun createDummySocket(url: String, callback: (SocketMessage) -> Unit): SocketConnection = object : SocketConnection {

    init {
        println("socket connection: $url")
    }

    private var _isConnected = true

    override val isConnecting: Boolean
        get() = false
    override val isConnected: Boolean
        get() = _isConnected

    override fun reconnect() {
        println("socket reconnect: $url")
        _isConnected = true
    }

    override fun disconnect() {
        println("socket disconnect: $url")
        _isConnected = false
        callback.invoke(SocketMessage.Disconnected)
    }

    override fun send(message: String) {
        println("socket send: $url, $message")
        callback.invoke(SocketMessage.Text(message))
    }

    override fun send(message: Array<Byte>) {
        println("socket send: $url, $message")
        callback.invoke(SocketMessage.Bytes(message.toByteArray()))
    }

}