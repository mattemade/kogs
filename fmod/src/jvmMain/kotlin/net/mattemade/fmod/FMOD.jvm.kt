package net.mattemade.fmod

import java.nio.IntBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.fmod.FMOD
import org.lwjgl.fmod.FMOD.FMOD_VERSION
import org.lwjgl.fmod.FMODStudio
import org.lwjgl.fmod.FMOD_CPU_USAGE
import org.lwjgl.fmod.FMOD_STUDIO_EVENT_CALLBACK
import org.lwjgl.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION
import org.lwjgl.fmod.FMOD_STUDIO_PARAMETER_ID

private val outvalBuffer = PointerBuffer.allocateDirect(1)
private val intBuffer = BufferUtils.createIntBuffer(1)

actual fun FMOD_FS_createPreloadedFile(filename: String) {
    // no-op, only needed for JS target
}

actual fun FMOD_Studio_System_Create(): FmodStudioSystem {
    FMODStudio.FMOD_Studio_System_Create(outvalBuffer.clear(), FMOD_VERSION)
    return FmodStudioSystem(id = outvalBuffer.get())
}

actual class FmodStudioSystem(val id: Long) {
    actual val coreSystem: FmodStudioSystemCore
        get() = FMODStudio.FMOD_Studio_System_GetCoreSystem(id, outvalBuffer.clear()).run {
            FmodStudioSystemCore(outvalBuffer.get())
        }

    actual fun initialize(
        maxChannels: Int,
        studioInitFlags: FmodStudioInitFlag,
        initFlags: FmodInitFlag,
        extraDriverData: Long?
    ) {
        FMODStudio.FMOD_Studio_System_Initialize(id, maxChannels, studioInitFlags, initFlags, extraDriverData ?: 0L)
    }

    actual fun loadBankFile(
        file: String,
        studioLoadingBankType: FmodStudioLoadingType
    ): FmodBank {
        val result = FMODStudio.FMOD_Studio_System_LoadBankFile(id, file, studioLoadingBankType, outvalBuffer.clear())
        if (result != net.mattemade.fmod.FMOD.OK) {
            println("FMOD ERROR: ${FMOD.FMOD_ErrorString(result)}")
        }
        return FmodBank(id = outvalBuffer.get())
    }

    actual fun update() {
        FMODStudio.FMOD_Studio_System_Update(id)
    }

    actual fun getEvent(eventName: String): FmodEventDescription {
        FMODStudio.FMOD_Studio_System_GetEvent(id, eventName, outvalBuffer.clear()).also {
            if (it != FMOD.FMOD_OK) {
                println("FMOD ERROR: ${FMOD.FMOD_ErrorString(it)}")
            }
        }
        return FmodEventDescription(id = outvalBuffer.get())
    }
}

actual class FmodStudioSystemCore(val id: Long) {
    actual fun setDSPBufferSize(bufferLength: Int, numBuffers: Int) {
        FMOD.FMOD_System_SetDSPBufferSize(id, bufferLength, numBuffers)
    }

    actual fun getCPUUsage(cpu: FmodCpu) {
        FMOD.FMOD_System_GetCPUUsage(id, cpu.usage)
    }

    actual fun getDriverInfo(
        id: Int
    ): FmodDriverInfo {
        val systemRate = IntBuffer.allocate(1)
        val speakerMode = IntBuffer.allocate(1)
        val speakerModeChannels = IntBuffer.allocate(1)
        FMOD.FMOD_System_GetDriverInfo(
            this@FmodStudioSystemCore.id,
            id,
            null,
            null,
            systemRate,
            speakerMode,
            speakerModeChannels
        )
        return FmodDriverInfo(systemRate.get(), speakerMode.get(), speakerModeChannels.get())
    }

    actual fun setSoftwareFormat(
        sampleRate: Int,
        speakerMode: FmodSpeakerMode,
        numSpeakers: Int
    ) {
        FMOD.FMOD_System_SetSoftwareFormat(id, sampleRate, speakerMode, numSpeakers)
    }
}

actual class FmodBank(val id: Long) {
    actual val loadingState: FmodStudioLoadingState
        get() = FMODStudio.FMOD_Studio_Bank_GetLoadingState(id, intBuffer.clear()).run {
            if (this != net.mattemade.fmod.FMOD.OK) {
                println("FMOD ERROR: ${FMOD.FMOD_ErrorString(this)}")
            }
            intBuffer.get()
        }
    actual val sampleLoadingState: FmodStudioLoadingState
        get() = FMODStudio.FMOD_Studio_Bank_GetSampleLoadingState(id, intBuffer.clear()).run { intBuffer.get() }

    actual fun loadSampleData() {
        FMODStudio.FMOD_Studio_Bank_LoadSampleData(id)
    }

    actual fun unloadSampleData() {
        FMODStudio.FMOD_Studio_Bank_UnloadSampleData(id)
    }
}

actual class FmodEventDescription(val id: Long) {
    actual fun createInstance(): FmodEventInstance {
        FMODStudio.FMOD_Studio_EventDescription_CreateInstance(id, outvalBuffer.clear())
        return FmodEventInstance(outvalBuffer.get())
    }

    actual fun loadSampleData() {
        FMODStudio.FMOD_Studio_EventDescription_LoadSampleData(id)
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        val result = FmodParameterDescription()
        FMODStudio.FMOD_Studio_EventDescription_GetParameterDescriptionByName(id, name, result.description)
        return result
    }
}

actual class FmodParameterDescription {

    internal val description = FMOD_STUDIO_PARAMETER_DESCRIPTION.create()

    actual val id: FmodParameterId by lazy {
        FmodParameterId(description.id())
    }
}

actual class FmodEventInstance(val id: Long) {
    actual fun start() {
        FMODStudio.FMOD_Studio_EventInstance_Start(id)
    }

    actual fun stop(mode: FmodStudioStopType) {
        FMODStudio.FMOD_Studio_EventInstance_Stop(id, mode)
    }

    actual fun release() {
        FMODStudio.FMOD_Studio_EventInstance_Release(id)
    }

    actual fun setCallback(
        callback: FmodCallback,
        callbackMask: FmodCallbackType
    ) {
        FMODStudio.FMOD_Studio_EventInstance_SetCallback(id, callback.realCallback, callbackMask)
    }

    actual fun setParameterByID(
        id: FmodParameterId,
        value: Float,
        something: Boolean
    ) {
    }
}

actual class FmodDriverInfo(
    actual val systemRate: Int,
    actual val speakerMode: FmodSpeakerMode,
    actual val speakerModeChannels: Int
)

actual class FmodCpu {

    private val pointerBuffer = PointerBuffer.allocateDirect(FMOD_CPU_USAGE.SIZEOF)
    private val byteBuffer = pointerBuffer.getByteBuffer(FMOD_CPU_USAGE.SIZEOF)
    internal val usage: FMOD_CPU_USAGE = FMOD_CPU_USAGE(byteBuffer)

    actual val dsp: Float
        get() = usage.dsp()
    actual val stream: Float
        get() = usage.`stream$`()
    actual val update: Float
        get() = usage.update()
}

actual class FmodParameterId(val id: FMOD_STUDIO_PARAMETER_ID)

actual class FmodCallback actual constructor(externalCallback: FmodCallbackExternal) {
    internal val realCallback = FMOD_STUDIO_EVENT_CALLBACK.create { type, event, parameters ->
        externalCallback.invoke(type, event, parameters)
    }
}