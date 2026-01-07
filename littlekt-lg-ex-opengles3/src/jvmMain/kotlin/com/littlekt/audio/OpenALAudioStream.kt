package com.littlekt.audio

import com.littlekt.log.Logger
import com.littlekt.util.seconds
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.AL_BUFFER
import org.lwjgl.openal.AL10.AL_BUFFERS_PROCESSED
import org.lwjgl.openal.AL10.AL_BUFFERS_QUEUED
import org.lwjgl.openal.AL10.AL_FALSE
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL10.AL_GAIN
import org.lwjgl.openal.AL10.AL_INVALID_VALUE
import org.lwjgl.openal.AL10.AL_LOOPING
import org.lwjgl.openal.AL10.AL_NO_ERROR
import org.lwjgl.openal.AL10.AL_PLAYING
import org.lwjgl.openal.AL10.AL_POSITION
import org.lwjgl.openal.AL10.AL_SOURCE_STATE
import org.lwjgl.openal.AL10.alBufferData
import org.lwjgl.openal.AL10.alDeleteBuffers
import org.lwjgl.openal.AL10.alGenBuffers
import org.lwjgl.openal.AL10.alGetError
import org.lwjgl.openal.AL10.alGetSourcei
import org.lwjgl.openal.AL10.alSource3f
import org.lwjgl.openal.AL10.alSourcePause
import org.lwjgl.openal.AL10.alSourcePlay
import org.lwjgl.openal.AL10.alSourceQueueBuffers
import org.lwjgl.openal.AL10.alSourceStop
import org.lwjgl.openal.AL10.alSourceUnqueueBuffers
import org.lwjgl.openal.AL10.alSourcef
import org.lwjgl.openal.AL10.alSourcei
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioStream(
    private val context: OpenALAudioContext,
    private val read: (ByteArray) -> Int,
    private val reset: suspend () -> Unit,
    val channels: Int,
    val sampleRate: Int
) : AudioStreamEx {
    private val NO_DEVICE
        get() = context.NO_DEVICE

    private val tempBytes = ByteArray(BUFFER_SIZE)
    private val tempBuffer = BufferUtils.createByteBuffer(BUFFER_SIZE)

    private var sourceID = -1

    private val maxSecondsToBuffer =
        (BUFFER_SIZE.toFloat() / (BYTES_PER_SAMPLE * channels * sampleRate)).seconds
    private val format = if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
    private var buffers: IntBuffer? = null
    private var needsReset = false

    override var looping = false

    override var volume: Float = 1f
        set(value) {
            check(value >= 0) { "Volume must not be less than 0!" }
            field = value
            if (NO_DEVICE) return
            if (sourceID != -1) {
                alSourcef(sourceID, AL_GAIN, value)
            }
        }

    private var isPlaying = false
    override val playing: Boolean
        get() {
            if (NO_DEVICE || sourceID == -1) {
                return false
            }
            return isPlaying
        }
    private var isPaused = false
    override val paused: Boolean
        get() {
            if (NO_DEVICE || sourceID == -1) {
                return false
            }
            return isPaused
        }

    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }
    }

    override suspend fun play(
        volume: Float,
        positionX: Float,
        positionY: Float,
        referenceDistance: Float,
        maxDistance: Float,
        rolloffFactor: Float,
        loop: Boolean,
        seek: Float,
    ) = withDevice {
        if (sourceID == -1) {
            if (needsReset) {
                reset()
                needsReset = false
            }
            sourceID = context.obtainSource()
            if (sourceID == -1) return@withDevice

            context.audioStreams += this

            if (buffers == null) {
                buffers =
                    BufferUtils.createIntBuffer(BUFFER_COUNT).also {
                        alGetError()
                        alGenBuffers(it)
                        val errorCode = alGetError()
                        if (errorCode != AL_NO_ERROR) {
                            error("Unable to allocate audio buffers. AL Error: $errorCode")
                        }
                    }
            }
            alSourcei(sourceID, AL_LOOPING, AL_FALSE)

            this.volume = volume
            this.looping = loop
            alSourcef(sourceID, AL_GAIN, volume)

            alGetError()

            buffers?.let { buffers ->
                for (i in 0 until BUFFER_COUNT) {
                    val bufferID = buffers.get(i)
                    if (!fill(bufferID)) break
                    alSourceQueueBuffers(sourceID, bufferID)
                }
            }
            if (alGetError() != AL_NO_ERROR) {
                stop()
                return@withDevice
            }
        }

        if (!isPlaying) {
            alSourcePlay(sourceID)
            isPlaying = true
        }
        isPaused = false
    }

    override suspend fun play(volume: Float, loop: Boolean) =
        play(volume, 0f, 0f, 10000f, 10000f, 0f, loop)

    override fun setPosition(positionX: Float, positionY: Float) = withDevice {
        if (sourceID == -1) return@withDevice
        alSource3f(sourceID, AL_POSITION, positionX, positionY, 0f)
    }

    override fun stop() = withDevice {
        if (sourceID == -1) return@withDevice
        needsReset = true
        context.audioStreams -= this

        alSourceStop(sourceID)
        alSourcei(sourceID, AL_BUFFER, 0)

        sourceID = -1
        isPlaying = false
        isPaused = false
    }

    override fun fullStop() = stop()

    override fun resume() = withDevice {
        if (sourceID != -1) {
            alSourcePlay(sourceID)
            isPlaying = true
            isPaused = false
        }
    }

    override fun pause() = withDevice {
        if (sourceID != -1) {
            alSourcePause(sourceID)
        }
        isPlaying = false
        isPaused = true
    }

    override fun release() = withDevice {
        if (sourceID != -1) {
            context.audioStreams -= this
            alSourceStop(sourceID)
            alSourcei(sourceID, AL_BUFFER, 0)

            sourceID = -1
            isPlaying = false
        }
        buffers?.let { alDeleteBuffers(it) } ?: return@withDevice
        buffers = null
    }

    suspend fun update() = withDevice {
        if (sourceID == -1) return@withDevice
        var end = false
        var buffers = alGetSourcei(sourceID, AL_BUFFERS_PROCESSED)

        while (buffers-- > 0) {
            val bufferID = alSourceUnqueueBuffers(sourceID)
            if (bufferID == AL_INVALID_VALUE) break
            if (end) continue
            if (fill(bufferID)) {
                alSourceQueueBuffers(sourceID, bufferID)
            } else {
                end = true
            }
        }
        if (end && alGetSourcei(sourceID, AL_BUFFERS_QUEUED) == 0) {
            stop()
        }

        if (isPlaying && alGetSourcei(sourceID, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(sourceID)
        }
    }

    private suspend fun fill(bufferID: Int): Boolean {
        tempBuffer.clear()
        var length = read(tempBytes)
        if (length <= 0) {
            if (looping) {
                reset()
                length = read(tempBytes)
                if (length <= 0) return false
            } else {
                return false
            }
        }
        tempBuffer.put(tempBytes, 0, length).flip()
        alBufferData(bufferID, format, tempBuffer, sampleRate)

        return true
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    companion object {
        private val logger = Logger<OpenALAudioStream>()
        private const val BUFFER_SIZE = 4096 * 10
        private const val BUFFER_COUNT = 3
        private const val BYTES_PER_SAMPLE = 2
    }
}
