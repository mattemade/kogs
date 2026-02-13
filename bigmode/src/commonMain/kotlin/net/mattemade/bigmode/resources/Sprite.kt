package net.mattemade.bigmode.resources

import com.littlekt.graphics.g2d.Batch
import net.mattemade.bigmode.BigmodeGameContext

class Sprite(private val resourceSprite: ResourceSprite, private val gameContext: BigmodeGameContext) {

    private val widthFloat: Float
    private val heightFloat: Float
    private val frames = gameContext.assets.textureFiles.map[resourceSprite.file]!!.run {
        val frameWidth = width / resourceSprite.animationFrames
        widthFloat = frameWidth.toFloat()
        heightFloat = height.toFloat()
        slice(frameWidth, height)[0]
    }
    private val size = frames.size
    private var frameIndex = 0
    private var frameTime = 0f
    private var currentFrame = frames.first()

    fun update(seconds: Float) {
        frameTime += seconds
        if (frameTime >= resourceSprite.frameTime) {
            frameTime -= resourceSprite.frameTime
            frameIndex = (frameIndex + 1) % size
            currentFrame = frames[frameIndex]
        }
    }

    fun render(batch: Batch, x: Float, y: Float) {
        batch.draw(
            currentFrame,
            x = x - resourceSprite.anchorX,
            y = y - resourceSprite.anchorY,
        )
    }

    fun copy(): Sprite = Sprite(resourceSprite, gameContext)
}