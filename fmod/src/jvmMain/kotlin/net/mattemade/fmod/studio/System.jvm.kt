package net.mattemade.fmod.studio

import java.nio.ByteBuffer
import org.lwjgl.fmod.FMODStudio
import org.lwjgl.fmod.FMOD_STUDIO_ADVANCEDSETTINGS

actual class System {

    private var system: Long? = null
    actual fun setAdvancedSettings(settings: StudioAdvancedSettings) {
        val system = system ?: return
        FMOD_STUDIO_ADVANCEDSETTINGS.create()
            .cbsize(settings.cbsize)
            .commandqueuesize(settings.commandQueueSize.toInt())
            .handleinitialsize(settings.handleInitialSize.toInt())
            .idlesampledatapoolsize(settings.idleSampleDataPoolSize)
            .streamingscheduledelay(settings.streamingScheduleDelay.toInt())
            .encryptionkey(ByteBuffer.wrap(settings.encryptionKey.toByteArray()))
            .apply { FMODStudio.FMOD_Studio_System_SetAdvancedSettings(system, this) }
            .free()
    }


    actual fun setAdvancedSettings(setup: StudioAdvancedSettingsDsl.() -> Unit) {
        val system = system ?: return
        val settings = FMOD_STUDIO_ADVANCEDSETTINGS.create()
        StudioAdvancedSettingsDslImpl(settings).setup()
        FMODStudio.FMOD_Studio_System_SetAdvancedSettings(system, settings)
        settings.free()
    }
}


class StudioAdvancedSettingsDslImpl(val settings: FMOD_STUDIO_ADVANCEDSETTINGS): StudioAdvancedSettingsDsl {
    override var cbsize: Int
        get() = settings.cbsize()
        set(value) { settings.cbsize(value) }
    override var commandQueueSize: UInt
        get() = settings.commandqueuesize().toUInt()
        set(value) { settings.commandqueuesize(value.toInt()) }
    override var handleInitialSize: UInt
        get() = settings.handleinitialsize().toUInt()
        set(value) { settings.handleinitialsize(value.toInt()) }
    override var studioUpdatePeriod: Int
        get() = settings.studioupdateperiod()
        set(value) { settings.studioupdateperiod(value) }
    override var idleSampleDataPoolSize: Int
        get() = settings.idlesampledatapoolsize()
        set(value) { settings.idlesampledatapoolsize(value) }
    override var streamingScheduleDelay: UInt
        get() = settings.streamingscheduledelay().toUInt()
        set(value) { settings.streamingscheduledelay(value.toInt()) }
    override var encryptionKey: String
        get() = ""
        set(value) { /*noop*/ }
}