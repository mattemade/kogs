package net.mattemade.utils.drawing.ui

import net.mattemade.utils.drawing.DynamicSmartLines
import net.mattemade.utils.drawing.MonoSpaceTextDrawer

class DynamicText<T>(private val drawer: MonoSpaceTextDrawer, startValue: T, val rule: (T) -> String) {
    val lines = DynamicSmartLines(mutableListOf(rule(startValue)), drawer)
    var value: T = startValue
        set(value) {
            if (field != value) {
                field = value
                lines.lines[0] = rule(value)
            }
        }
}