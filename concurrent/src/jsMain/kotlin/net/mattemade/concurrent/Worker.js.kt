package net.mattemade.concurrent

import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker

actual fun spawnWorker(
    fromFile: String, id: String,
    block: () -> Unit
): MessageChannel {
    return JsMessageChannel(Worker(scriptURL = fromFile))
}

private class JsMessageChannel(val selfPort: dynamic) : MessageChannel {
    private val incomingQueue = mutableListOf<Any>()

    init {
        selfPort.onmessage = { event: MessageEvent ->
            event.data?.let { incomingQueue.add(it) }
        }
    }

    override fun write(message: Float) {
        selfPort.postMessage(message)
    }

    override fun write(message: Int) {
        selfPort.postMessage(message)
    }

    override fun write(message: String) {
        selfPort.postMessage(message)
    }

    override fun hasNext(): Boolean =
        incomingQueue.isNotEmpty()

    override fun readFloat(): Float =
        incomingQueue.removeFirst() as Float


    override fun readInt(): Int =
        incomingQueue.removeFirst() as Int

    override fun readString(): String =
        incomingQueue.removeFirst() as String

    override fun terminate() {
        selfPort.terminate()
    }

}

actual fun currentWorkerContext(): MessageChannel? {
    val isWorker: Boolean =
        js("typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope") as Boolean
    return if (isWorker) {
        JsMessageChannel(js("self"))
    } else {
        null
    }
}

internal external fun setInterval(handler: dynamic, timeout: Int)

actual fun repeatEvery(ms: Int, block: () -> Unit) {
    setInterval(block, ms)
}
