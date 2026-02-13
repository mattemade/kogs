package net.mattemade.bigmode.stuff

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.RESTAURANT_HEIGHT
import net.mattemade.bigmode.RESTAURANT_TOP_BORDER
import net.mattemade.bigmode.TEXTURE_SCALE
import kotlin.random.Random

class Thrown(
    val gameContext: BigmodeGameContext,
    x: Float,
    y: Float,
    val hazard: Boolean,
    val velocity: Vec2f,
    val leftBorder: Float,
    val rightBorder: Float,
    overrideOrder: String? = null,
    overrideHeight: Float = 0f,
    val onLand: (Thrown) -> Unit
) : Being {

    val sprite = Sprite(x, y, gameContext, overrideOrder ?: if (hazard) gameContext.assets.resourceSheet.getRandomHazard() else "Coin", TEXTURE_SCALE)
    val position = sprite.position
    val visiblePosition = sprite.visiblePosition
    var height = if (overrideHeight > 0f) overrideHeight else 10f
    var verticalVelocity = 10f
    var time = 0f
    var rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 10f
    var angle: Float = 0f
    var landed: Boolean = false

    override val depth: Float
        get() = position.y

    override fun update(dt: Float) {
        time += dt
        if (!landed) {
            verticalVelocity -= 10f * dt
            height = height + verticalVelocity * dt
            angle = time * rotationSpeed
        }

        if (height < 0f) {
            landed = true
            angle = 0f
            onLand(this)
            height = 0f
            sprite.angle = 0f
            sprite.updateVisiblePosition()
        } else if (height > 0f) {
            sprite.angle = angle
            sprite.position.add(velocity.x * dt, velocity.y * dt)
            sprite.position.x = sprite.position.x.clamp(leftBorder, rightBorder)
            sprite.position.y = sprite.position.y.clamp(RESTAURANT_TOP_BORDER, RESTAURANT_HEIGHT)
            sprite.updateVisiblePosition()
            sprite.visiblePosition.y -= height
        }
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        sprite.render(batch)
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        /*shapeRenderer.filledEllipse(
            x = visiblePosition.x,
            y = visiblePosition.y + height,
            rx = radius.x,
            ry = radius.y,
            innerColor = color,
            outerColor = color
        )*/
    }

    companion object {
        private val gravity = 10f
        private val radius = Vec2f(2.5f, 1.25f)
        private val color = Color.RED.toMutableColor().apply { a = 0.5f }.toFloatBits()
    }
}