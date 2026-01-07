package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice
import net.mattemade.utils.util.ByteArrayTools

class TiledMapPacker {

    private var reusableArray: ByteArray? = null

    fun markDirty() {
        reusableArray = null
    }

    fun pack(map: TiledMap): ByteArray =
        reusableArray ?: ByteArrayTools().pack{
            append(map.width)
            append(map.height)
            append(map.cachedTileSets.size)
            for (tileSetKey in map.cachedTileSets) {
                append(tileSetKey.first)
                append(tileSetKey.second)
                append(tileSetKey.third)
            }
            append(map.layers.size)
            for (layer in map.layers.values) {
                append(layer.tag)
                append(layer.mark)
                when (layer) {
                    is TiledLayer.Simple -> {
                        append(LAYER_TYPE_SIMPLE)
                        append(layer.tiles)
                    }

                    is TiledLayer.Quarters -> {
                        append(LAYER_TYPE_QUARTERS)
                        append(layer.tileSetKey)
                        val offset = currentOffset()
                        layer.onSomethingChanged { x, y, state ->
                            reusableArray?.set(offset + y + x * map.width, (if (state) 1 else 0).toByte())
                        }
                        for (x in 0..<map.width) {
                            for (y in 0..<map.height) {
                                val state = layer.state(x, y)
                                append((if (state) 1 else 0).toByte())
                            }
                        }
                    }

                    is TiledLayer.DualGrid -> {
                        append(LAYER_TYPE_DUAL_GRID)
                        append(layer.tileSetKey)
                        append(layer.sliceType)
                        val offset = currentOffset()
                        layer.onSomethingChanged { x, y, state ->
                            reusableArray?.set(offset + y + x * map.width, (if (state) 1 else 0).toByte())
                        }
                        for (x in 0..<map.width) {
                            for (y in 0..<map.height) {
                                val state = layer.state(x, y)
                                append((if (state) 1 else 0).toByte())
                            }
                        }
                    }

                    is TiledLayer.Alternating -> {
                        append(LAYER_TYPE_ALTERNATING)
                        append(layer.tileWidth)
                        append(layer.tileHeight)
                        val offset = currentOffset()
                        layer.onSomethingChanged { x, y, state ->
                            reusableArray?.set(offset + y + x * map.width, (if (state) 1 else 0).toByte())
                        }
                        for (x in 0..<map.width) {
                            for (y in 0..<map.height) {
                                val state = layer.state(x, y)
                                append((if (state) 1 else 0).toByte())
                            }
                        }
                    }
                }
            }
            append(map.order.size)
            map.order.forEach {
                append(it)
            }
        }.also {
            reusableArray = it
        }

    fun unpack(bytes: ByteArray, caching: Boolean = false, provideTileSet: (String) -> TextureSlice): TiledMap =
        ByteArrayTools().unpack(bytes).run {
            val width = nextInt()
            val height = nextInt()
            TiledMap(width, height, provideTileSet).apply {
                repeat(nextInt()) {
                    cacheTileSet(nextString(), nextInt(), nextInt())
                }
                repeat(nextInt()) { index ->
                    val tag = nextString()
                    val mark = nextInt()
                    when (nextInt()) {
                        LAYER_TYPE_SIMPLE -> {
                            val tiles: Array<IntArray> = nextArrayIntArray()
                            createSimpleLayer(tag, tiles)
                        }
                        LAYER_TYPE_QUARTERS -> {
                            val layer = createQuartersLayer(tag, nextString())
                            if (caching) {
                                val offset = currentOffset()
                                layer.onSomethingChanged { x, y, state ->
                                    reusableArray?.set(offset + y + x * width, (if (state) 1 else 0).toByte())
                                }
                            }
                            for (x in 0..<width) {
                                for (y in 0..<height) {
                                    if (nextByte() > 0) {
                                        changeTile(tag, x, y, 1)
                                    }
                                }
                            }
                        }
                        LAYER_TYPE_DUAL_GRID -> {
                            val layer = createDualGridLayer(tag, nextString(), nextInt())
                            if (caching) {
                                val offset = currentOffset()
                                layer.onSomethingChanged { x, y, state ->
                                    reusableArray?.set(offset + y + x * width, (if (state) 1 else 0).toByte())
                                }
                            }
                            for (x in 0..<width) {
                                for (y in 0..<height) {
                                    if (nextByte() > 0) {
                                        changeTile(tag, x, y, 1)
                                    }
                                }
                            }
                        }
                        LAYER_TYPE_ALTERNATING -> {
                            val layer = createAlternatingGrid(tag, nextString(), nextInt(), nextInt(), nextInt())
                            if (caching) {
                                val offset = currentOffset()
                                layer.onSomethingChanged { x, y, state ->
                                    reusableArray?.set(offset + y + x * width, (if (state) 1 else 0).toByte())
                                }
                            }
                            for (x in 0..<width) {
                                for (y in 0..<height) {
                                    if (nextByte() > 0) {
                                        changeTile(tag, x, y, 1)
                                    }
                                }
                            }
                        }

                        else -> throw IllegalStateException()
                    }
                    addLayerMark(tag, mark)
                }
                order.clear()
                repeat(nextInt()) {
                    order.add(nextString())
                }
                if (caching) {
                    reusableArray = bytes
                }
            }
        }


    companion object {
        private const val LAYER_TYPE_SIMPLE = 0
        private const val LAYER_TYPE_QUARTERS = 1
        private const val LAYER_TYPE_DUAL_GRID = 2
        private const val LAYER_TYPE_ALTERNATING = 3
    }
}