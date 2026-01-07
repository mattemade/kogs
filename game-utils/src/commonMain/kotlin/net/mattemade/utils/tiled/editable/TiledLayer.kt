package net.mattemade.utils.tiled.editable

import com.littlekt.graphics.g2d.TextureSlice

sealed class TiledLayer(val tag: String) {
    var mark: Int = 0
    abstract val state: (x: Int, y: Int) -> Boolean

    class Simple(val tiles: Array<IntArray>, tag: String) : TiledLayer(tag) {
        override val state: (x: Int, y: Int) -> Boolean = { x, y -> tiles[x][y] != 0}

        constructor(tag: String, width: Int, height: Int) :
                this(Array(width) { IntArray(height) { 0 } }, tag)
    }

    class Quarters(
        val tileSetKey: String,
        val onStateChange: (x: Int, y: Int, filled: Boolean) -> Unit,
        override val state: (x: Int, y: Int) -> Boolean,
        val layers: (x: Int, y: Int) -> List<TextureSlice>,
        tag: String,
    ) : TiledLayer(tag) {
        private var callback: ((x: Int, y: Int, state: Boolean) -> Unit)? = null

        fun changeState(x: Int, y: Int, state: Boolean) {
            onStateChange(x, y, state)
            callback?.invoke(x, y, state)
        }

        fun onSomethingChanged(callback: (x: Int, y: Int, state: Boolean) -> Unit) {
            this.callback = callback
        }
    }

    class DualGrid(
        val tileSetKey: String,
        val onStateChange: (x: Int, y: Int, filled: Boolean) -> Unit,
        override  val state: (x: Int, y: Int) -> Boolean,
        val tile: (x: Int, y: Int) -> TextureSlice?,
        val sliceType: Int,
        tag: String,
    ) : TiledLayer(tag) {
        private var callback: ((x: Int, y: Int, state: Boolean) -> Unit)? = null

        fun changeState(x: Int, y: Int, state: Boolean) {
            onStateChange(x, y, state)
            callback?.invoke(x, y, state)
        }

        fun onSomethingChanged(callback: (x: Int, y: Int, state: Boolean) -> Unit) {
            this.callback = callback
        }
    }

    class Alternating(
        val tileSetKey: String,
        val onStateChange: (x: Int, y: Int, filled: Boolean) -> Unit,
        override val state: (x: Int, y: Int) -> Boolean,
        val tile: (x: Int, y: Int) -> TextureSlice?,
        val tileWidth: Int,
        val tileHeight: Int,
        tag: String,
    ): TiledLayer(tag) {
        private var callback: ((x: Int, y: Int, state: Boolean) -> Unit)? = null

        fun changeState(x: Int, y: Int, state: Boolean) {
            onStateChange(x, y, state)
            callback?.invoke(x, y, state)
        }

        fun onSomethingChanged(callback: (x: Int, y: Int, state: Boolean) -> Unit) {
            this.callback = callback
        }
    }
}