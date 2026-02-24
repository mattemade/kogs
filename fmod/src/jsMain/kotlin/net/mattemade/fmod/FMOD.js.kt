package net.mattemade.fmod

actual fun FMOD_FS_createPreloadedFile(filename: String) {
}

actual fun FMOD_Studio_System_Create(): FmodStudioSystem {
    TODO("Not yet implemented")
}

actual class FmodStudioSystem {
    actual val coreSystem: FmodStudioSystemCore
        get() = TODO("Not yet implemented")

    actual fun initialize(
        maxChannels: Int,
        studioInitFlags: FmodStudioInitFlag,
        initFlags: FmodInitFlag,
        extraDriverData: Long?
    ) {
    }

    actual fun loadBankFile(
        file: String,
        studioLoadingBankType: FmodStudioLoadingType
    ): FmodBank {
        TODO("Not yet implemented")
    }

    actual fun update() {
    }

    actual fun getEvent(eventName: String): FmodEventDescription {
        TODO("Not yet implemented")
    }
}

actual class FmodStudioSystemCore {
    actual fun setDSPBufferSize(bufferLength: Int, numBuffers: Int) {
    }

    actual fun getCPUUsage(cpu: FmodCpu) {
    }

    actual fun getDriverInfo(
        id: Int
    ): FmodDriverInfo {
        TODO("Not yet implemented")
    }

    actual fun setSoftwareFormat(
        sampleRate: Int,
        speakerMode: FmodSpeakerMode,
        numSpeakers: Int
    ) {
    }
}

actual class FmodBank {
    actual val loadingState: FmodStudioLoadingState
        get() = TODO("Not yet implemented")
    actual val sampleLoadingState: FmodStudioLoadingState
        get() = TODO("Not yet implemented")

    actual fun loadSampleData() {
    }

    actual fun unloadSampleData() {
    }
}

actual class FmodEventDescription {
    actual fun createInstance(): FmodEventInstance {
        TODO("Not yet implemented")
    }

    actual fun loadSampleData() {
    }

    actual fun getParameterDescriptionByName(name: String): FmodParameterDescription {
        TODO("Not yet implemented")
    }
}

actual class FmodParameterDescription {
    actual val id: FmodParameterId
        get() = TODO("Not yet implemented")
}

actual class FmodEventInstance {
    actual fun start() {
    }

    actual fun stop(mode: FmodStudioStopType) {
    }

    actual fun release() {
    }

    actual fun setCallback(
        callback: FmodCallback,
        callbackMask: FmodCallbackType
    ) {
    }

    actual fun setParameterByID(
        id: FmodParameterId,
        value: Float,
        something: Boolean
    ) {
    }
}

actual class FmodDriverInfo

actual class FmodCpu() {
    actual val dsp: Float
        get() = TODO("Not yet implemented")
    actual val stream: Float
        get() = TODO("Not yet implemented")
    actual val update: Float
        get() = TODO("Not yet implemented")
}

actual class FmodParameterId
actual class FmodCallback actual constructor(externalCallback: FmodCallbackExternal)