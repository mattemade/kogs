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
}