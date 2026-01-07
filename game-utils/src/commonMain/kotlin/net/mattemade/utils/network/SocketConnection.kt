package net.mattemade.utils.network

interface SocketConnection {
    val isConnecting: Boolean
    val isConnected: Boolean
    fun reconnect()
    fun disconnect()
    fun send(message: String)
    fun send(message: Array<Byte>)
}