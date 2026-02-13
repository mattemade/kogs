package net.mattemade.bigmode.stuff

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer

interface Being {

    val depth: Float
    fun update(dt: Float)
    fun render(batch: Batch, shapeRenderer: ShapeRenderer)
    fun renderShadow(shapeRenderer: ShapeRenderer) {}

}