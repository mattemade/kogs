package net.mattemade.concurrency

class Executor(val type: Type, val id: String) {

    private val connections = mutableMapOf<String, Channel>()

    fun execute(job: Job) {
        job.runInContext(this)
    }

    fun connectTo(id: String, channel: Channel) {
        connections[id] = channel
    }

    fun sendMessage(toId: String, message: Any) {
        connections[id]?.send(message)
    }

    fun receiveMessage(fromId: String, message: Any) {

    }

    enum class Type {
        MAIN,
        IO,
    }
}