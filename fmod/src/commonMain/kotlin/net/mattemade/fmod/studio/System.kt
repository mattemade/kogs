package net.mattemade.fmod.studio

class StudioAdvancedSettings(
    val cbsize: Int,
    val commandQueueSize: UInt,
    val handleInitialSize: UInt,
    val studioUpdatePeriod: Int,
    val idleSampleDataPoolSize: Int,
    val streamingScheduleDelay: UInt,
    val encryptionKey: String,
)

interface StudioAdvancedSettingsDsl {
    var cbsize: Int
    var commandQueueSize: UInt
    var handleInitialSize: UInt
    var studioUpdatePeriod: Int
    var idleSampleDataPoolSize: Int
    var streamingScheduleDelay: UInt
    var encryptionKey: String
}


expect class System {
    fun setAdvancedSettings(settings: StudioAdvancedSettings)
    fun setAdvancedSettings(setup: StudioAdvancedSettingsDsl.() -> Unit)

}