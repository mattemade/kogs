package com.littlekt.util

import com.littlekt.util.internal.clamp
import java.util.*

/**
 * @author Colton Daily
 * @date 12/8/2021
 */
actual fun Double.toString(precision: Int): String = String.format(Locale.ENGLISH, "%.${precision.clamp(0, 12)}f", this)
