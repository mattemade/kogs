package net.mattemade.concurrency

class System {

    private val executors = mutableMapOf<Executor.Type, RoundRobinMutableList<Executor>>()

    fun createScheduler(type: Executor.Type, id: String): Executor {
        return Executor(type, id)
    }

    fun schedule(type: Executor.Type, job: Job) {
        executors[type]?.next?.execute(job)
    }

    fun <T> createChannel(left: Executor, right: Executor) {

    }

    private class RoundRobinMutableList<T> {
        val list = mutableListOf<T>()
        private var index = 0

        val next: T
            get() {
                index %= list.size
                return list[index++]
            }
    }

}