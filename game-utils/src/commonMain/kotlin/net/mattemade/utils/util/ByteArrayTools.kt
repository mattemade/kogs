package net.mattemade.utils.util

class ByteArrayTools {

    fun pack(arrayToReuse: ByteArray? = null, block: Appender.() -> Unit): ByteArray {
        return ByteArrayBuilder(arrayToReuse).apply(block).build()
    }

    fun unpack(bytes: ByteArray): Provider =
        Unpacker(bytes)

    interface Appender {
        fun currentOffset(): Int
        fun update(offset: Int, value: Byte)
        fun append(value: Byte)
        fun append(value: Int)
        fun append(value: String)
        fun append(value: IntArray)
        fun append(value: Array<IntArray>)
    }

    interface Provider {
        fun currentOffset(): Int
        fun nextByte(): Byte
        fun nextInt(): Int
        fun nextString(): String
        fun nextIntArray(): IntArray
        fun nextArrayIntArray(): Array<IntArray>
    }


    private class ByteArrayBuilder(val arrayToReuse: ByteArray? = null) : Appender {

        private val typeOrder = mutableListOf<Byte>()
        private val objects = mutableListOf<Any>()
        private var size = 0

        override fun currentOffset(): Int = if (arrayToReuse != null) offset else size

        override fun update(offset: Int, value: Byte) {
            result[offset] = value
        }

        override fun append(value: Int) {
            if (arrayToReuse != null) {
                insert(value)
                return
            }
            objects += value
            typeOrder += TYPE_INT
            size += 4
        }

        override fun append(value: Byte) {
            if (arrayToReuse != null) {
                insert(value)
                return
            }
            objects += value
            typeOrder += TYPE_BYTE
            size +=1
        }

        override fun append(value: String) {
            if (arrayToReuse != null) {
                insert(value.length)
                for (char in value) insert(char.code)
                return
            }
            objects += value
            typeOrder += TYPE_STRING
            size += 4 + value.length * 4
        }

        override fun append(value: IntArray) {
            if (arrayToReuse != null) {
                insert(value.size)
                for (item in value) insert(item)
                return
            }
            objects += value
            typeOrder += TYPE_INT_ARRAY
            size += 4 + value.size * 4
        }

        override fun append(value: Array<IntArray>) {
            if (arrayToReuse != null) {
                insert(value.size)
                for (inner in value) {
                    insert(inner.size)
                    for (item in inner) insert(item)
                }
                return
            }
            objects += value as Any
            typeOrder += TYPE_TYPED_ARRAY
            size += 4 + value.sumOf { 4 + it.size * 4 }
        }

        val result by lazy { arrayToReuse ?: ByteArray(size) }
        private var offset = 0
        private var typeIndex = 0

        private fun insert(value: Byte) {
            result[offset++] = value
        }

        private fun insert(value: Int) {
            for (i in 0..3) insert((value shr (i * 8)).toByte())
        }

        fun build(): ByteArray {
            if (arrayToReuse != null) {
                return arrayToReuse
            }

            if (offset > 0) {
                throw IllegalStateException("Impossible to reuse builder")
            }

            for (obj in objects) {
                insertSomething(obj)
            }

            return result
        }

        private fun insertSomething(obj: Any) {
            when (typeOrder[typeIndex++]) {
                TYPE_BYTE -> {
                    insert(obj as Byte)
                }

                TYPE_INT -> {
                    insert(obj as Int)
                }

                TYPE_STRING -> {
                    insert((obj as String).length)
                    for (char in obj) insert(char.code)
                }

                TYPE_INT_ARRAY -> {
                    insert((obj as IntArray).size)
                    for (item in obj) insert(item)
                }

                TYPE_TYPED_ARRAY -> {
                    insert((obj as Array<IntArray>).size)
                    for (inner in obj) {
                        insert(inner.size)
                        for (item in inner) {
                            insert(item)
                        }
                    }
                }
            }
        }
    }


    private class Unpacker(val bytes: ByteArray) : Provider {
        var index = 0

        private fun readInt(): Int =
            ((bytes[index++].toInt() and 0xff) or
                    (bytes[index++].toInt() and 0xff shl 8) or
                    (bytes[index++].toInt() and 0xff shl 16) or
                    (bytes[index++].toInt() shl 24))

        override fun currentOffset(): Int = index

        override fun nextByte(): Byte = bytes[index++]

        override fun nextInt(): Int = readInt()

        override fun nextString(): String {
            val stringLength = readInt()
            val builder = StringBuilder(stringLength)
            repeat(stringLength) {
                builder.append(Char(readInt()))
            }
            return builder.toString()
        }

        override fun nextIntArray(): IntArray = IntArray(readInt()) { readInt() }

        override fun nextArrayIntArray(): Array<IntArray> = Array(readInt()) { nextIntArray() }
    }

    private companion object {
        const val TYPE_BYTE: Byte = 0
        const val TYPE_INT: Byte = 1
        const val TYPE_STRING: Byte = 2
        const val TYPE_INT_ARRAY: Byte = 3
        const val TYPE_TYPED_ARRAY: Byte = 4
    }
}
