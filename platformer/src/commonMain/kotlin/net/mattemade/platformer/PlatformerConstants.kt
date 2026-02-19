package net.mattemade.platformer

import com.littlekt.math.floor

const val WORLD_UNIT_WIDTH = 32
const val WORLD_UNIT_HEIGHT = 18
const val PIXEL_PER_UNIT = 30
const val WORLD_WIDTH = WORLD_UNIT_WIDTH * PIXEL_PER_UNIT
const val WORLD_HEIGHT = WORLD_UNIT_HEIGHT * PIXEL_PER_UNIT
const val WORLD_WIDTH_FLOAT = WORLD_WIDTH.toFloat()
const val WORLD_HEIGHT_FLOAT = WORLD_HEIGHT.toFloat()
const val PIXEL_PER_UNIT_FLOAT = PIXEL_PER_UNIT.toFloat()

const val HALF_WORLD_UNIT_WIDTH = WORLD_UNIT_WIDTH * 0.5f
const val HALF_WORLD_UNIT_HEIGHT = WORLD_UNIT_HEIGHT * 0.5f

const val UNITS_PER_PIXEL = 1 / PIXEL_PER_UNIT_FLOAT

val Float.px: Float get() = (this / UNITS_PER_PIXEL).floor() * UNITS_PER_PIXEL