package com.littlekt.util.datastructure

class BiMap<K, V> {

    private val directMap = mutableMapOf<K, V>()
    private val reverseMap = mutableMapOf<V, K>()

    val entries: MutableSet<MutableMap.MutableEntry<K, V>> = directMap.entries
    val keys: MutableSet<K> = directMap.keys
    val values: MutableSet<V> = reverseMap.keys
    val size: Int
        get() = directMap.size

    fun put(key: K, value: V): V? = directMap.put(key, value).also { reverseMap[value] = key }
    fun putReverse(value: V, key: K): K? = reverseMap.put(value, key).also { directMap[key] = value }

    fun get(key: K): V? = directMap[key]
    fun getReverse(value: V): K? = reverseMap[value]

    fun containsKey(key: K): Boolean = directMap.containsKey(key)
    fun containsValue(value: V): Boolean = directMap.containsValue(value)

    fun clear() {
        directMap.clear()
        reverseMap.clear()
    }

    fun isEmpty(): Boolean = directMap.isEmpty()

    fun removeKey(key: K): V? = directMap.remove(key)?.also { reverseMap.remove(it) }
    fun removeValue(value: V): K? = reverseMap.remove(value)?.also { directMap.remove(it) }
}
