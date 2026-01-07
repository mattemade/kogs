package net.mattemade.utils.evaluation

class ConstantValueProvider<T, I>(private val constant: T): ValueProvider<T, I> {
    override fun get(i: I): T = constant

    companion object {
        fun <T, I> T.provide(): ValueProvider<T, I> = ConstantValueProvider<T, I>(this)
    }
}