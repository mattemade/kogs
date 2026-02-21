package net.mattemade.platformer

import kotlin.math.roundToInt

val parameterOverride = mutableMapOf<String, String>()

val WORLD_NAME get() = parameterOverride["World file"] ?: "test-world.world"
val FIRST_LEVEL_NAME get() = parameterOverride["First level"] ?: "test-level1.tmj"
val SCREEN_RESOLUTION_WIDTH get() = parameterOverride["Screen width"]?.toIntOrNull() ?: 960
val SCREEN_RESOLUTION_HEIGHT get() = parameterOverride["Screen height"]?.toIntOrNull() ?: 540
val TILE_PIXEL_SIZE get() = parameterOverride["Tile size"]?.toIntOrNull() ?: 48

val WORLD_UNIT_WIDTH get() = SCREEN_RESOLUTION_WIDTH.toFloat() / TILE_PIXEL_SIZE
val WORLD_UNIT_HEIGHT get() = SCREEN_RESOLUTION_HEIGHT.toFloat() / TILE_PIXEL_SIZE
val PIXEL_PER_UNIT get() = TILE_PIXEL_SIZE//60
val WORLD_WIDTH get() = SCREEN_RESOLUTION_WIDTH//WORLD_UNIT_WIDTH * PIXEL_PER_UNIT
val WORLD_HEIGHT get() = SCREEN_RESOLUTION_HEIGHT//WORLD_UNIT_HEIGHT * PIXEL_PER_UNIT
val WORLD_WIDTH_FLOAT get() = WORLD_WIDTH.toFloat()
val WORLD_HEIGHT_FLOAT get() = WORLD_HEIGHT.toFloat()
val PIXEL_PER_UNIT_FLOAT get() = PIXEL_PER_UNIT.toFloat()

val HALF_WORLD_UNIT_WIDTH get() = WORLD_UNIT_WIDTH * 0.5f
val HALF_WORLD_UNIT_HEIGHT get() = WORLD_UNIT_HEIGHT * 0.5f

val UNITS_PER_PIXEL get() = 1 / PIXEL_PER_UNIT_FLOAT

val Float.px: Float get() = (this / UNITS_PER_PIXEL).roundToInt() * UNITS_PER_PIXEL

val WALK_VELOCITY get() = parameterOverride["Walk speed"]?.toFloat() ?: 8f
val JUMP_VELOCITY get() = parameterOverride["Jump speed"]?.toFloat() ?: 12f
val MAX_FALL_VELOCITY get() = parameterOverride["Max fall speed"]?.toFloat() ?: 30f
val GRAVITY_IN_JUMP get() = parameterOverride["Jump gravity scale"]?.toFloat() ?: 2f
val GRAVITY_IN_JUMPFALL get() = parameterOverride["Jumpfall gravity scale"]?.toFloat() ?: 10f
val GRAVITY_IN_FALL get() = parameterOverride["Normal gravity scale"]?.toFloat() ?: 10f

//parameterOverride["Screen"]?.toFloat()