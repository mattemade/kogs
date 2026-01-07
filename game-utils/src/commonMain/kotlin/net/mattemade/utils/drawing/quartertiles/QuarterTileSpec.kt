package net.mattemade.utils.drawing.quartertiles

class QuarterTileSpec {

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