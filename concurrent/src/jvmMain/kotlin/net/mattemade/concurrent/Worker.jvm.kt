package net.mattemade.concurrent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

private var mainThread: Thread? = null
private val workers: ConcurrentHashMap<String, MessageChannel> = ConcurrentHashMap()
private val threads: ConcurrentHashMap<Long, String> = ConcurrentHashMap()


actual fun currentWorkerContext(): MessageChannel? {
    // TODO: we assume that this method will first be called on the main thread, maybe make it more explicit?
    if (mainThread == null) {
        mainThread = Thread.currentThread()
        return null
    }

    return workers[threads[Thread.currentThread().threadId()]]
}


// TODO: did you hear about performance?
private class JvmMessageChannel(
    val incomingQueue: LinkedBlockingQueue<Any> = LinkedBlockingQueue<Any>(),
    val outgoingQueue: LinkedBlockingQueue<Any> = LinkedBlockingQueue<Any>()
) : MessageChannel {

    constructor(other: JvmMessageChannel) : this(
        incomingQueue = other.outgoingQueue,
        outgoingQueue = other.incomingQueue
    )

    override fun write(message: Float) {
        outgoingQueue.put(message)
    }

    override fun write(message: Int) {
        outgoingQueue.put(message)
    }

    override fun write(message: String) {
        outgoingQueue.put(message)
    }

    override fun hasNext(): Boolean =
        incomingQueue.isNotEmpty()


    override fun readFloat(): Float =
        incomingQueue.poll() as Float


    override fun readInt(): Int =
        incomingQueue.poll() as Int


    override fun readString(): String =
        incomingQueue.poll() as String

    override fun terminate() {
        Thread.currentThread().interrupt()
    }

}

actual fun spawnWorker(
    fromFile: String,
    id: String,
    block: () -> Unit
): MessageChannel {
    val workerChannel = JvmMessageChannel()
    val reverseChanel = JvmMessageChannel(other = workerChannel)

    Thread {
        threads[Thread.currentThread().threadId()] = id
        workers[id] = workerChannel
        block()
        workers.remove(id)
        threads.remove(Thread.currentThread().threadId())
    }.apply {
        isDaemon = true
        start()
    }

    return reverseChanel
}

actual fun repeatEvery(ms: Int, block: () -> Unit) {
    val msLong = ms.toLong()
    while (true) {
        block()
        Thread.sleep(msLong)
    }
}
