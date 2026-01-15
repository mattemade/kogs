package net.mattemade.gui.api.collection

object Util {

    fun <T> List<T>.fastForEach(action: (T) -> Unit) {
        var index = 0
        val size = size
        while (index < size) {
            action(get(index++))
        }
    }
}