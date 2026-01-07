package net.mattemade.utils.drawing

interface SmartLines: List<String> {
    val drawingWidth: List<Float>
    val drawingHeight: List<Float>
    val textBoxWidth: Float
    val textBoxHeight: Float
}

open class StaticSmartLines(val lines: List<String>, monoSpaceTextDrawer: MonoSpaceTextDrawer): SmartLines, List<String> by lines {
    override val drawingWidth = lines.map { monoSpaceTextDrawer.drawingWidth(it) }
    override val drawingHeight = lines.map { monoSpaceTextDrawer.drawingHeight(it) }
    override val textBoxWidth = drawingWidth.max()
    override val textBoxHeight = drawingHeight.sum()
}

class DynamicSmartLines(val lines: MutableList<String>, val monoSpaceTextDrawer: MonoSpaceTextDrawer): SmartLines, List<String> by lines {
    override val drawingWidth get() = lines.map { monoSpaceTextDrawer.drawingWidth(it) }
    override val drawingHeight get() = lines.map { monoSpaceTextDrawer.drawingHeight(it) }
    override val textBoxWidth get() = drawingWidth.max()
    override val textBoxHeight get() = drawingHeight.sum()
}