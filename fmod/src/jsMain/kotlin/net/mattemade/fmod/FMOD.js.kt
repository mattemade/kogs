package net.mattemade.fmod

import kotlinx.browser.window

internal val fmodJS = js("{}")
internal val outval = js("{}")

internal external fun FMODModule(instance: Any)

actual fun FMOD_Module_Create(preRun: () -> Unit, callback: () -> Unit) {
    fmodJS.window = window
    fmodJS.preRun = preRun
    fmodJS.onRuntimeInitialized = callback
    fmodJS.INITIAL_MEMORY = 128 * 1024 * 1024
    FMODModule(fmodJS)
}

actual fun FMOD_FS_createPreloadedFile(filename: String) {
    val virtualPath = StringBuilder("/")
    val lastPathDelimiter = filename.lastIndexOf('/')
    val virtualFileName = if (lastPathDelimiter > 0) {
        val folder = filename.substring(0, lastPathDelimiter)
        folder.splitToSequence('/').forEach {
            fmodJS.FS_createPath(virtualPath.toString(), it, true, true)
            virtualPath.append("$it/")
        }
        virtualPath.removeSuffix("/")

        filename.substring(lastPathDelimiter + 1)
    } else {
        filename
    }
    fmodJS.FS_createPreloadedFile(virtualPath.toString(), virtualFileName, filename, true, false)
}

actual fun FMOD_Studio_System_Create(): FmodStudioSystem =
    FmodStudioSystem(getResult { checkError(fmodJS.Studio_System_Create(it)) })

actual class FmodStudioSystem(private val actualSystem: dynamic) {
    actual val coreSystem: FmodStudioSystemCore
        get() = FmodStudioSystemCore(getResult { checkError(actualSystem.getCoreSystem(it)) })

    actual fun initialize(
        maxChannels: Int,
        studioInitFlags: FmodStudioInitFlag,
        initFlags: FmodInitFlag,
        extraDriverData: Long?
    ) {
        checkError(actualSystem.initialize(maxChannels, studioInitFlags, initFlags, extraDriverData))
    }

    actual fun loadBankFile(
        file: String,
        studioLoadingBankType: FmodStudioLoadingType
    ): FmodBank =
        FmodBank(getResult { checkError(actualSystem.loadBankFile(file, studioLoadingBankType, it)) })

    actual fun update() {
        checkError(actualSystem.update())
    }

    actual fun getEvent(eventName: String): FmodEventDescription =
        FmodEventDescription(getResult { checkError(actualSystem.getEvent(eventName, it)) })

    actual fun setListenerAttributes(listener: Int, attributes: Fmod3DAttributes, attenuationPosition: FmodVector?) {
        checkError(actualSystem.setListenerAttributes(listener, attributes.actual, attenuationPosition?.actual))
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        val outval = js("{}")
        checkError(actualSystem.getParameterDescriptionByName(name, outval))
        return FmodParameterDescription(outval.id)
    }

    actual fun setParameterByID(id: FmodParameterId, value: Float, ignoreSeekSpeed: Int) {
        checkError(actualSystem.setParameterByID(id.actualId, value, ignoreSeekSpeed))
    }

    actual fun setParameterByIDWithLabel(id: FmodParameterId, label: String, ignoreSeekSpeed: Int) {
        checkError(actualSystem.setParameterByIDWithLabel(id.actualId, label, ignoreSeekSpeed))
    }
}

actual class FmodStudioSystemCore(private val actualCore: dynamic) {

    actual fun setDSPBufferSize(bufferLength: Int, numBuffers: Int) {
        checkError(actualCore.setDSPBufferSize(bufferLength, numBuffers))
    }

    actual fun getCPUUsage(cpu: FmodCpu) {
        checkError(actualCore.getCPUUsage(cpu.actualCpu))
    }

    actual fun getDriverInfo(
        id: Int
    ): FmodDriverInfo {
        val systemRate = js("{}")
        val spearkerMode = js("{}")
        val speakerModeChanels = js("{}")
        checkError(actualCore.getDriverInfo(id, null, null, systemRate, spearkerMode, speakerModeChanels))
        return FmodDriverInfo(
            systemRate.`val`,
            spearkerMode.`val`,
            speakerModeChanels.`val`,
        )
    }

    actual fun setSoftwareFormat(
        sampleRate: Int,
        speakerMode: FmodSpeakerMode,
        numSpeakers: Int
    ) {
        checkError(actualCore.setSoftwareFormat(sampleRate, speakerMode, numSpeakers))
    }

    actual fun mixerSuspend() {
        checkError(actualCore.mixerSuspend())
    }

    actual fun mixerResume() {
        checkError(actualCore.mixerResume())
    }
}

