package net.mattemade.gametemplate.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import net.mattemade.gametemplate.TemplateGameContext
import net.mattemade.gametemplate.WORLD_HEIGHT_FLOAT
import net.mattemade.gametemplate.WORLD_WIDTH_FLOAT
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.widgets.GuiBox
import net.mattemade.gui.api.widgets.GuiLabel
import net.mattemade.utils.GuiRenderer

class TemplateScene(private val gameContext: TemplateGameContext): Scene {

    private val items = listOf(
        gameContext.assets.sprite("Left item")!!,
        gameContext.assets.sprite("Middle item")!!,
        gameContext.assets.sprite("Right item")!!,
    )

    private val clickSound = gameContext.assets.sound("Click")!!
    private val backgroundMusic = gameContext.assets.music("Intro")!!
    private var firstClick: Boolean = true

    private val guiRect = Rect.borrow().set(0f, 0f, WORLD_WIDTH_FLOAT, WORLD_HEIGHT_FLOAT)
    private var gui: GuiBox? = null
    private var guiRenderer: GuiRenderer? = null


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
        if (guiRenderer == null) {
            val renderer = GuiRenderer(batch, shapeRenderer, gameContext.assets.font.fredokaMedium128)
            guiRenderer = renderer
            gui = GuiBox(renderer).apply {
                add(GuiLabel("Hello, world!", renderer), GuiBox.Specs())
                add(GuiLabel("Second label", renderer), GuiBox.Specs(10f, 10f))
            }
        }

        items.forEachIndexed { index, sprite ->
            sprite.render(batch, -90f + index * 90f, -50f)
        }
        gui?.render(guiRect)
    }
}