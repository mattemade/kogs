package net.mattemade.gametemplate.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer

interface Scene {
    fun update(seconds: Float)
    fun render(batch: Batch, shapeRenderer: ShapeRenderer)
}