package net.mattemade.gui.api.widgets

import net.mattemade.gui.api.GuiColor
import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.math.Vec2

class GuiLabel(initialText: String, private val renderer: GuiRenderer) : GuiWidget() {

    var text: String = initialText
        set(value) {
            if (field != value) {
                field = value
                parent?.isLayoutCalculated = false
            }
        }

    override fun measure(result: Vec2, within: Rect) {
        renderer.measureText(text, result)
    }

    override fun render(within: Rect) {
        renderer.drawRect(within.x, within.y, within.width, within.height, GuiColor.WIDGET_BACKGROUND)
        renderer.drawText(text, within.x, within.y, GuiColor.ON_CONTAINER)
    }

}