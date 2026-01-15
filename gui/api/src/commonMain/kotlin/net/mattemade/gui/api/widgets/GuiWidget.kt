package net.mattemade.gui.api.widgets

import net.mattemade.gui.api.GuiInputEvent
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.math.Vec2

abstract class GuiWidget {

    var parent: GuiWidgetContainer<*>? = null
        set(value) {
            if (field != value) {
                field?.remove(this)
                field = value
            }
        }

    abstract fun measure(result: Vec2, within: Rect)
    abstract fun render(within: Rect)
    open fun consume(event: GuiInputEvent): Boolean = false


}