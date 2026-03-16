package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import com.littlekt.util.Scaler
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.gui.api.math.Vec2
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.UNITS_PER_PIXEL
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.StoryComponent
import net.mattemade.utils.msdf.MsdfFontRenderer

class AlternativeStoryRenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
) : IteratingSystem(family = family { any(StoryComponent) }) {

    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH,
        virtualHeight = WORLD_UNIT_HEIGHT
    )
    private val camera = viewport.camera.apply {
        position.set(HALF_WORLD_UNIT_WIDTH, HALF_WORLD_UNIT_HEIGHT, 0f)
    }
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val fontRenderer = MsdfFontRenderer(gameContext.assets.font.verdanaBoldMsdf)

    override fun onTick() {
        super.onTick()

        gameContext.canStartStory = true // hack to resume a bility to start story after the load is successfully completed
    }

    private var currentCharacter: Character = characters[speakerNarrator]!!
    private var parsedFor: String = ""
    private val parsedLines = mutableListOf<String>()

    override fun onTickEntity(entity: Entity) {
        if (gameContext.story == null) {
            return
        }

        val storyComponent = entity[StoryComponent]
        if (storyComponent.currentText.all{ it.isEmpty() }) {
            gameContext.story = null
            return
        }


        viewport.apply(context)
        batch.begin(camera.viewProjection)

        shapeRenderer.filledRectangle(
            0f,
            0f,
            width = WORLD_UNIT_WIDTH,
            height = WORLD_UNIT_HEIGHT,
            color = backgroundColor
        )

        gameContext.assets.textureFiles.apply {
            storyComponent.currentTags.forEach {
                characters[it]?.let {
                    currentCharacter = it
                }
            }
        }

        currentCharacter.portrait(gameContext).apply {
            val portraitWidth = width * UNITS_PER_PIXEL
            batch.draw(this, x = if (currentCharacter.onLeft) 0f else WORLD_UNIT_WIDTH - portraitWidth, y = 0f, width = portraitWidth, height = height * UNITS_PER_PIXEL)
        }
        currentCharacter.textbox(gameContext).apply {
            batch.draw(this, 0f, 0f, width = WORLD_UNIT_WIDTH, height = WORLD_UNIT_HEIGHT)
        }

        if (precalculatedPlacements.isEmpty()) {
            var optionOffsetY = 4f
            gameContext.assets.textureFiles.decisionBoxes.forEachIndexed { index, slice ->
                val boxWidth = slice.width * UNITS_PER_PIXEL
                val boxHeight = slice.height * UNITS_PER_PIXEL
                precalculatedPlacements += Rect(
                    x = (WORLD_UNIT_WIDTH - boxWidth)*0.5f,
                    y = optionOffsetY,
                    width = boxWidth,
                    height = boxHeight,
                )
                optionOffsetY += boxHeight
            }
        }

        if (storyComponent.options.size > 1) {
            storyComponent.options.forEachIndexed { index, text ->
                gameContext.assets.textureFiles.decisionBoxes[index].apply {
                    val placement = precalculatedPlacements[index]
                    batch.draw(
                        this,
                        x = placement.x,
                        y = placement.y,
                        width = placement.width,
                        height = placement.height,
                        colorBits = if (text.focused) batch.colorBits else unselectedTint
                    )
                }
            }
        }

        fontRenderer.drawAllTextAtOnce(batch) {
            if (parsedFor != storyComponent.currentText.first()) {
                parsedLines.clear()
                storyComponent.currentText.forEach {
                    parsedLines.addAll(softWrap(it, scale = fontScale, textWidthLimit))
                }
                parsedFor = storyComponent.currentText.first()
            }

            var offsetY = currentCharacter.textOffsetY
            parsedLines.forEach {
                measure(it, scale = fontScale, to = tempVec2); draw(it, horizontalTextOffset, WORLD_UNIT_HEIGHT - 2.5f + offsetY, fontScale, batch, tint = currentCharacter.textColor)
                offsetY += tempVec2.y
            }

            if (storyComponent.options.size > 1) {
                storyComponent.options.forEachIndexed { index, text ->
                    val placement = precalculatedPlacements[index]
                    measure(text.text, scale = fontScale, to = tempVec2)
                    draw(text.text, placement.cx - tempVec2.x * 0.5f, y = placement.cy - tempVec2.y * 0.5f + index * 0.4f, fontScale, batch, tint = leilanaTextColor)
                }
            }

        }
        batch.end()
    }



    companion object {
        private val precalculatedPlacements = mutableListOf<Rect>()
        private val unselectedTint = Color.DARK_GRAY.toFloatBits()
        private val fontScale = 0.4f
        private val horizontalTextOffset = 1.9f
        private val textWidthLimit = WORLD_UNIT_WIDTH - horizontalTextOffset * 2f
        private val tempVec2 = Vec2.borrow()
        private val backgroundColor = Color.BLACK.toFloatBits()
        private val leilanaTextColor = Color.fromHex("386A77").toFloatBits()
        private val speakerNarrator = "speaker: Narrator"

        private val characters = listOf(
            Character(
                speakerTag = "speaker: Leilana",
                portrait = { if (it.gameState.airPearl) {
                    it.assets.textureFiles.leilana3
                } else if (it.gameState.waterPearl) {
                    it.assets.textureFiles.leilana2
                } else {
                    it.assets.textureFiles.leilana1
                } },
                textbox = { it.assets.textureFiles.leilanaBox },
                onLeft = true,
                textColor = Color.fromHex("386A77").toFloatBits(),
                textOffsetY = 0f,
            ),
            Character(
                speakerTag = "speaker: DragonGuardian",
                portrait = { it.assets.textureFiles.dragon },
                textbox = { it.assets.textureFiles.dragonBox },
                onLeft = false,
                textColor = Color.fromHex("386A77").toFloatBits(),
                textOffsetY = 0f,
            ),
            Character(
                speakerTag = "speaker: Celestial Guardian",
                portrait = { it.assets.textureFiles.celestial },
                textbox = { it.assets.textureFiles.celestialBox },
                onLeft = false,
                textColor = Color.fromHex("386A77").toFloatBits(),
                textOffsetY = 0f,
            ),
            Character(
                speakerTag = "speaker: Dream Guardian",
                portrait = { it.assets.textureFiles.dream },
                textbox = { it.assets.textureFiles.dreamBox },
                onLeft = false,
                textColor = Color.fromHex("386A77").toFloatBits(),
                textOffsetY = 0f,
            ),
            Character(
                speakerTag = "speaker: Narrator",
                portrait = { it.assets.textureFiles.empty },
                textbox = { it.assets.textureFiles.empty },
                onLeft = true,
                textColor = Color.WHITE.toFloatBits(),
                textOffsetY = -6f,
            ),
        ).associateBy { it.speakerTag }
    }

    private class Character(
        val speakerTag: String,
        val portrait: (PlatformerGameContext) -> TextureSlice,
        val textbox: (PlatformerGameContext) -> TextureSlice,
        val onLeft: Boolean,
        val textColor: Float,
        val textOffsetY: Float,
    )
}