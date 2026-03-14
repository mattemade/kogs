package net.mattemade.fmod

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.fmod.FMOD
import org.lwjgl.fmod.FMOD.FMOD_VERSION
import org.lwjgl.fmod.FMODStudio
import org.lwjgl.fmod.FMOD_3D_ATTRIBUTES
import org.lwjgl.fmod.FMOD_CPU_USAGE
import org.lwjgl.fmod.FMOD_STUDIO_EVENT_CALLBACK
import org.lwjgl.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION
import org.lwjgl.fmod.FMOD_STUDIO_PARAMETER_ID
import org.lwjgl.fmod.FMOD_VECTOR

private val outvalBuffer = PointerBuffer.allocateDirect(1)
private val intBuffer = BufferUtils.createIntBuffer(1)

actual fun FMOD_FS_createPreloadedFile(filename: String) {
    // no-op, only needed for JS target
}

actual fun FMOD_Module_Create(preRun: () -> Unit, callback: () -> Unit) {
    // no-op, only needed for JS target
    preRun()
    callback()
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
        FMODStudio.FMOD_Studio_System_Initialize(id, maxChannels, studioInitFlags, initFlags, extraDriverData ?: 0L).checkSuccess()
    }

    actual fun loadBankFile(
        file: String,
        studioLoadingBankType: FmodStudioLoadingType
    ): FmodBank {
        FMODStudio.FMOD_Studio_System_LoadBankFile(id, file, studioLoadingBankType, outvalBuffer.clear()).checkSuccess()
        return FmodBank(id = outvalBuffer.get())
    }

    actual fun update() {
        FMODStudio.FMOD_Studio_System_Update(id).checkSuccess()
    }

    actual fun getEvent(eventName: String): FmodEventDescription? {
        if (FMODStudio.FMOD_Studio_System_GetEvent(id, eventName, outvalBuffer.clear()).checkSuccess()) {
            return FmodEventDescription(id = outvalBuffer.get())
        }
        return null
    }

    actual fun setListenerAttributes(listener: Int, attributes: Fmod3DAttributes, attenuationPosition: FmodVector?) {
        FMODStudio.FMOD_Studio_System_SetListenerAttributes(
            id,
            listener,
            attributes.actual,
            attenuationPosition?.actual
        ).checkSuccess()
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        val result = FmodParameterDescription()
        FMODStudio.FMOD_Studio_System_GetParameterDescriptionByName(id, name, result.description).checkSuccess()
        return result
    }

    actual fun setParameterByID(id: FmodParameterId, value: Float, ignoreSeekSpeed: Int) {
        FMODStudio.FMOD_Studio_System_SetParameterByID(this.id, id.id, value, ignoreSeekSpeed).checkSuccess()
    }

    actual fun setParameterByIDWithLabel(id: FmodParameterId, label: String, ignoreSeekSpeed: Int) {
        FMODStudio.FMOD_Studio_System_SetParameterByIDWithLabel(this.id, id.id, label, ignoreSeekSpeed)
            .checkSuccess()
    }
}

actual class FmodStudioSystemCore(val id: Long) {
    actual fun setDSPBufferSize(bufferLength: Int, numBuffers: Int) {
        FMOD.FMOD_System_SetDSPBufferSize(id, bufferLength, numBuffers).checkSuccess()
    }

    actual fun getCPUUsage(cpu: FmodCpu) {
        FMOD.FMOD_System_GetCPUUsage(id, cpu.usage).checkSuccess()
    }

    actual fun getDriverInfo(
        id: Int
    ): FmodDriverInfo {
        val systemRate = BufferUtils.createIntBuffer(1)
        val speakerMode = BufferUtils.createIntBuffer(1)
        val speakerModeChannels = BufferUtils.createIntBuffer(1)
        FMOD.FMOD_System_GetDriverInfo(
            this@FmodStudioSystemCore.id,
            id,
            null,
            null,
            systemRate,
            speakerMode,
            speakerModeChannels
        ).checkSuccess()
        return FmodDriverInfo(systemRate.get(), speakerMode.get(), speakerModeChannels.get())
    }

    actual fun setSoftwareFormat(
        sampleRate: Int,
        speakerMode: FmodSpeakerMode,
        numSpeakers: Int
    ) {
        FMOD.FMOD_System_SetSoftwareFormat(id, sampleRate, speakerMode, numSpeakers).checkSuccess()
    }

    actual fun mixerSuspend() {
        FMOD.FMOD_System_MixerSuspend(id).checkSuccess()
    }

    actual fun mixerResume() {
        FMOD.FMOD_System_MixerResume(id).checkSuccess()
    }
}

