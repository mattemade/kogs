package com.littlekt.graphics

import com.littlekt.Releasable

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor actual constructor(
    pixmap: Pixmap,
    xHotspot: Int,
    yHotSpot: Int
) : Releasable {
    actual val pixmap: Pixmap = Pixmap(0, 0)
    actual val xHotspot: Int = 0
    actual val yHotSpot: Int = 0
    actual override fun release() = Unit
}