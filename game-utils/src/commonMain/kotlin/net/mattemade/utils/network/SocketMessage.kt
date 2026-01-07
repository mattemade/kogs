package net.mattemade.utils.network

sealed interface SocketMessage {
    object Connected: SocketMessage
    object Disconnected: SocketMessage
    object Error: SocketMessage
    class Text(val value: String): SocketMessage
    class Bytes(val value: ByteArray): SocketMessage
}