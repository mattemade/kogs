package net.mattemade.bigmode.stuff

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.POWERUP_SCALE
import net.mattemade.bigmode.TEXTURE_SCALE
import net.mattemade.bigmode.resources.PowerUpSpec
import kotlin.math.sin

class PowerUp(val gameContext: BigmodeGameContext, val spec: PowerUpSpec, x: Float, y: Float, val velocity: Vec2f): Being {

    val sprite = Sprite(x, y, gameContext, spec.sprite, POWERUP_SCALE)
    val position = sprite.position
    val visiblePosition = sprite.visiblePosition
    var height = 0f
    var time = 0f

    override val depth: Float
        get() = position.y

    override fun update(dt: Float) {
        time += dt
        sprite.position.add(velocity.x * dt, velocity.y * dt)
        sprite.updateVisiblePosition()
        height = -2.5f + sin(time*5f) * 2.5f
        sprite.visiblePosition.y += height
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        sprite.render(batch)
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        //shapeRenderer.filledEllipse(x = visiblePosition.x, y = visiblePosition.y - height, rx = radius.x, ry = radius.y, innerColor = color, outerColor = color)
    }

    companion object {
        private val radius = Vec2f(5f, 2.5f)
        private val color = Color.WHITE.toMutableColor().apply { a = 0.5f }.toFloatBits()
    }
}