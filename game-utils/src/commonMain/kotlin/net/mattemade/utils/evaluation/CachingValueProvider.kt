package net.mattemade.utils.evaluation

class CachingValueProvider<T, I>(private val base: ValueProvider<T, I>): ValueProvider<T, I> {
    private val cache = mutableMapOf<I, T>()
    override fun get(i: I): T = cache.getOrPut(i) { base.get(i) }

    companion object {
        fun <T, I> ValueProvider<T, I>.cached() = CachingValueProvider(this)
    }
}