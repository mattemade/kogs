package net.mattemade.utils.evaluation

class ModifyingValueProvider<T, I>(private val base: ValueProvider<T, I>, private val modify: (T) -> T): ValueProvider<T, I> {
    override fun get(i: I): T
        = modify(base.get(i))

    companion object {
        fun <T, I> ValueProvider<T, I>.modify(modify: (T) -> T) = ModifyingValueProvider(this, modify)
    }
}