actual class FmodBank(val id: Long) {
    actual val loadingState: FmodStudioLoadingState
        get() = FMODStudio.FMOD_Studio_Bank_GetLoadingState(id, intBuffer.clear()).run {
            checkSuccess()
            intBuffer.get()
        }
    actual val sampleLoadingState: FmodStudioLoadingState
        get() = FMODStudio.FMOD_Studio_Bank_GetSampleLoadingState(id, intBuffer.clear()).run {
            checkSuccess()
            intBuffer.get()
        }

    actual fun loadSampleData() {
        FMODStudio.FMOD_Studio_Bank_LoadSampleData(id).checkSuccess()
    }

    actual fun unloadSampleData() {
        FMODStudio.FMOD_Studio_Bank_UnloadSampleData(id).checkSuccess()
    }
}

actual class FmodEventDescription(val id: Long) {
    actual fun createInstance(): FmodEventInstance {
        FMODStudio.FMOD_Studio_EventDescription_CreateInstance(id, outvalBuffer.clear()).checkSuccess()
        return FmodEventInstance(outvalBuffer.get())
    }

    actual fun loadSampleData() {
        FMODStudio.FMOD_Studio_EventDescription_LoadSampleData(id).checkSuccess()
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        val result = FmodParameterDescription()
        FMODStudio.FMOD_Studio_EventDescription_GetParameterDescriptionByName(id, name, result.description).checkSuccess()
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
        FMODStudio.FMOD_Studio_EventInstance_Start(id).checkSuccess()
    }

    actual fun stop(mode: FmodStudioStopType) {
        FMODStudio.FMOD_Studio_EventInstance_Stop(id, mode).checkSuccess()
    }

    actual fun release() {
        FMODStudio.FMOD_Studio_EventInstance_Release(id).checkSuccess()
    }

    actual fun setCallback(
        callback: FmodCallback,
        callbackMask: FmodCallbackType
    ) {
        FMODStudio.FMOD_Studio_EventInstance_SetCallback(id, callback.realCallback, callbackMask).checkSuccess()
    }

    actual fun setParameterByID(
        id: FmodParameterId,
        value: Float,
        ignoreSeekSpeed: Int
    ) {
        FMODStudio.FMOD_Studio_EventInstance_SetParameterByID(this.id, id.id, value, ignoreSeekSpeed).checkSuccess()
    }

    actual fun setParameterByIDWithLabel(id: FmodParameterId, label: String, ignoreSeekSpeed: Int) {
        FMODStudio.FMOD_Studio_EventInstance_SetParameterByIDWithLabel(this.id, id.id, label, ignoreSeekSpeed)
            .checkSuccess()
    }


    actual fun getPlaybackState(): FmodPlaybackState {
        FMODStudio.FMOD_Studio_EventInstance_GetPlaybackState(this.id, intBuffer.clear()).checkSuccess()
        return intBuffer.get()
    }

    actual fun set3DAttributes(attributes: Fmod3DAttributes) {
        FMODStudio.FMOD_Studio_EventInstance_Set3DAttributes(id, attributes.actual).checkSuccess()
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

private inline fun Int.checkSuccess(): Boolean {
    if (this != net.mattemade.fmod.FMOD.OK) {
        println("FMOD ERROR: ${FMOD.FMOD_ErrorString(this)}")
        return false
    }
    return true
}

actual class Fmod3DAttributes {
    internal val actual: FMOD_3D_ATTRIBUTES = FMOD_3D_ATTRIBUTES.create()

    actual val position: FmodVector = FmodVector(actual.`position$`())
    actual val velocity: FmodVector = FmodVector(actual.velocity())
    actual val forward: FmodVector = FmodVector(actual.forward())
    actual val up: FmodVector = FmodVector(actual.up())
}

actual class FmodVector(internal val actual: FMOD_VECTOR) {

    actual constructor() : this(FMOD_VECTOR.create())

    actual var x: Float
        get() = actual.x()
        set(value) {
            actual.x(value)
        }
    actual var y: Float
        get() = actual.y()
        set(value) {
            actual.y(value)
        }
    actual var z: Float
        get() = actual.z()
        set(value) {
            actual.z(value)
        }
}