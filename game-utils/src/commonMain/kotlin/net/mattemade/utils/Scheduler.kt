package net.mattemade.utils

class Scheduler {


    private val activeOperations = mutableListOf<TaskSequence>()
    private val taggedOperations = mutableMapOf<Any, TaskSequence>()

    fun schedule(): TaskSequence =
        schedule(Unit)

    fun schedule(tag: Any): TaskSequence {
        return TaskSequence(tag).also {
            activeOperations += it
            if (tag != Unit) {
                taggedOperations[tag] = it
            }
        }
    }

    fun isActive(tag: Any): Boolean = taggedOperations.containsKey(tag)
    fun isActive(): Boolean = activeOperations.isNotEmpty()

    fun forceFinish(tag: Any) {
        taggedOperations.remove(tag)?.let {
            activeOperations.remove(it)
            it.update(9001f)
        }
    }

    fun forceFinish() {
        while (activeOperations.isNotEmpty()) {
            for (i in 0 ..< activeOperations.size) {
                val operation = activeOperations[i]
                operation.completed = !operation.update(9001f)
            }
            activeOperations.removeAll {
                if (it.completed) {
                    taggedOperations.remove(it.tag)
                }
                it.completed
            }
        }
        //activeOperations.clear() // they are already cleared
        taggedOperations.clear()
    }

    fun forceStop(tag: Any) {
        taggedOperations.remove(tag)?.let {
            activeOperations.remove(it)
        }
    }

    fun forceStop() {
        activeOperations.clear()
        taggedOperations.clear()
    }

    fun update(dt: Float): Boolean {
        val result = activeOperations.isNotEmpty()
        for (i in 0 ..< activeOperations.size) {
            val operation = activeOperations[i]
            operation.completed = !operation.update(dt)
        }
        activeOperations.removeAll {
            if (it.completed) {
                taggedOperations.remove(it.tag)
            }
            it.completed
        }
        return result
    }

    class TaskSequence(val tag: Any) {

        private var timeToNextTask: Float = 0f
        private var currentTaskIndex = -1
        private var currentTask: TimerWithProgress? = null
        private val tasks = mutableListOf<TimerWithProgress>()
        var completed: Boolean = false

        fun update(dt: Float): Boolean {
            timeToNextTask -= dt
            while (timeToNextTask <= 0f) {
                currentTask?.onProgressChanged?.invoke(1f)
                tasks.getOrNull(++currentTaskIndex)?.let {
                    currentTask = it
                    timeToNextTask += it.duration
                }?: return false
            }
            currentTask?.let {
                it.onProgressChanged.invoke(it.interpolator.map(1f - timeToNextTask / it.duration))
            }
            return true
        }

        fun then(
            duration: Float = 0f,
            interpolator: Interpolator = Interpolator.Smoothstep,
            action: (progress: Float) -> Unit = {},
        ): TaskSequence {
            tasks += TimerWithProgress(duration, interpolator, action)
            return this
        }
    }

    class TimerWithProgress(
        val duration: Float,
        val interpolator: Interpolator,
        val onProgressChanged: (progress: Float) -> Unit,
    )

    sealed interface Interpolator {

        fun map(t: Float): Float

        object Linear: Interpolator {
            override fun map(t: Float): Float = t
        }

        object Quadratic: Interpolator {
            override fun map(t: Float): Float =  t * t
        }

        object Smoothstep: Interpolator {
            override fun map(t: Float): Float =  t * t * (3f - 2f * t)
        }
    }
}