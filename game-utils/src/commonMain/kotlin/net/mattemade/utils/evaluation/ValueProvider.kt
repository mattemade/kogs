package net.mattemade.utils.evaluation

fun interface ValueProvider<T, I> {
    fun get(i: I): T
}