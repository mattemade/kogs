package net.mattemade.bigmode.stuff

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.RESTAURANT_HEIGHT_REVERSE_RATIO
import net.mattemade.bigmode.resources.ResourceSprite
import net.mattemade.utils.Scheduler.Interpolator
import kotlin.math.sin

class Sprite(
    x: Float,
    y: Float,
    private val texture: TextureSlice,
    private val textureScale: Float = 1f,
    private val normalizedTextureOffsetX: Float = 0f,
    private val normalizedTextureOffsetY: Float = 0f,
    var flipX: Boolean = false
) {

    constructor(
        x: Float,
        y: Float,
        gameContext: BigmodeGameContext,
        name: String,
        textureScale: Float = 1f,
        flipX: Boolean = false,
        spriteDef: ResourceSprite = gameContext.assets.spriteDef(name)!!,
        textureSlice: TextureSlice = gameContext.assets.textureFiles.map[spriteDef.file]!!
    ) : this(
        x = x,
        y = y,
        texture = textureSlice,
        textureScale = textureScale,
        normalizedTextureOffsetX = spriteDef.anchorX,
        normalizedTextureOffsetY = spriteDef.anchorY,
        flipX = flipX,
    )

    val tint = Color.WHITE.toMutableColor()
    var angle: Float = 0f
    val position = MutableVec2f(x, y)
    val visiblePosition = MutableVec2f(x, y * RESTAURANT_HEIGHT_REVERSE_RATIO)
    private val travellingFrom = MutableVec2f()
    private val travellingOffset = MutableVec2f()
    val width = texture.width * textureScale
    val height = texture.height * textureScale
    private val offsetX = (if (flipX) normalizedTextureOffsetX - 1f else -normalizedTextureOffsetX) * width
    private val offsetY = -normalizedTextureOffsetY * height

    private val rotatingOffset = MutableVec2f()

    var bpmScaling = 1f
    private var blopXScaling = 1f
    private var blopYScaling = 1f
    private var bloppingYFor = 0f
    private var bloppingXFor = 0f
    private var travellingFor = 0f
    private var firstRenderOfTravelling = false

    fun update(dt: Float) {
        if (bloppingYFor > 0f) {
            bloppingYFor = maxOf(bloppingYFor - dt, 0f)
            val bloppingRatio = bloppingYFor / MAX_BLOP
            blopYScaling = 1f - bloppingRatio * sin(bloppingRatio * PI_F * 8f) * 0.1f
        }
        if (bloppingXFor > 0f) {
            bloppingXFor = maxOf(bloppingXFor - dt, 0f)
            val bloppingRatio = bloppingXFor / MAX_BLOP
            blopXScaling = 1f - bloppingRatio * sin(bloppingRatio * PI_F * 8f) * 0.1f
        }
        if (travellingFor > 0f) {
            travellingFor = maxOf(travellingFor - dt, 0f)
            val travellingRatio = Interpolator.Quadratic.map(travellingFor / MAX_TRAVEL_TIME)
            val length = travellingOffset.set(travellingFrom).subtract(visiblePosition).length()
            travellingOffset.setLength(length * travellingRatio)
        }
    }

    fun updateVisiblePosition() {
        visiblePosition.set(position).scale(1f, RESTAURANT_HEIGHT_REVERSE_RATIO)
    }

    fun render(batch: Batch) {
        if (firstRenderOfTravelling) { // just started travelling, so offset was not calculated correctly yet
            firstRenderOfTravelling = false
            update(0f)
        }
        val totalYScale = bpmScaling * blopYScaling
        val totalXScale = blopXScaling

        if (angle != 0f) {
            rotatingOffset.set(offsetX * totalXScale, offsetY * totalYScale)
            val rotation = angle.radians
            rotatingOffset.rotate(rotation)
            batch.draw(
                texture,
                x = visiblePosition.x + rotatingOffset.x + travellingOffset.x,
                y = visiblePosition.y + rotatingOffset.y + travellingOffset.y,
                width = width * totalXScale,
                height = height * totalYScale,
                flipX = flipX,
                rotation = rotation,
                colorBits = tint.toFloatBits(),
            )
        } else {
            batch.draw(
                texture,
                x = visiblePosition.x + offsetX * totalXScale + travellingOffset.x,
                y = visiblePosition.y + offsetY * totalYScale + travellingOffset.y,
                width = width * totalXScale,
                height = height * totalYScale,
                flipX = flipX,
                colorBits = tint.toFloatBits(),
            )
        }
    }

    fun blopX() {
        bloppingXFor = MAX_BLOP
    }

    fun blopY() {
        bloppingYFor = MAX_BLOP
    }

    fun travelFrom(previousVisiblePosition: Vec2f) {
        travellingFrom.set(previousVisiblePosition)
        //travellingFrom.set(1000f, 1000f)
        travellingFor = MAX_TRAVEL_TIME
        firstRenderOfTravelling = true
    }

    companion object {
        const val MAX_BLOP = 1f
        const val MAX_TRAVEL_TIME = 0.3f
    }
}