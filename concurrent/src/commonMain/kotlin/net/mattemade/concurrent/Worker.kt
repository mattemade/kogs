package net.mattemade.concurrent

interface MessageChannel {
    fun write(message: Float)
    fun write(message: Int)
    fun write(message: String)

    fun hasNext(): Boolean
    fun readFloat(): Float
    fun readInt(): Int
    fun readString(): String

    fun terminate()
}

expect fun spawnWorker(fromFile: String, id: String, block: () -> Unit): MessageChannel
expect fun currentWorkerContext(): MessageChannel?
expect fun repeatEvery(ms: Int, block: () -> Unit)