actual class FmodBank(private val actualBank: dynamic) {
    actual val loadingState: FmodStudioLoadingState
        get() = getResult { checkError(actualBank.getLoadingState(it)) }
    actual val sampleLoadingState: FmodStudioLoadingState
        get() = getResult { checkError(actualBank.getSampleLoadingState(it)) }

    actual fun loadSampleData() {
        checkError(actualBank.loadSampleData())
    }

    actual fun unloadSampleData() {
        checkError(actualBank.unloadSampleData())
    }
}

actual class FmodEventDescription(private val actualEventDescription: dynamic) {
    actual fun createInstance(): FmodEventInstance =
        FmodEventInstance(getResult { checkError(actualEventDescription.createInstance(it)) })

    actual fun loadSampleData() {
        checkError(actualEventDescription.loadSampleData())
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        val outval = js("{}")
        checkError(actualEventDescription.getParameterDescriptionByName(name, outval))
        return FmodParameterDescription(outval.id)
    }
}

actual class FmodParameterDescription(private val actualParameterDescriptionId: dynamic) {
    actual val id: FmodParameterId = FmodParameterId(actualParameterDescriptionId)
}

actual class FmodEventInstance(private val actualEventInstance: dynamic) {
    actual fun start() {
        checkError(actualEventInstance.start())
    }

    actual fun stop(mode: FmodStudioStopType) {
        checkError(actualEventInstance.stop(mode))
    }

    actual fun release() {
        checkError(actualEventInstance.release())
    }

    actual fun setCallback(
        callback: FmodCallback,
        callbackMask: FmodCallbackType
    ) {
        checkError(actualEventInstance.setCallback(callback.externalCallback::invoke, callbackMask))
    }

    actual fun setParameterByID(
        id: FmodParameterId,
        value: Float,
        ignoreSeekSpeed: Int
    ) {
        checkError(actualEventInstance.setParameterByID(id.actualId, value, ignoreSeekSpeed))
    }

    actual fun setParameterByIDWithLabel(id: FmodParameterId, label: String, ignoreSeekSpeed: Int) {
        checkError(actualEventInstance.setParameterByIDWithLabel(id.actualId, label, ignoreSeekSpeed))
    }

    actual fun getPlaybackState(): FmodPlaybackState =
        getResult { checkError(actualEventInstance.getPlaybackState(it)) }

    actual fun set3DAttributes(attributes: Fmod3DAttributes) {
        checkError(actualEventInstance.set3DAttributes(attributes.actual))
    }
}

private inline fun getResult(crossinline block: (outval: dynamic) -> Unit): dynamic {
    block(outval)
    return outval.`val`
}

actual class FmodDriverInfo(
    actual val systemRate: Int,
    actual val speakerMode: FmodSpeakerMode,
    actual val speakerModeChannels: Int,
)

actual class FmodCpu {
    internal val actualCpu: dynamic = js("{}")
    actual val dsp: Float
        get() = actualCpu.cpu
    actual val stream: Float
        get() = actualCpu.stream
    actual val update: Float
        get() = actualCpu.update
}

private inline fun checkError(result: dynamic) {
    if (result != FMOD.OK) {
        println("FMOD ERROR: ${fmodJS.ErrorString(result)}")
    }
}

actual class FmodParameterId(val actualId: dynamic)
actual class FmodCallback actual constructor(val externalCallback: FmodCallbackExternal)
actual class Fmod3DAttributes {

    internal val actual: dynamic = fmodJS._3D_ATTRIBUTES()

    actual val position: FmodVector = FmodVector(actual.position)
    actual val velocity: FmodVector = FmodVector(actual.velocity)
    actual val forward: FmodVector = FmodVector(actual.forward)
    actual val up: FmodVector = FmodVector(actual.up)

    init {
        position.apply { x = 0f; y = 0f; z = 0f; }
        velocity.apply { x = 0f; y = 0f; z = 0f; }
        forward.apply { x = 0f; y = 0f; z = 0f; }
        up.apply { x = 0f; y = 0f; z = 0f; }
    }
}

actual class FmodVector(internal val actual: dynamic) {

    actual constructor() : this(fmodJS.VECTOR()) {
        x = 0f
        y = 0f
        z = 0f
    }

    actual var x: Float
        get() = actual.x
        set(value) {
            actual.x = value
        }
    actual var y: Float
        get() = actual.y
        set(value) {
            actual.y = value
        }
    actual var z: Float
        get() = actual.z
        set(value) {
            actual.z = value
        }
}