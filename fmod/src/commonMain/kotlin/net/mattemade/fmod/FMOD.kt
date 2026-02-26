package net.mattemade.fmod

/** Should be run before initialising FMOD */
expect fun FMOD_Module_Create(preRun: () -> Unit, callback: () -> Unit)
/** Should be run before loading FMOD banks */
expect fun FMOD_FS_createPreloadedFile(filename: String)

expect fun FMOD_Studio_System_Create(): FmodStudioSystem

object FMOD {
    const val OK: FmodResult = 0

    const val STUDIO_INIT_NORMAL: FmodStudioInitFlag = 0x0
    const val STUDIO_INIT_LIVEUPDATE: FmodStudioInitFlag = 0x1
    const val STUDIO_INIT_ALLOW_MISSING_PLUGINS: FmodStudioInitFlag = 0x2
    const val STUDIO_INIT_SYNCHRONOUS_UPDATE: FmodStudioInitFlag = 0x4
    const val STUDIO_INIT_DEFERRED_CALLBACKS: FmodStudioInitFlag = 0x8
    const val STUDIO_INIT_LOAD_FROM_UPDATE: FmodStudioInitFlag = 0x10
    const val STUDIO_INIT_MEMORY_TRACKING: FmodStudioInitFlag = 0x20


    const val INIT_NORMAL: FmodInitFlag = 0x0;
    const val INIT_STREAM_FROM_UPDATE: FmodInitFlag = 0x1;
    const val INIT_MIX_FROM_UPDATE: FmodInitFlag = 0x2;
    const val INIT_3D_RIGHTHANDED: FmodInitFlag = 0x4;
    const val INIT_CLIP_OUTPUT: FmodInitFlag = 0x8;
    const val INIT_CHANNEL_LOWPASS: FmodInitFlag = 0x100;
    const val INIT_CHANNEL_DISTANCEFILTER: FmodInitFlag = 0x200;
    const val INIT_PROFILE_ENABLE: FmodInitFlag = 0x10000;
    const val INIT_VOL0_BECOMES_VIRTUAL: FmodInitFlag = 0x20000;
    const val INIT_GEOMETRY_USECLOSEST: FmodInitFlag = 0x40000;
    const val INIT_PREFER_DOLBY_DOWNMIX: FmodInitFlag = 0x80000;
    const val INIT_THREAD_UNSAFE: FmodInitFlag = 0x100000;
    const val INIT_PROFILE_METER_ALL: FmodInitFlag = 0x200000;
    const val INIT_MEMORY_TRACKING: FmodInitFlag = 0x400000;

    const val STUDIO_LOADING_STATE_UNLOADING: FmodStudioLoadingState = 0
    const val STUDIO_LOADING_STATE_UNLOADED: FmodStudioLoadingState = 1
    const val STUDIO_LOADING_STATE_LOADING: FmodStudioLoadingState = 2
    const val STUDIO_LOADING_STATE_LOADED: FmodStudioLoadingState = 3
    const val STUDIO_LOADING_STATE_ERROR: FmodStudioLoadingState = 4

    const val STUDIO_LOAD_BANK_NORMAL: FmodStudioLoadingType = 0x0
    const val STUDIO_LOAD_BANK_NONBLOCKING: FmodStudioLoadingType = 0x1

    const val FMOD_STUDIO_STOP_ALLOWFADEOUT: FmodStudioStopType = 0
    const val STUDIO_STOP_IMMEDIATE: FmodStudioStopType = 1

    const val SPEAKERMODE_DEFAULT: FmodSpeakerMode = 0
    const val SPEAKERMODE_RAW: FmodSpeakerMode = 1
    const val SPEAKERMODE_MONO: FmodSpeakerMode = 2
    const val SPEAKERMODE_STEREO: FmodSpeakerMode = 3
    const val SPEAKERMODE_QUAD: FmodSpeakerMode = 4
    const val SPEAKERMODE_SURROUND: FmodSpeakerMode = 5
    const val SPEAKERMODE_5POINT1: FmodSpeakerMode = 6
    const val SPEAKERMODE_7POINT1: FmodSpeakerMode = 7
    const val SPEAKERMODE_7POINT1POINT4: FmodSpeakerMode = 8
    const val SPEAKERMODE_MAX: FmodSpeakerMode = 9

    const val STUDIO_EVENT_CALLBACK_TIMELINE_MARKER: FmodCallbackType = 0x800
    const val STUDIO_EVENT_CALLBACK_TIMELINE_BEAT: FmodCallbackType = 0x1000
    const val STUDIO_EVENT_CALLBACK_SOUND_PLAYED: FmodCallbackType = 0x2000
    const val STUDIO_EVENT_CALLBACK_SOUND_STOPPED: FmodCallbackType = 0x4000
}

expect class FmodStudioSystem {
    val coreSystem: FmodStudioSystemCore
    fun initialize(maxChannels: Int, studioInitFlags: FmodStudioInitFlag, initFlags: FmodInitFlag, extraDriverData: Long? = null)
    fun loadBankFile(file: String, studioLoadingBankType: FmodStudioLoadingType): FmodBank

    //fun loadBankMemory(memoryPointer: FmodMemoryPointer, memoryLength: Long, memoryMode: FmodStudioLoadMemoryMode)
    fun update()

    fun getEvent(eventName: String): FmodEventDescription
}

expect class FmodStudioSystemCore {
    fun setDSPBufferSize(bufferLength: Int, numBuffers: Int)
    fun getCPUUsage(cpu: FmodCpu)
    fun getDriverInfo(
        id: Int
    ): FmodDriverInfo

    fun setSoftwareFormat(sampleRate: Int, speakerMode: FmodSpeakerMode, numSpeakers: Int)
}

expect class FmodBank {
    val loadingState: FmodStudioLoadingState
    val sampleLoadingState: FmodStudioLoadingState

    fun loadSampleData()
    fun unloadSampleData()
}

expect class FmodEventDescription {
    fun createInstance(): FmodEventInstance
    fun loadSampleData()
    fun getParameterDescriptionByName(name: String): FmodParameterDescription
}

expect class FmodParameterDescription {
    val id: FmodParameterId
}

expect class FmodEventInstance {
    fun start()
    fun stop(mode: FmodStudioStopType)
    fun release()
    fun setCallback(callback: FmodCallback, callbackMask: FmodCallbackType)
    fun setParameterByID(id: FmodParameterId, value: Float, ignoreSeekSpeed: Int)
}

expect class FmodDriverInfo {
    val systemRate: Int
    val speakerMode: FmodSpeakerMode
    val speakerModeChannels: Int
}

expect class FmodCallback(externalCallback: FmodCallbackExternal)

fun interface FmodCallbackExternal {
    fun invoke(type: FmodCallbackType, event: Long, parameters: Long): FmodResult
}

expect class FmodCpu {
    val dsp: Float
    val stream: Float
    val update: Float
}

class FmodFile(val memoryPointer: FmodMemoryPointer, val length: Long)

expect class FmodParameterId


typealias FmodResult = Int
typealias FmodStudioInitFlag = Int
typealias FmodInitFlag = Int
typealias FmodStudioLoadingState = Int
typealias FmodStudioLoadingType = Int
typealias FmodMemoryPointer = Long
typealias FmodStudioLoadMemoryMode = Int

typealias FmodStudioStopType = Int
typealias FmodSpeakerMode = Int
typealias FmodCallbackType = Int
