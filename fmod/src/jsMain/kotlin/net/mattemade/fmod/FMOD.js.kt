package net.mattemade.fmod

import kotlinx.browser.window

internal val fmodJS = js("{}")

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
    FmodStudioSystem(getResult { fmodJS.Studio_System_Create(it) })

actual class FmodStudioSystem(private val actualSystem: dynamic) {
    actual val coreSystem: FmodStudioSystemCore
        get() = FmodStudioSystemCore(getResult { actualSystem.getCoreSystem(it) })

    actual fun initialize(
        maxChannels: Int,
        studioInitFlags: FmodStudioInitFlag,
        initFlags: FmodInitFlag,
        extraDriverData: Long?
    ) {
        actualSystem.initialize(maxChannels, studioInitFlags, initFlags, extraDriverData)
    }

    actual fun loadBankFile(
        file: String,
        studioLoadingBankType: FmodStudioLoadingType
    ): FmodBank =
        FmodBank(getResult { actualSystem.loadBankFile(file, studioLoadingBankType, it) })

    actual fun update() {
        actualSystem.update()
    }

    actual fun getEvent(eventName: String): FmodEventDescription =
        FmodEventDescription(getResult { actualSystem.getEvent(eventName, it) })
}

actual class FmodStudioSystemCore(private val actualCore: dynamic) {

    actual fun setDSPBufferSize(bufferLength: Int, numBuffers: Int) {
        actualCore.setDSPBufferSize(bufferLength, numBuffers)
    }

    actual fun getCPUUsage(cpu: FmodCpu) {
        actualCore.getCPUUsage(cpu.actualCpu)
    }

    actual fun getDriverInfo(
        id: Int
    ): FmodDriverInfo {
        val systemRate = js("{}")
        val spearkerMode = js("{}")
        val speakerModeChanels = js("{}")
        actualCore.getDriverInfo(id, null, null, systemRate, spearkerMode, speakerModeChanels)
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
        actualCore.setSoftwareFormat(sampleRate, speakerMode, numSpeakers)
    }
}

actual class FmodBank(private val actualBank: dynamic) {
    actual val loadingState: FmodStudioLoadingState
        get() = getResult { actualBank.getLoadingState(it) }
    actual val sampleLoadingState: FmodStudioLoadingState
        get() = getResult { actualBank.getSampleLoadingState(it) }

    actual fun loadSampleData() {
        actualBank.loadSampleData()
    }

    actual fun unloadSampleData() {
        actualBank.unloadSampleData()
    }
}

actual class FmodEventDescription(private val actualEventDescription: dynamic) {
    actual fun createInstance(): FmodEventInstance =
        FmodEventInstance(getResult { actualEventDescription.createInstance(it) })

    actual fun loadSampleData() {
        actualEventDescription.loadSampleData()
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription
    = FmodParameterDescription(getResult { actualEventDescription.getParameterDescriptionByName(name, it) })
}

actual class FmodParameterDescription(private val actualParameterDescription: dynamic) {
    actual val id: FmodParameterId = actualParameterDescription.id
}

actual class FmodEventInstance(private val actualEventInstance: dynamic) {
    actual fun start() {
        actualEventInstance.start()
    }

    actual fun stop(mode: FmodStudioStopType) {
        actualEventInstance.stop(mode)
    }

    actual fun release() {
        actualEventInstance.release()
    }

    actual fun setCallback(
        callback: FmodCallback,
        callbackMask: FmodCallbackType
    ) {
        actualEventInstance.setCallback(callback.externalCallback::invoke, callbackMask)
    }

    actual fun setParameterByID(
        id: FmodParameterId,
        value: Float,
        ignoreSeekSpeed: Int
    ) {
        actualEventInstance.setParameterByID(id, value, ignoreSeekSpeed)
    }
}

private inline fun getResult(crossinline block: (outval: dynamic) -> Unit): dynamic {
    val outval = js("{}")
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

actual class FmodParameterId
actual class FmodCallback actual constructor(val externalCallback: FmodCallbackExternal)
