package net.mattemade.gametemplate.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import net.mattemade.gametemplate.TemplateGameContext

class TemplateScene(private val gameContext: TemplateGameContext): Scene {

    private val items = listOf(
        gameContext.assets.sprite("Left item")!!,
        gameContext.assets.sprite("Middle item")!!,
        gameContext.assets.sprite("Right item")!!,
    )

    private val clickSound = gameContext.assets.sound("Click")!!
    private val backgroundMusic = gameContext.assets.music("Intro")!!
    private var firstClick: Boolean = true

    override fun update(seconds: Float) {
        items.forEach {
            it.update(seconds)
        }

        with (gameContext.context.input) {
            if (justTouched) {
                clickSound.play()
                if (firstClick) {
                    firstClick = false
                    backgroundMusic.play()
                }
            }
        }
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        items.forEachIndexed { index, sprite ->
            sprite.render(batch, -90f + index * 90f, -50f)
        }
    }
}