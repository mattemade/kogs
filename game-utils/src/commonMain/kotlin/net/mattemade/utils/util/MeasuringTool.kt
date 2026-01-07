package net.mattemade.utils.util

import org.jbox2d.internal.System_nanoTime

class MeasuringTool {

    var ticks: Int = 0

    private val operations = mutableMapOf<String, Long>()

    private var lastCheck: Long = 0

    fun cycleStarts() {
        lastCheck = System_nanoTime()
    }

    fun operationCompletes(tag: String) {
        val now = System_nanoTime()
        val diff = now - lastCheck
        lastCheck = now
        operations[tag] = (operations[tag] ?: 0) + diff
    }

    fun cycleEnds() {
        ticks++
        operations.forEach { (key, value) ->
            println("$key avg ${value / 1000000f / ticks}")
        }
    }
}