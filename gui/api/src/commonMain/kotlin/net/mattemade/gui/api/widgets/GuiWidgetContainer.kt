package net.mattemade.gui.api.widgets

import net.mattemade.gui.api.GuiColor
import net.mattemade.gui.api.GuiInputEvent
import net.mattemade.gui.api.GuiRenderer
import net.mattemade.gui.api.math.Rect

abstract class GuiWidgetContainer<T>(val renderer: GuiRenderer) : GuiWidget() {

    protected val widgets = mutableListOf<Entity<T>>()
    internal var isLayoutCalculated = false
    private val lastArea = Rect.borrow()

    fun add(
        widget: GuiWidget,
        specs: T?
    ) {
        widgets += Entity(widget, specs, Rect.borrow())
        isLayoutCalculated = false
        widget.parent = this
    }

    fun remove(widget: GuiWidget) {
        widgets.removeAll { it.widget == widget }
        isLayoutCalculated = false
    }

    final override fun render(within: Rect) {
        layoutIfNeeded(within)
        renderContainer(within)
        renderWidgets()
    }

    override fun consume(event: GuiInputEvent): Boolean {
        for (entity in widgets) {
            if (entity.widget.consume(event)) {
                return true
            }
        }
        return false
    }

    private fun layoutIfNeeded(within: Rect) {
        if (isLayoutCalculated || lastArea == within) {
            return
        }
        layout(within)
        lastArea.set(within)
        isLayoutCalculated = true
    }

    private fun renderWidgets() {
        widgets.forEach { entity ->
            entity.widget.render(entity.bounds)
        }
    }

    protected abstract fun layout(within: Rect)
    open fun renderContainer(within: Rect) {
        renderer.drawRect(within.x, within.y, within.width, within.height, color = GuiColor.CONTAINER_BACKGROUND)
    }

    protected class Entity<T>(
        val widget: GuiWidget,
        val specs: T?,
        val bounds: Rect,
    )
}