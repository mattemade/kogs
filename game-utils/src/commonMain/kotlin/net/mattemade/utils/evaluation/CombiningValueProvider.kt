package net.mattemade.utils.evaluation

class CombiningValueProvider<T, I>(private vararg val providers: ValueProvider<T, I>, private val combine: (acc: T, next: T) -> T): ValueProvider<T, I> {
    override fun get(i: I): T {
        val iterator = providers.iterator()
        var result  = iterator.next().get(i)
        while (iterator.hasNext()) {
            result = combine(result, iterator.next().get(i))
        }
        return result
    }
}