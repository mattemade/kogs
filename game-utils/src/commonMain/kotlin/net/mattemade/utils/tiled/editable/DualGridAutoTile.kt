package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice

class DualGridAutoTile(
    tileSet: TextureSlice,
    val width: Int,
    val height: Int,
    sliceType: Int,
) {

    private val indecies: Array<Array<Int>> = if (sliceType == 0) maiIndecies else standardIndecies
    private val tilesPerWidth = if (sliceType == 0) 20 else 4
    private val tilesPerHeight = if (sliceType == 0) 1 else 4
    private val tiles: Array<Array<DualGridTileSpec>> =
        Array(width) { Array(height) { DualGridTileSpec() } }
    private val tilesCache = mutableMapOf<Int, Array<TextureSlice>>().apply {
        this[0] = cachedEmptyArray
    }

    private val slices =
        tileSet.slice(tileSet.width / tilesPerWidth, tileSet.height / tilesPerHeight).flatten()

    private val textureVariations =
        Array(2) { topLeft ->
            Array(2) { bottomLeft -> // top right and bottom left are switched by purpose!!! otherwise it doesn't work
                Array(2) { topRight ->
                    Array(2) { bottomRight ->
                        val topLeft = topLeft == 1
                        val topRight = topRight == 1
                        val bottomLeft = bottomLeft == 1
                        val bottomRight = bottomRight == 1
                        val slicesIndex = if (topLeft && topRight && bottomLeft && bottomRight) {
                            0
                        } else if (topLeft && topRight && bottomLeft) {
                            1
                        } else if (topLeft && topRight && bottomRight) {
                            2
                        } else if (topLeft && bottomLeft && bottomRight) {
                            3
                        } else if (topRight && bottomLeft && bottomRight) {
                            4
                        } else if (topLeft && topRight) {
                            5
                        } else if (topLeft && bottomLeft) {
                            6
                        } else if (topLeft && bottomRight) {
                            7
                        } else if (topRight && bottomLeft) {
                            8
                        } else if (topRight && bottomRight) {
                            9
                        } else if (bottomLeft && bottomRight) {
                            10
                        } else if (topLeft) {
                            11
                        } else if (topRight) {
                            12
                        } else if (bottomLeft) {
                            13
                        } else if (bottomRight) {
                            14
                        } else {
                            15
                        }
                        val result = indecies[slicesIndex].map(slices::get)
                        result
                    }
                }
            }
        }

    fun onChange(x: Int, y: Int, state: Boolean) {
        tiles[x][y].change { this[0][0] = state }

        for ((dX, dY) in affectingDirections) {
            val affectX = x + dX
            val affectY = y + dY
            if (affectX >= 0 && affectX < width && affectY >= 0 && affectY < height) {
                tiles[affectX][affectY].change { this[0 - dX][0 - dY] = state }
            }
        }
    }

    fun currentState(x: Int, y: Int): Boolean = tiles[x][y].isSolid

    fun tile(x: Int, y: Int): TextureSlice? =
        tiles[x][y].let { spec ->
            tilesCache.getOrPut(spec.spec) {
                val solids = spec.solids
                textureVariations[solids[0][0].i][solids[0][1].i][solids[1][0].i][solids[1][1].i]
            }.let {
                if (it.isNotEmpty()) it[(x + y) % it.size] else null
            }
        }

    private val Boolean.i: Int get() = if (this) 1 else 0

    private class DualGridTileSpec {

        // 2x2 array representing solid tiles, with this tile in top left corner
        val solids: Array<Array<Boolean>> = Array(2) { Array(2) { false } }

        var isSolid = false
        var spec: Int = 0

        fun change(block: Array<Array<Boolean>>.() -> Unit) {
            solids.block()
            isSolid = solids[0][0]
            spec = 0
            for (rows in solids) {
                for (item in rows) {
                    spec = spec shl 1
                    if (item) {
                        spec += 1
                    }
                }
            }
        }
    }

    private inline fun <reified T> Array<Int>.map(crossinline block: (Int) -> T): Array<T> =
        Array(this.size) { block(this[it]) }

    companion object {
        private val affectingDirections = listOf(
            0 to -1,
            -1 to 0,
            -1 to -1,
        )

        private val cachedEmptyArray = arrayOf<TextureSlice>()

        val maiIndecies = arrayOf(
            arrayOf(7, 8, 13, 14),
            arrayOf(4),
            arrayOf(5),
            arrayOf(10),
            arrayOf(11),
            arrayOf(17, 18),
            arrayOf(9, 15),
            arrayOf(),
            arrayOf(),
            arrayOf(6, 12),
            arrayOf(1, 1, 1, 1, 1, 2),
            arrayOf(19),
            arrayOf(16),
            arrayOf(3),
            arrayOf(0),
            arrayOf(),
        )

        val standardIndecies = arrayOf(
            arrayOf(6),
            arrayOf(7),
            arrayOf(10),
            arrayOf(2),
            arrayOf(5),
            arrayOf(9),
            arrayOf(11),
            arrayOf(4),
            arrayOf(14),
            arrayOf(1),
            arrayOf(3),
            arrayOf(15),
            arrayOf(8),
            arrayOf(0),
            arrayOf(13),
            arrayOf(12),
        )
    }
}