package net.mattemade.bigmode.scene

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer

interface Scene {
    fun update(seconds: Float)
    fun render(batch: Batch, shapeRenderer: ShapeRenderer)


    sealed interface Type {
        class Restaurant(val level: Int): Type
        class Cutscene(val level: Int): Type
    }
}