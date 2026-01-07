package net.mattemade.wavefunctioncollapse.ui

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import net.mattemade.utils.drawing.ui.UiElement

class UiContext(val worldWidth: Float, val worldHeight: Float, val cursorPosition: Vec2f, val isMousePressed: () -> Boolean) {

    private val halfWorldWidth = worldWidth * 0.5f
    private val halfWorldHeight = worldHeight * 0.5f

    val elements = mutableListOf<UiElement>()
    val taggedElements = mutableMapOf<String, UiElement>()
    var currentlyFocused: UiElement? = null

    private var concurrentUpdate: Boolean = false
    private val elementsToAddAfterUpdate = mutableListOf<UiElement>()

    private val tempRect = Rect()
    private val focusDirection = MutableVec2f()
    private val currentlyFocusedCenter = MutableVec2f()
    fun focusNext(tryToGoFromOtherScreenSide: Boolean = false) {
        currentlyFocused?.let {
            tempRect.set(it.area/*.cx, it.area.cy, 0f, 0f*/)
            if (tryToGoFromOtherScreenSide) {
                tempRect.x -= focusDirection.x * worldWidth
                tempRect.y -= focusDirection.y * worldHeight
            }
            if (focusDirection.x != 0f) {
                if (focusDirection.x < 0f) {
                    tempRect.width = tempRect.x
                    tempRect.x = 0f
                } else {
                    tempRect.x = tempRect.x2
                    tempRect.width = worldWidth - tempRect.x
                }
            }
            if (focusDirection.y != 0f) {
                if (focusDirection.y < 0f) {
                    tempRect.height = tempRect.y
                    tempRect.y = 0f
                } else {
                    tempRect.y = tempRect.y2
                    tempRect.height = worldHeight - tempRect.y
                }
            }
            currentlyFocusedCenter.set(it.area.cx, it.area.cy)
            if (tryToGoFromOtherScreenSide) {
                currentlyFocusedCenter.subtract(
                    focusDirection.x * worldWidth,
                    focusDirection.y * worldHeight
                )
            }
        } ?: run {
            if (tryToGoFromOtherScreenSide) {
                return
            }
            tempRect.set(0f, 0f, worldWidth, worldHeight)
            if (focusDirection.x != 0f) {
                currentlyFocusedCenter.set(
                    if (currentlyFocusedCenter.x < 0f) worldWidth else 0f,
                    halfWorldHeight
                )

            } else if (focusDirection.y != 0f) {
                currentlyFocusedCenter.set(
                    halfWorldWidth,
                    if (currentlyFocusedCenter.y < 0f) worldHeight else 0f
                )
            }
        }

        /*        elements
                    .asSequence()
                    .filter { it.focusableWithKeys && it.area.intersects(tempRect) }
                    .minByOrNull { currentlyFocusedCenter.distance(it.area.cx, it.area.cy) }
                    ?.focus()
                    ?: run {
                        if (!tryToGoFromOtherScreenSide) {
                            focusNext(tryToGoFromOtherScreenSide = true)
                        }
                    }*/
        val filtered = elements.filter { it.focusableWithKeys && it.area.intersects(tempRect) }
        val sorted = filtered.sortedBy { currentlyFocusedCenter.distance(it.area.cx, it.area.cy) }
        sorted.firstOrNull()?.focus() /*?: run {
            if (!tryToGoFromOtherScreenSide) {
                focusNext(true)
            }
        }*/
    }

