package net.mattemade.utils.drawing.quartertiles

class QuarterTileLayer(val width: Int, val height: Int) {

    val tiles: Array<Array<QuarterTileSpec>> = Array(width) { Array(height) { QuarterTileSpec() } }
    val solids: Array<Array<Boolean>> = Array(width) { Array(height) { false } }
    val solidChars: ByteArray = ByteArray((width+1)*height) { if (it % (width+1) == width) '\n'.code.toByte() else '0'.code.toByte() }

    fun change(x: Int, y: Int, toSolid: Boolean) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            solids[x][y] = toSolid
            solidChars[x + y * (width + 1)] = if (toSolid) '1'.code.toByte() else '0'.code.toByte()
            tiles[x][y].change { this[1][1] = toSolid }
        }
        for ((dX, dY) in affectingDirections) {
            val affectX = x + dX
            val affectY = y + dY
            if (affectX >= 0 && affectX < width && affectY >= 0 && affectY < height) {
                tiles[affectX][affectY].change { this[1 - dX][1 - dY] = toSolid }
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