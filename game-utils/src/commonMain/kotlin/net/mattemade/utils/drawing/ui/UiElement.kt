package net.mattemade.utils.drawing.ui

import com.littlekt.graph.node.resource.HAlign
import com.littlekt.graph.node.resource.VAlign
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import net.mattemade.utils.drawing.SmartLines
import net.mattemade.wavefunctioncollapse.ui.UiContext

class UiElement(
    val uiContext: UiContext,
    val area: Rect,
    var focusableWithKeys: Boolean = false,
    var focusableWithMouse: Boolean = false,
    val actionOnRight: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionOnLeft: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionOnUp: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionOnDown: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionA: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionB: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val actionClick: ((uiElement: UiElement, repeat: Int, holding: Boolean) -> Unit)? = null,
    val onFocusChanged: ((uiElement: UiElement, Boolean) -> Unit)? = null,
    var text: SmartLines? = null,
    //val textColor: ((Int) -> Color) = { Color.WHITE },
    val textVAlign: VAlign = VAlign.CENTER,
    val textHAlign: HAlign = HAlign.CENTER,
    val customUpdate: ((UiElement, dtSeconds: Float) -> Unit)? = null,
    val customRender: ((UiElement, Batch, ShapeRenderer) -> Unit)? = null,
    var enabled: Boolean = true,
    var bgColor: Float = 0f,
    private val cx: Float = area.cx,
    private val cy: Float = area.cy,
    val tag: String? = null,
    val isGreenAction: Boolean? = null,
    val isButton: Boolean = true,
    val isPriceGraph: Boolean = false,
    //val render: (UiElement, Batch, ShapeRenderer) -> Unit,
) {

    //val gameContext = uiContext.gameContext
    private val textCenterY = cy + 2f
    var keyPressed: Boolean = false
    var cursorPressed: Boolean = false
    var hoverX: Float = -1f
    var hoverY: Float = -1f

    var isHighlighted: Boolean = false
        set(value) {
            field = value
            if (enabled && value) {
                //gameContext.playSound(gameContext.assets.sound.highlight)
            }
        }
    var isFocused: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    uiContext.currentlyFocused?.isFocused = false
                    uiContext.currentlyFocused = this
                } else {
                    keyPressed = false
                    cursorPressed = false
                    if (uiContext.currentlyFocused == this) {
                        uiContext.currentlyFocused = null
                    }
                }
                isHighlighted = value
                onFocusChanged?.invoke(this, value)
            }
        }


    fun focus() {
        isFocused = true
    }
/*
    private var textColor: Color = Color.WHITE
    private var frameColor: Color =
        if (isGreenAction == null) WHITE_ACTION else if (isGreenAction) GREEN_ACTION else RED_ACTION
    private var frameColorBits: Float =
        if (isGreenAction == null) WHITE_ACTION_BITS else if (isGreenAction) GREEN_ACTION_BITS else RED_ACTION_BITS
    private var pressedColor: Color =
        if (isGreenAction == null) WHITE_PRESSED else if (isGreenAction) GREEN_PRESSED else RED_PRESSED
    private var pressedColorBits: Float =
        if (isGreenAction == null) WHITE_PRESSED_BITS else if (isGreenAction) GREEN_PRESSED_BITS else RED_PRESSED_BITS
    private fun textColor(chatNumber: Int): Color = textColor

    fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (bgColor != 0f) {
            shapeRenderer.filledRectangle(area, color = bgColor)
        } else {
            if (isPriceGraph) {
                if (isFocused) {
                    shapeRenderer.rectangle(
                        x = area.x+1f,
                        y = area.y,
                        width = area.width,
                        height = area.height,
                        color = frameColorBits,
                    )
                }
            } else if (isButton) {
                if (isHighlighted) {
                    textColor = Color.BLACK
                    shapeRenderer.filledRectangle(
                        x = area.x,
                        y = area.y+1f,
                        width = area.width-1f,
                        height = area.height-1f,
                        color = if (cursorPressed || keyPressed || !enabled) {
                            pressedColorBits
                        } else {
                            frameColorBits
                        }
                    )
                } else {
                    textColor = if (cursorPressed || keyPressed || !enabled) {
                        pressedColor
                    } else {
                        frameColor
                    }
                    shapeRenderer.rectangle(
                        x = area.x + 1f,
                        y = area.y + 1f,
                        width = area.width - 2f,
                        height = area.height - 2f,
                        color = if (cursorPressed || keyPressed || !enabled) {
                            pressedColorBits
                        } else {
                            frameColorBits
                        }
                    )
                }
            }
        }

        text?.let {
            gameContext.assets.font.drawer.apply {
                drawText(
                    batch,
                    lines = it,
                    x = cx,
                    y = textCenterY,
                    vAlign = textVAlign,
                    hAlign = textHAlign,
                    color = ::textColor,
                )
            }
        }

        customRender?.invoke(this, batch, shapeRenderer)
    }*/

    companion object {
        private val focusedColor = Color.DARK_GRAY.toFloatBits()
        private val keyPressedColor = Color.DARK_GREEN.toFloatBits()
        private val cursorPressedColor = Color.DARK_BLUE.toFloatBits()
        private val disabledColor = Color.DARK_RED.toFloatBits()
    }
}