    private var cursorIsFocusedOn: UiElement? = null
    fun update(dtSeconds: Float) {
        /*gameContext.input.apply {
            if (pause.justPressed) {
                gameContext.switchScene(SceneConfig.PAUSE)
            }

            currentlyFocused.let {
                it.checkRepeating(left, it?.actionOnLeft, ::focusLeft)
                it.checkRepeating(right, it?.actionOnRight, ::focusRight)
                it.checkRepeating(up, it?.actionOnUp, ::focusUp)
                it.checkRepeating(down, it?.actionOnDown, ::focusDown)
                it.checkRepeating(actionA, it?.actionA, ::noop)
                it.checkRepeating(actionB, it?.actionB, ::noop)
            }

            if (moveCursor.pressed || tap.pressed || tap.justReleased) {
                cursorIsFocusedOn?.let {
                    it.hoverX = -1f
                    it.hoverY = -1f
                }
                val currentlyFocusedOn = elements.lastOrNull {
                    if (it.focusableWithMouse && it.area.contains(cursorPosition)) {
                        it.hoverX = cursorPosition.x - it.area.x
                        it.hoverY = cursorPosition.y - it.area.y
                        it.isFocused = true
                        true
                    } else {
                        false
                    }
                }

                if (tap.pressed) {
                    if (tap.justPressed) {
                        cursorIsFocusedOn = currentlyFocusedOn
                    }
                    if (cursorIsFocusedOn == currentlyFocusedOn) {
                        cursorIsFocusedOn?.cursorPressed = true
                        cursorIsFocusedOn.checkRepeating(tap, cursorIsFocusedOn?.actionClick, ::noop)
                    } else {
                        cursorIsFocusedOn?.cursorPressed = false
                    }
                } else {
                    if (tap.justReleased) {
                        cursorIsFocusedOn.checkRepeating(tap, cursorIsFocusedOn?.actionClick, ::noop)
                    }
                    cursorIsFocusedOn = currentlyFocusedOn
                    if (moveCursor.pressed && currentlyFocusedOn == null) {
                        currentlyFocused?.isFocused = false
                    }
                }
            }
        }*/

/*        if (isMousePressed()) {
            cursorIsFocusedOn?.let {
                it.hoverX = -1f
                it.hoverY = -1f
            }
            val currentlyFocusedOn = elements.lastOrNull {
                if (it.focusableWithMouse && it.area.contains(cursorPosition)) {
                    it.hoverX = cursorPosition.x - it.area.x
                    it.hoverY = cursorPosition.y - it.area.y
                    it.isFocused = true
                    true
                } else {
                    false
                }
            }

            if (isMousePressed()) {
                if (tap.justPressed) {
                    cursorIsFocusedOn = currentlyFocusedOn
                }
                if (cursorIsFocusedOn == currentlyFocusedOn) {
                    cursorIsFocusedOn?.cursorPressed = true
                    cursorIsFocusedOn.checkRepeating(tap, cursorIsFocusedOn?.actionClick, ::noop)
                } else {
                    cursorIsFocusedOn?.cursorPressed = false
                }
            } else {
                if (tap.justReleased) {
                    cursorIsFocusedOn.checkRepeating(tap, cursorIsFocusedOn?.actionClick, ::noop)
                }
                cursorIsFocusedOn = currentlyFocusedOn
                if (moveCursor.pressed && currentlyFocusedOn == null) {
                    currentlyFocused?.isFocused = false
                }
            }
        }*/

        concurrentUpdate = true
        elements.forEach {
            it.customUpdate?.invoke(it, dtSeconds)
        }
        concurrentUpdate = false
        if (elementsToAddAfterUpdate.isNotEmpty()) {
            addAll(elementsToAddAfterUpdate)
            elementsToAddAfterUpdate.clear()
        }
    }

    private fun focusLeft() {
        focusAt(-1f, 0f)
    }

    private fun focusRight() {
        focusAt(1f, 0f)
    }

    private fun focusUp() {
        focusAt(0f, 1f)
    }

    private fun focusDown() {
        focusAt(0f, -1f)
    }

    private fun noop() {}

    private fun focusAt(x: Float, y: Float) {
        focusDirection.set(x, y)
        focusNext()
    }

/*    private val repeatingInputs = mutableMapOf<GameInput.State, Int>()
    private inline fun UiElement?.checkRepeating(input: GameInput.State, noinline action: ((element: UiElement, repeat: Int, holding: Boolean) -> Unit)?, alternative: () -> Unit) {
        if (input.justPressed) {
            activateAction(action, 0, true, alternative)
            repeatingInputs[input] = 0
        } else if (input.pressed) {
            val repeats = (maxOf(0f, input.pressedFor - REPEAT_START_AT) / REPEAT_EVERY).ceilToInt()
            val was = repeatingInputs[input] ?: 0
            if (repeats > was) {
                activateAction(action, repeats, true, alternative)
                repeatingInputs[input] = repeats
            }
        } else if (input.justReleased) {
            activateAction(action, 0, false, alternative)
            repeatingInputs.remove(input)
        }
    }*/

    private var doNotPlayNextSound: Boolean = false
    fun ignoreNextPressSound() {
        doNotPlayNextSound = true
    }
    private inline fun UiElement?.activateAction(
        noinline action: ((UiElement, Int, Boolean) -> Unit)?,
        repeat: Int,
        holding: Boolean,
        alternative: () -> Unit
    ) {
        this?.let {
            if (action != null) {
                if (it.enabled || !holding) {
                    action.invoke(this, repeat, holding)
                }
                if (!doNotPlayNextSound && it.enabled && holding) {
                   // gameContext.playSound(gameContext.assets.sound.press, volume = if (repeat == 0) 1f else 0.25f)
                }
                doNotPlayNextSound = false
            } else if (holding) {
                alternative()
            }
        } ?: run {
            if (holding) {
                alternative()
            }
        }
    }

    fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        elements.forEach {
            //it.render(batch, shapeRenderer)
        }
    }


    fun addAll(elements: Collection<UiElement>) {
        if (concurrentUpdate) {
            elementsToAddAfterUpdate.addAll(elements)
            return
        }
        this.elements.addAll(elements)
        elements.forEach {
            if (it.tag != null) {
                taggedElements[it.tag] = it
            }
        }
    }
    fun addAll(vararg elements: UiElement) {
        if (concurrentUpdate) {
            elementsToAddAfterUpdate.addAll(elements)
            return
        }
        this.elements.addAll(elements)
        elements.forEach {
            if (it.tag != null) {
                taggedElements[it.tag] = it
            }
        }
    }


}