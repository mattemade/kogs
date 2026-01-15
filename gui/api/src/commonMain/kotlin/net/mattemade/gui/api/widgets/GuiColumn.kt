package net.mattemade.gui.api.widgets

import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.math.Vec2
import net.mattemade.gui.api.memory.Pool.Companion.use
import kotlin.math.max

class GuiColumn(renderer: GuiRenderer) : GuiWidgetContainer<Unit>(renderer) {

    override fun layout(within: Rect) {
        var offsetY = 0f
        widgets.forEach { entity ->
            Vec2.use {
                entity.widget.measure(this, within)
                entity.bounds.set(
                    x = within.x,
                    y = within.y + offsetY,
                    width = within.width,
                    height = within.height,
                )
                offsetY += within.height
            }

        }
    }

    override fun measure(result: Vec2, within: Rect) {
        result.set(0f, 0f)
        widgets.forEach { entity ->
            result.x = max(result.x, entity.bounds.x2)
            result.y = max(result.y, entity.bounds.y2)
        }
    }
}