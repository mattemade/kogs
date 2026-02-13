package net.mattemade.bigmode.scene

import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readAudioClipEx
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import com.littlekt.math.Rect
import net.mattemade.bigmode.BUTTON_SCALE
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.CUTSCENE_MUSIC_VOLUME
import net.mattemade.bigmode.HALF_WORLD_HEIGHT_FLOAT
import net.mattemade.bigmode.HALF_WORLD_WIDTH_FLOAT
import net.mattemade.bigmode.MUSIC_VOLUME
import net.mattemade.bigmode.WORLD_HEIGHT_FLOAT
import net.mattemade.bigmode.WORLD_WIDTH_FLOAT

class CutsceneScene(private val gameContext: BigmodeGameContext, private val level: Int) : Scene, InputProcessor {

    //private val okButton = gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Ok button")!!.file]!!
/*    private val buttonArea = Rect(
        x = WORLD_WIDTH_FLOAT - 40f,
        y = WORLD_HEIGHT_FLOAT - 40f,
        width = okButton.width * BUTTON_SCALE,
        height = okButton.height * BUTTON_SCALE
    )*/
    private var buttonHovered: Boolean = false
    private var buttonPressed: Boolean = false
    private var playing: Boolean = false

    override fun touchDown(
        screenX: Float,
        screenY: Float,
        pointer: Pointer
    ): Boolean {
        if (buttonHovered) {
            okSound?.play(volume = 0.2f)
            buttonPressed = true
        }
        return super.touchDown(screenX, screenY, pointer)
    }

    override fun touchDragged(
        screenX: Float,
        screenY: Float,
        movementX: Float,
        movementY: Float,
        pointer: Pointer
    ): Boolean {
        if (buttonHovered) {
            if (!buttonPressed) {
                if (!theEnd) {
                    okSound?.play(volume = 0.2f)
                    buttonPressed = true
                }
            }
        }
        return super.touchDragged(screenX, screenY, movementX, movementY, pointer)
    }

    override fun touchUp(
        screenX: Float,
        screenY: Float,
        pointer: Pointer
    ): Boolean {
        if (buttonHovered && !theEnd && gameContext.inputEnabled) {
            buttonClicked()
        }
        buttonPressed = false
        return super.touchUp(screenX, screenY, pointer)
    }
    // piano drums bass sax guitar
    private val textureNames = listOf(
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
        listOf(
            "CUTSCENE_BASS_1.png",
            "CUTSCENE_BASS_2.png",
            "CUTSCENE_BASS_3.png",
        ),
    ).getOrNull(level)

    private var textures: List<Texture>? = null
    private var currentTexture = 0
    private var theEnd = false

    init {
        gameContext.log("c${level}")
        if (textureNames == null) {
            gameContext.scheduler.schedule().then(1.1f).then { // just wait a bit to not soft-lock
                gameContext.openScene(Scene.Type.Restaurant(level))
            }
        } else {
            gameContext.input.addInputProcessor(this)
            gameContext.context.vfs.launch {
                if (okButton == null) {
                    val okButton = gameContext.context.vfs["texture/OK_button.png"].readTexture()
                    buttonArea.apply {
                        x = WORLD_WIDTH_FLOAT - 40f
                        y = WORLD_HEIGHT_FLOAT - 20f
                        width = okButton.width * BUTTON_SCALE
                        height = okButton.height * BUTTON_SCALE
                    }
                    CutsceneScene.okButton = okButton
                    okSound = gameContext.context.vfs["sound/click ok button.mp3"].readAudioClipEx()
                    music = gameContext.context.vfs["music/cutscene music.mp3"].readAudioClipEx()
                }
                textures = textureNames.map {
                    gameContext.context.vfs["texture/$it"].readTexture()
                }
                gameContext.scheduler.schedule().then { gameContext.sceneReady() }
            }
        }
    }

    private fun buttonClicked() {
        textures?.let {
            if (currentTexture >= it.size - 1) {
                gameContext.input.removeInputProcessor(this)
                gameContext.scheduler.schedule().then(0.5f).then { ratio ->
                    music?.setVolumeAll(CUTSCENE_MUSIC_VOLUME * (1f - ratio))
                }.then {
                    music?.stopAll()
                }
                gameContext.openScene(Scene.Type.Restaurant(level))
            } else {
                currentTexture++
                if (level == 5 && currentTexture == it.size - 1) {
                    theEnd = true
                }
            }
        }
    }

    override fun update(seconds: Float) {
        if (textures != null) {
            buttonHovered = buttonArea.contains(gameContext.cursorPositionInWorldCoordinates)
            if (!buttonHovered) {
                buttonPressed = false
            }
        }
        if (!playing) {
            if (level == 5) {
                gameContext.assets.music("Level 5")!!.play(MUSIC_VOLUME)
                playing = true
            } else {
                music?.let {
                    it.play(CUTSCENE_MUSIC_VOLUME, loop = true)
                    playing = true
                }
            }
        }
        gameContext.camera.position.set(HALF_WORLD_WIDTH_FLOAT, HALF_WORLD_HEIGHT_FLOAT, 0f)
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        textures?.getOrNull(currentTexture)?.let {
            batch.draw(it, x = 0f, y = 0f, width = WORLD_WIDTH_FLOAT, height = WORLD_HEIGHT_FLOAT)
            if (!theEnd) {
                okButton?.let {
                    batch.draw(
                        it,
                        x = buttonArea.x,
                        y = buttonArea.y,
                        width = buttonArea.width,
                        height = buttonArea.height,
                        colorBits = if (buttonPressed) pressColor else if (buttonHovered) hoverColor else normalColor
                    )
                }
            }
        }

    }

    companion object {
        private val normalColor = Color.WHITE.toFloatBits()
        private val hoverColor = Color.LIGHT_YELLOW.toFloatBits()
        private val pressColor = Color.LIGHT_GRAY.toFloatBits()
        private var okButton: Texture? = null
        private var okSound: AudioClipEx? = null
        var music: AudioClipEx? = null
        private val buttonArea = Rect()
    }
}