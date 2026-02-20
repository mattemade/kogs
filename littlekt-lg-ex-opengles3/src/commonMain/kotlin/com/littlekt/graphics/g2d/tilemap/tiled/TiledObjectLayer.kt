package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledObjectLayer(
    type: String,
    name: String,
    id: Int,
    visible: Boolean,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    parallaxFactor: Vec2f,
    properties: Map<String, TiledMap.Property>,
    val drawOrder: TiledMap.Object.DrawOrder?,
    val objects: List<TiledMap.Object>,
    private val tiles: Map<Int, TiledTileset.Tile>
) : TiledLayer(
    type,
    name,
    id,
    visible,
    width,
    height,
    offsetX,
    offsetY,
    tileWidth,
    tileHeight,
    tintColor,
    opacity,
    parallaxFactor,
    properties
) {

    private val flipData = TileData()

    val objectsById by lazy { objects.associateBy { it.id } }
    val objectsByName by lazy { objects.associateBy { it.name } }
    val objectsByType by lazy { objects.groupBy { it.type } }

    private val tempVec2f = MutableVec2f()

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float, displayObjects: Boolean) {
        if (!displayObjects || !visible) return

        objects.forEach { obj ->
            if (!obj.visible) return@forEach

            obj.gid?.let { gid ->
                val tileData = gid.toInt().bitsToTileData(flipData)
                // Tiled assumes the (0,0) is bottom-left, but we have it top-left, so need to compensate for that when rotating
                tempVec2f.set(0f, obj.bounds.height).rotate(obj.rotation)
                tiles[tileData.id]?.let {
                    batch.draw(
                        slice = it.slice,
                        x = (obj.x + offsetX + it.offsetX - tempVec2f.x) * scale + x,
                        y = (obj.y + offsetY + it.offsetY - tempVec2f.y) * scale + y,
                        originX = 0f,
                        originY = 0f,
                        width = obj.bounds.width * scale,
                        height = obj.bounds.height * scale,
                        scaleX = 1f,
                        scaleY = 1f,
                        rotation = obj.rotation,
                        flipX = tileData.flipX,
                        flipY = tileData.flipY,
                        colorBits = colorBits
                    )
                }
            }
        }
    }

    fun getById(id: Int): TiledMap.Object = objectsById[id] ?: error("Object: '$id' does not exist in this layer!")
    fun getByName(name: String): TiledMap.Object = objectsByName[name] ?: error("Object: '$name' does not exist in this layer!")
    fun getByType(type: String): List<TiledMap.Object> = objects.filter { it.type == type }

    operator fun get(name: String) = getByName(name)
    operator fun get(id: Int) = getById(id)
}