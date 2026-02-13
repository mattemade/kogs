package net.mattemade.bigmode

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Texture
import com.littlekt.input.GameButton
import com.littlekt.input.Input
import com.littlekt.input.Key
import com.littlekt.math.MutableVec2f
import com.littlekt.math.isFuzzyZero
import net.mattemade.bigmode.scene.Scene
import net.mattemade.utils.Scheduler
import kotlin.random.Random

class BigmodeGameContext(
    val context: Context,
    private val sendLog: (String) -> Unit,
    val encodeUrlComponent: (String) -> String,
    val getFromUrl: (String) -> List<String>?,
    val overrideResourcesFrom: String?,
    val camera: Camera,
    val openScene: (Scene.Type) -> Unit,
    val sceneReady: () -> Unit,
) {

    var inputEnabled: Boolean = false
    val input = context.input
    lateinit var curtainTexture: Texture
    val assets = BigmodeAssets(context, this, getFromUrl, overrideResourcesFrom)
    val scheduler = Scheduler()
    var canvasZoom: Float = 1f
    val cursorPositionInWorldCoordinates = MutableVec2f(HALF_WORLD_WIDTH_FLOAT, HALF_WORLD_HEIGHT_FLOAT)
    var usingKeyPosition = false
    val stickPosition = MutableVec2f(0f, 0f)
    var buttonWasPressed: Boolean = false
    var mousePressed: Boolean = false
    var brakePressed: Boolean = false
    //var rocketPressed: Boolean = false

    private var tag =
        context.vfs.loadString("tag") ?: Random.nextInt().toString().also {
            context.vfs.store("tag", it)
        }
    private var run = Random.nextInt().toString()

    private val gpNormalButtons = listOf(
        GameButton.XBOX_A,
        GameButton.XBOX_B,
        GameButton.XBOX_X,
        GameButton.XBOX_Y,
    )

    private val gpShifts = listOf(
        GameButton.L1,
        GameButton.R1,
        GameButton.LEFT_TRIGGER,
        GameButton.RIGHT_TRIGGER,
    )

    fun update(dt: Float) {
        scheduler.update(dt)
        val ax = input.axisLeftX
        val ay = input.axisLeftY
        if (!ax.isFuzzyZero(eps = 0.05f) || !ay.isFuzzyZero(eps = 0.05f)) {
            stickPosition.set(ax, ay)
            usingKeyPosition = true
        } else {
            stickPosition.set(
                (if (input.isKeyPressed(Key.A)
                    || input.isKeyPressed(Key.ARROW_LEFT)
                    || input.isButtonPressedOnAnyGamepad(GameButton.LEFT)
                ) -1f else 0f) + (if (input.isKeyPressed(
                        Key.D
                    ) || input.isKeyPressed(Key.ARROW_RIGHT)
                    || input.isButtonPressedOnAnyGamepad(GameButton.RIGHT)
                ) 1f else 0f),
                (if (input.isKeyPressed(Key.S) || input.isKeyPressed(Key.ARROW_DOWN)
                    || input.isButtonPressedOnAnyGamepad(GameButton.DOWN)) 1f else 0f) + (if (input.isKeyPressed(
                        Key.W
                    ) || input.isKeyPressed(Key.ARROW_UP)
                    || input.isButtonPressedOnAnyGamepad(GameButton.UP)
                ) -1f else 0f),
            )
            if (stickPosition.length() > 0f) {
                stickPosition.setLength(1f)
                usingKeyPosition = true
            }
        }

        brakePressed = mousePressed || input.isKeyPressed(Key.SPACE) || input.isAnyGamepadButtonPressed(gpNormalButtons)
        /*rocketPressed =
            input.isKeyPressed(Key.SHIFT_LEFT) || input.isKeyPressed(Key.SHIFT_RIGHT) || input.isAnyGamepadButtonPressed(
                gpShifts
            )*/
    }


    private fun Input.isButtonPressedOnAnyGamepad(button: GameButton): Boolean =
        connectedGamepads.any { gamepad ->
            isGamepadButtonPressed(button, gamepad.index)
        }

    private fun Input.isAnyGamepadButtonPressed(buttons: List<GameButton>): Boolean =
        connectedGamepads.any { gamepad ->
            buttons.any {
                isGamepadButtonPressed(it, gamepad.index)
            }
        }

    fun log(log: String) {
        sendLog("$LOG_TAG|$tag|$run|$log")
    }

    companion object {
        private val LOG_TAG = "bm6"
    }
}