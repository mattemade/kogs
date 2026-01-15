package net.mattemade.gui.api.memory

open class Pool<T>(private val constructor: () -> T, initialCapacity: Int = 500) {

    private val recycledObjects = ArrayList<T>(initialCapacity)
    private var recycled: Int = 0

    fun borrow(): T {
        if (recycledObjects.isNotEmpty()) {
            return recycledObjects.removeAt(recycled--)
        }

        return constructor()
    }
    fun recycle(item: T) {
        recycledObjects.add(item)
        recycled++
    }

    companion object {
        fun <T> Pool<T>.use(block: T.() -> Unit) {
            val item = borrow()
            item.block()
            recycle(item)
        }
    }
}