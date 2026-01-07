package net.mattemade.utils.drawing.quartertiles

import com.littlekt.graphics.g2d.TextureSlice
import kotlin.random.Random

class QuarterTileSet(val slice: TextureSlice, val tileWidth: Int, val tileHeight: Int) {

    private val slices = slice.slice(tileWidth, tileHeight)
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

    private val tiles = mutableMapOf<Int, List<TextureSlice>>()

    fun tileLayers(spec: QuarterTileSpec): List<TextureSlice> =
        tiles.getOrPut(spec.spec) {
            // recalculate tile layers based on whatever
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