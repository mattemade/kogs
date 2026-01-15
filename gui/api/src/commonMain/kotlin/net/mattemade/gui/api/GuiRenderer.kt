package net.mattemade.gui.api

import net.mattemade.gui.api.math.Vec2

interface GuiRenderer {
    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: GuiColor)
    fun drawText(text: String, x: Float, y: Float, color: GuiColor)
    fun measureText(text: String, into: Vec2)
}