package com.littlekt.audio

import com.littlekt.Audio
import com.littlekt.Releasable
import com.littlekt.util.fastForEach
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL10.AL_POSITION
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import org.lwjgl.openal.SOFTDirectChannels
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class OpenALAudioContext : Audio, Releasable {
    val device =
        try {
            val byteBuffer: ByteBuffer? = null
            ALC10.alcOpenDevice(byteBuffer)
        } catch (ex: Exception) {
            ex.printStackTrace()
            -1
        }
    val context: Long =
        try {
            val deviceCaps = ALC.createCapabilities(device)
            val context = ALC10.alcCreateContext(device, null as IntBuffer?)
            ALC10.alcMakeContextCurrent(context)
            AL.createCapabilities(deviceCaps)
            AL11.alDistanceModel(AL11.AL_LINEAR_DISTANCE_CLAMPED)
            context
        } catch (ex: Exception) {
            ex.printStackTrace()
            -1
        }

    val NO_DEVICE
        get() = context == -1L || device == -1L

    internal val audioStreams = mutableListOf<OpenALAudioStream>()

    private val sources by lazy {
        val result = mutableListOf<Int>()
        for (i in 0 until 4) { // total simultaneous sources
            val sourceID: Int = AL10.alGenSources()
            if (AL10.alGetError() != AL10.AL_NO_ERROR) break
            result.add(sourceID)
        }
        result
    }

    suspend fun update() {
        if (NO_DEVICE) return
        audioStreams.fastForEach { it.update() }
    }

    fun obtainSource(): Int {
        if (NO_DEVICE) return 0

        return sources
            .asSequence()
            .map { it to AL10.alGetSourcei(it, AL10.AL_SOURCE_STATE) }
            .firstOrNull { (_, state) -> state != AL10.AL_PLAYING && state != AL10.AL_PAUSED }
            ?.let { (sourceId, _) ->
                AL10.alSourceStop(sourceId)
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
                AL10.alSourcef(sourceId, AL10.AL_GAIN, 1f)
                AL10.alSourcef(sourceId, AL10.AL_PITCH, 1f)
                //AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcei(sourceId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
                sourceId
            } ?: -1
    }

    fun resumeSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePlay(it)
            }
        }
    }

    fun stopSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
            }
        }
    }

    fun pauseSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePause(it)
            }
        }
    }

    fun disposeSourceViaBufferID(bufferID: Int) {
        if (bufferID == -1) return

        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        AL10.alDeleteBuffers(bufferID)
    }

    override fun release() {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == it) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        if (context != -1L) {
            ALC10.alcDestroyContext(context)
        }
        if (device != -1L) {
            ALC10.alcCloseDevice(device)
        }
    }

    override fun isReady(): Boolean = true

    override fun setListenerPosition(x: Float, y: Float, z: Float) {
        AL10.alListener3f(AL_POSITION, x, y, z)
    }

    override fun suspend() {}

    override fun resume() {}

    override fun currentTime() =
        System.currentTimeMillis() / 1000.0

    override suspend fun loadModule(filename: String, tag: String) {}
}
