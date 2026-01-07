package net.mattemade.evaluation

import net.mattemade.utils.evaluation.CachingValueProvider.Companion.cached
import net.mattemade.utils.evaluation.CombiningValueProvider
import net.mattemade.utils.evaluation.ConstantValueProvider.Companion.provide
import net.mattemade.utils.evaluation.ModifyingValueProvider.Companion.modify
import net.mattemade.utils.evaluation.ValueProvider

fun main() {
    val floatProvider = ValueProvider<Float, Int> {
        println("calculating floatProvider at $it")
        it.toFloat()
    }.cached()

    val doubleFloatProvider = floatProvider.modify { it * 2f }
    val constantProvider = 10f.provide<Float, Int>()

    val combined =
        CombiningValueProvider(
            floatProvider,
            doubleFloatProvider,
            constantProvider
        ) { acc, next -> acc + next }

    for (i in 0..10) {
        println("$i, ${floatProvider.get(i)}, ${doubleFloatProvider.get(i)}, ${combined.get(i)}")
    }
}