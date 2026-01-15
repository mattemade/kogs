package net.mattemade.gui.api.widgets

import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Rect
import net.mattemade.gui.api.math.Vec2
import net.mattemade.gui.api.memory.Pool.Companion.use
import kotlin.math.max

class GuiBox(renderer: GuiRenderer) : GuiWidgetContainer<GuiBox.Specs>(renderer) {

    override fun layout(within: Rect) {
        val unused = Vec2.borrow()
        widgets.forEach { entity ->
            entity.widget.measure(result = unused, within)
            entity.bounds.apply {
                entity.specs?.let { specs ->
                    set(
                        x = within.x + specs.x,
                        y = within.y + specs.y,
                        width = if (specs.width == 0f) unused.x else max(if (specs.width == -1f) within.width else specs.width, within.width - specs.x),
                        height = if (specs.height == 0f) unused.y else max(if (specs.height == -1f) within.height else specs.height, within.height - specs.y),
                    )
                } ?: run {
                    set(within)
                }
            }

        }
        Vec2.recycle(unused)
    }

    override fun measure(result: Vec2, within: Rect) {
        result.set(0f, 0f)
        widgets.forEach { entity ->
            Vec2.use {
                entity.widget.measure(this, within)
                // TODO: use the measured size somehow?

                result.x = max(result.x, entity.bounds.x2)
                result.y = max(result.y, entity.bounds.y2)
            }
        }
    }

    class Specs(
        val x: Float = 0f,
        val y: Float = 0f,
        val width: Float = 0f,
        val height: Float = 0f,
    )
}