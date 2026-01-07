package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice

class QuarterAutoTile(tileSet: TextureSlice, val width: Int, val height: Int, val cachedEmptyList: List<TextureSlice>) {

    private val tiles: Array<Array<QuarterAutoTileSpec>> = Array(width) { Array(height) { QuarterAutoTileSpec() } }
    private val tilesCache = mutableMapOf<Int, List<TextureSlice>>().apply {
        this[0] = cachedEmptyList
    }

    private val slices = tileSet.slice(tileSet.width / 3, tileSet.height / 4)
    private val solidBg = slices[3][1]
    private val innerWall = listOf(slices[0][1].allRotations, slices[2][1].allRotations)
    private val innerCornerWall = listOf(slices[0][0].allRotations, slices[2][0].allRotations)
    private val innerCornerPiece = listOf(slices[1][0].allRotations, slices[3][0].allRotations)
    private val innerDeadEnd = listOf(slices[0][2].allRotations, slices[2][2].allRotations)
    private val innerClose = listOf(slices[1][2], slices[3][2])


    private val TextureSlice.allRotations
        get() = listOf(
            this,
            TextureSlice(this).apply { rotated = true },
            TextureSlice(this).apply {
                flipH()
                flipV()
            },
            TextureSlice(this).apply {
                flipH()
                flipV()
                rotated = true
            },
        )

    fun onChange(x: Int, y: Int, state: Boolean) {
        tiles[x][y].change { this[1][1] = state }

        for ((dX, dY) in affectingDirections) {
            val affectX = x + dX
            val affectY = y + dY
            if (affectX >= 0 && affectX < width && affectY >= 0 && affectY < height) {
                tiles[affectX][affectY].change { this[1 - dX][1 - dY] = state }
            }
        }
    }

    fun currentState(x: Int, y: Int): Boolean = tiles[x][y].isSolid


    fun tileLayers(x: Int, y: Int): List<TextureSlice> =
        tiles[x][y].let { spec ->
            tilesCache.getOrPut(spec.spec) {
                // recalculate tile layers based on its configuration and placement
                val result = mutableListOf<TextureSlice>()
                val pieceIndex = if (spec.isSolid) 1 else 0
                val innerWall = innerWall[pieceIndex]
                val innerCornerWall = innerCornerWall[pieceIndex]
                val innerCornerPiece = innerCornerPiece[pieceIndex]
                val innerDeadEnd = innerDeadEnd[pieceIndex]
                val innerClose = innerClose[pieceIndex]
                if (spec.isSolid) {
                    result.add(solidBg)
                }
                // inner walls, first check 4, 3 and 2 adjacent wall combinations
                if (spec.isLeftEdge && spec.isTopEdge && spec.isRightEdge && spec.isBottomEdge) {
                    result.add(innerClose)
                } else if (spec.isLeftEdge && spec.isTopEdge && spec.isRightEdge) {
                    result.add(innerDeadEnd[3])
                } else if (spec.isLeftEdge && spec.isTopEdge && spec.isBottomEdge) {
                    result.add(innerDeadEnd[2])
                } else if (spec.isLeftEdge && spec.isRightEdge && spec.isBottomEdge) {
                    result.add(innerDeadEnd[1])
                } else if (spec.isTopEdge && spec.isRightEdge && spec.isBottomEdge) {
                    result.add(innerDeadEnd[0])
                } else if (spec.isLeftEdge && spec.isTopEdge) {
                    result.add(innerCornerWall[0])
                } else if (spec.isTopEdge && spec.isRightEdge) {
                    result.add(innerCornerWall[1])
                } else if (spec.isRightEdge && spec.isBottomEdge) {
                    result.add(innerCornerWall[2])
                } else if (spec.isLeftEdge && spec.isBottomEdge) {
                    result.add(innerCornerWall[3])
                } else {
                    // only 1 or 2 non-adjacent walls left, they may form a corridor when combined
                    if (spec.isLeftEdge) {
                        result.add(innerWall[3])
                    }
                    if (spec.isTopEdge) {
                        result.add(innerWall[0])
                    }
                    if (spec.isRightEdge) {
                        result.add(innerWall[1])
                    }
                    if (spec.isBottomEdge) {
                        result.add(innerWall[2])
                    }
                }

                // regardless of the walls, add correct corner pieces (they all may combine)
                if (spec.solids[0][0] != spec.isSolid && !spec.isLeftEdge && !spec.isTopEdge) {
                    result.add(innerCornerPiece[0])
                }
                if (spec.solids[2][0] != spec.isSolid && !spec.isTopEdge && !spec.isRightEdge) {
                    result.add(innerCornerPiece[1])
                }
                if (spec.solids[2][2] != spec.isSolid && !spec.isRightEdge && !spec.isBottomEdge) {
                    result.add(innerCornerPiece[2])
                }
                if (spec.solids[0][2] != spec.isSolid && !spec.isBottomEdge && !spec.isLeftEdge) {
                    result.add(innerCornerPiece[3])
                }
                result
            }
        }


    private class QuarterAutoTileSpec {

        // 3x3 array representing solid tiles, with this tile in the center
        val solids: Array<Array<Boolean>> = Array(3) { Array(3) { false } }

        var isSolid: Boolean = false
        var isLeftEdge: Boolean = false
        var isRightEdge: Boolean = false
        var isTopEdge: Boolean = false
        var isBottomEdge: Boolean = false

        // binary representation of solids array, that can be directly translated to a tile
        var spec: Int = 0

        fun change(block: Array<Array<Boolean>>.() -> Unit) {
            solids.block()
            if (solids[1][1]) {
                isSolid = true
                isLeftEdge = !solids[0][1]
                isRightEdge = !solids[2][1]
                isTopEdge = !solids[1][0]
                isBottomEdge = !solids[1][2]
            } else {
                isSolid = false
                isLeftEdge = solids[0][1]
                isRightEdge = solids[2][1]
                isTopEdge = solids[1][0]
                isBottomEdge = solids[1][2]
            }
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

    companion object {
        private val affectingDirections = listOf(
            -1 to -1,
            -1 to 0,
            -1 to 1,
            0 to -1,
            0 to 1,
            1 to -1,
            1 to 0,
            1 to 1,
        )
    }
}