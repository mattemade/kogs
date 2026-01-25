package net.mattemade.concurrency

interface Job {
    fun runInContext(executor: Executor)
}