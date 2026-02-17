package net.mattemade.platformer.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WORLD_HEIGHT_FLOAT
import net.mattemade.platformer.WORLD_WIDTH_FLOAT
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.widgets.GuiBox
import net.mattemade.gui.api.widgets.GuiLabel
import net.mattemade.utils.GuiRenderer
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self

class TemplateScene(private val gameContext: PlatformerGameContext): Scene, Releasing by Self() {

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
            val renderer = GuiRenderer(shapeRenderer, gameContext.assets.font.fredokaMsdf)
            guiRenderer = renderer
            gui = GuiBox(renderer).apply {
                add(GuiLabel("Hello, world!", renderer), GuiBox.Specs())
                add(GuiLabel("Second label", renderer), GuiBox.Specs(10f, 10f))
                add(GuiLabel("Optional challenges a.k.a. PTSD moments (it’s not necessary to include any of them):\n" +
                        "Put a checkpoint or healing station that has a trap (which can damage or give nasty debuffs to the player)\n" +
                        "Put an enemy character that can capture the player (and send the player to prison, where the player has to retrieve their equipments and escape)\n" +
                        "Put a boss with an ability to self destruct upon getting defeated (which can damage and/or kill unsuspecting player).\n", renderer), GuiBox.Specs(-160f, -90f))
            }
        }

        items.forEachIndexed { index, sprite ->
            sprite.render(batch, -90f + index * 90f, -50f)
        }
        gui?.render(guiRect)
        guiRenderer?.flushText(batch)
    }
}