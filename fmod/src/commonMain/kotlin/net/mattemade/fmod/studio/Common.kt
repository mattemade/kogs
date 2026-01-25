package net.mattemade.fmod.studio

enum class LoadingState {
    UNLOADING,
    UNLOADED,
    LOADING,
    LOADED,
    ERROR,
}

class ParameterDescription(
    val name: String,
    val id: ParameterId,
    val miiumum: Float,
    val maximum: Float,
    val defaultValue: Float,
    val type: ParameterType,
    val flags: ParameterFlags,
) {

}

class ParameterId(
    val data1: UInt,
    val data2: UInt,
)

enum class ParameterType {
    GAME_CONTROLLED,
    AUTOMATIC_DISTANCE,
    AUTOMATIC_EVENT_CONE_ANGLE,
    AUTOMATIC_EVENT_ORIENTATION,
    AUTOMATIC_DIRECTION,
    AUTOMATIC_ELEVATION,
    AUTOMATIC_LISTENER_ORIENTATION,
    AUTOMATIC_SPEED,
    AUTOMATIC_SPEED_ABSOLUTE,
    MAX
}

typealias ParameterFlags = Int

object ParameterFlag {
    const val READONLY: ParameterFlags = 0x00000001
    const val AUTOMATIC: ParameterFlags = 0x00000002
    const val GLOBAL: ParameterFlags = 0x00000004
    const val DISCRETE: ParameterFlags = 0x00000008
}