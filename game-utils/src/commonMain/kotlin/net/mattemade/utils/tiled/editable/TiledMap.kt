package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice

class TiledMap(val width: Int, val height: Int, val provideTileSet: (String) -> TextureSlice) {

    val layers = mutableMapOf<String, TiledLayer>()
    val order = mutableListOf<String>()
    val cachedTileSets = mutableListOf<Triple<String, Int, Int>>()
    val simpleTileSets = mutableMapOf<Int, TextureSlice>()
    var nextTileSetIndex = 1

    inline fun cookPie(x: Int, y: Int, crossinline accept: (TextureSlice?, Float, Float) -> Unit) {
        order.forEach { tag ->
            layers[tag]?.let { layer ->
                when (layer) {
                    is TiledLayer.Simple -> {
                        simpleTileSets[layer.tiles[x][y]]?.let { accept(it, 0f, 0f) }
                    }

                    is TiledLayer.Quarters -> {
                        for (innerLayer in layer.layers(x, y)) {
                            accept(innerLayer, 0f, 0f)
                        }
                    }

                    is TiledLayer.DualGrid -> accept(layer.tile(x - 1, y - 1), -0.5f, -0.5f)

                    is TiledLayer.Alternating -> accept(layer.tile(x, y), 0f, 0f)
                }
            }
        }
    }

    fun createSimpleLayer(tag: String) {
        layers.put(tag, TiledLayer.Simple(tag, width, height))
        order.add(tag)
    }

    fun createSimpleLayer(tag: String, fromTiles: Array<IntArray>) {
        layers.put(tag, TiledLayer.Simple(fromTiles, tag))
        order.add(tag)
    }

    fun addLayerMark(tag: String, markMask: Int) {
        layers[tag]?.let {
            it.mark = it.mark or markMask
        }
    }

    fun removeLayerMark(tag: String, markMask: Int) {
        layers[tag]?.let {
            it.mark = it.mark and -markMask
        }

    }

    fun createQuartersLayer(tag: String, tileSetKey: String, marks: Int = 0): TiledLayer.Quarters {
        val quarterAutoTile =
            QuarterAutoTile(provideTileSet(tileSetKey), width, height, emptySlices)
        val layer = TiledLayer.Quarters(
            tileSetKey,
            quarterAutoTile::onChange,
            quarterAutoTile::currentState,
            quarterAutoTile::tileLayers,
            tag
        )
        layer.mark = marks
        layers[tag] = layer
        order.add(tag)
        return layer
    }

    fun createDualGridLayer(
        tag: String,
        tileSetKey: String,
        sliceType: Int = 0,
        marks: Int = 0
    ): TiledLayer.DualGrid {
        val quarterAutoTile = DualGridAutoTile(provideTileSet(tileSetKey), width, height, sliceType)
        val layer = TiledLayer.DualGrid(
            tileSetKey,
            quarterAutoTile::onChange,
            quarterAutoTile::currentState,
            quarterAutoTile::tile,
            sliceType,
            tag,
        )
        layer.mark = marks
        layers[tag] = layer
        order.add(tag)
        return layer
    }

    fun createAlternatingGrid(
        tag: String,
        tileSetKey: String,
        tileWidth: Int,
        tileHeight: Int,
        marks: Int = 0
    ): TiledLayer.Alternating {
        val alternatingAutoTile = AlternatingAutoTile(provideTileSet(tileSetKey), tileWidth, tileHeight, width, height)
        val layer = TiledLayer.Alternating(
            tileSetKey,
            alternatingAutoTile::onChange,
            alternatingAutoTile::currentState,
            alternatingAutoTile::tile,
            tileWidth,
            tileHeight,
            tag,
        )
        layer.mark = marks
        layers[tag] = layer
        order.add(tag)
        return layer
    }

    fun cacheTileSet(tileSetKey: String, tileWidth: Int, tileHeight: Int) {
        cachedTileSets += Triple(tileSetKey, tileWidth, tileHeight)
        for (row in provideTileSet(tileSetKey).slice(tileWidth, tileHeight)) {
            for (tile in row) {
                simpleTileSets[nextTileSetIndex++] = tile
            }
        }
    }

    fun changeTile(
        tag: String,
        x: Int,
        y: Int,
        tileIndex: Int,
        matchesMaskedMark: Int = 0
    ): Boolean {
        layers[tag]?.let { layer ->
            when (layer) {
                is TiledLayer.Simple -> layer.tiles[x][y] = tileIndex
                is TiledLayer.Quarters -> layer.changeState(x, y, tileIndex > 0)
                is TiledLayer.DualGrid -> layer.changeState(x, y, tileIndex > 0)
                is TiledLayer.Alternating -> layer.changeState(x, y, tileIndex > 0)
            }
            return layer.mark and matchesMaskedMark != 0
        }
        return false
    }

    fun isTileFilled(tag: String, x: Int, y: Int): Boolean {
        layers[tag]?.let { layer ->
            if (layer.isFilled(x, y)) {
                return true
            }
        }
        return false
    }

    private fun TiledLayer.set(x: Int, y: Int, value: Int) {
        when (this) {
            is TiledLayer.Simple -> tiles[x][y] = value
            is TiledLayer.Quarters -> changeState(x, y, value > 0)
            is TiledLayer.DualGrid -> changeState(x, y, value > 0)
            is TiledLayer.Alternating -> changeState(x, y, value > 0)
        }
    }

    private fun TiledLayer.isFilled(x: Int, y: Int): Boolean =
        when (this) {
            is TiledLayer.Simple -> tiles[x][y] > 0
            is TiledLayer.Quarters -> state(x, y)
            is TiledLayer.DualGrid -> state(x, y)
            is TiledLayer.Alternating -> state(x, y)
        }

    fun isTileMarked(x: Int, y: Int, markMask: Int): Boolean {
        for (layer in layers.values) {
            if ((layer.mark and markMask) != 0) {
                if (layer.isFilled(x, y)) {
                    return true
                }
            }
        }
        return false
    }

    private val directions = listOf(
        1 to 0,
        0 to 1,
        -1 to 0,
        0 to -1,
    )

    fun moveCluster(from: String, to: String, x: Int, y: Int) {
        layers[from]?.let { fromLayer ->
            layers[to]?.let { toLayer ->
                val queueX = mutableListOf(x)
                val queueY = mutableListOf(y)
                while (queueX.isNotEmpty()) {
                    val x = queueX.removeFirst()
                    val y = queueY.removeFirst()
                    if (fromLayer.isFilled(x, y)) {
                        fromLayer.set(x, y, 0)
                        toLayer.set(x, y, 1)
                        // TODO: update physics? so far we are moving one vine into another, though
                        directions.forEach { (dx, dy) ->
                            val x = x + dx
                            val y = y + dy
                            queueX += x
                            queueY += y
                        }
                    }
                }
            }
        }
    }

    companion object {
        val emptySlices: List<TextureSlice> = emptyList()
    }

}