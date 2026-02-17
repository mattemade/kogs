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

expect class System {
    fun setAdvancedSettings(settings: StudioAdvancedSettings)
}