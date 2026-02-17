package net.mattemade.platformer.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import net.mattemade.utils.releasing.Releasing

interface Scene: Releasing {
    fun update(seconds: Float)
    fun render(batch: Batch, shapeRenderer: ShapeRenderer)
}