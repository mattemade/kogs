package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice

class AlternatingAutoTile(
    tileSet: TextureSlice,
    tileWidth: Int,
    tileHeight: Int,
    val width: Int,
    val height: Int,
) {

    private val tiles = tileSet.slice(tileWidth, tileHeight)

    private val state: Array<Array<Boolean>> =
        Array(width) { Array(height) { false } }


    fun onChange(x: Int, y: Int, value: Boolean) {
        state[x][y] = value
    }

    fun currentState(x: Int, y: Int): Boolean = state[x][y]

    fun tile(x: Int, y: Int): TextureSlice? =
        if (state[x][y]) tiles[y % tiles.size].let { it[x % it.size] } else null
}