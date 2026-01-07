package com.littlekt.audio

import com.littlekt.log.Logger
import org.lwjgl.openal.AL10.AL_BUFFER
import org.lwjgl.openal.AL10.AL_FALSE
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL10.AL_GAIN
import org.lwjgl.openal.AL10.AL_LOOPING
import org.lwjgl.openal.AL10.AL_MAX_DISTANCE
import org.lwjgl.openal.AL10.AL_POSITION
import org.lwjgl.openal.AL10.AL_REFERENCE_DISTANCE
import org.lwjgl.openal.AL10.AL_ROLLOFF_FACTOR
import org.lwjgl.openal.AL10.AL_SOURCE_RELATIVE
import org.lwjgl.openal.AL10.AL_TRUE
import org.lwjgl.openal.AL10.alBufferData
import org.lwjgl.openal.AL10.alGenBuffers
import org.lwjgl.openal.AL10.alSource3f
import org.lwjgl.openal.AL10.alSourcePlay
import org.lwjgl.openal.AL10.alSourcef
import org.lwjgl.openal.AL10.alSourcei
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioClip(
    private val context: OpenALAudioContext,
    pcm: ByteArray,
    val channels: Int,
    val sampleRate: Int
) : AudioClipEx {

    private var bufferID = -1

    override var volume: Float = 1f
    override val duration: Duration

    private val NO_DEVICE
        get() = context.NO_DEVICE

    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }
        val bytes = pcm.size - (pcm.size % (if (channels > 1) 4 else 2))
        val samples = bytes / (2 * channels)
        duration = (samples / sampleRate.toDouble()).seconds

        val buffer =
            ByteBuffer.allocateDirect(bytes).apply {
                order(ByteOrder.nativeOrder())
                put(pcm, 0, bytes)
                flip()
            }
        if (bufferID == -1) {
            bufferID = alGenBuffers()
            alBufferData(
                bufferID,
                (if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16),
                buffer.asShortBuffer(),
                sampleRate
            )
        }
    }

    override fun play(volume: Float, loop: Boolean) = withDevice {
        play(volume, 0f, 0f, 10000f, 10000f, 0f, loop)
    }

    override fun play(
        volume: Float,
        positionX: Float,
        positionY: Float,
        referenceDistance: Float,
        maxDistance: Float,
        rolloffFactor: Float,
        loop: Boolean,
        onlyIfNoSameSounds: Boolean,
        seek: Float,
        scheduleAt: Float,
        onEnded: ((Int) -> Unit)?
    ): Int = withDeviceReturning {
        val sourceId = context.obtainSource()

        if (sourceId == -1) return -1

        if (channels > 1 && positionX != 0f && positionY != 0f) {
            logger.error { "Multi-channel track does not support positioning" }
        }

        alSourcei(sourceId, AL_BUFFER, bufferID)
        alSourcei(sourceId, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcef(sourceId, AL_GAIN, volume)
        alSourcef(sourceId, AL_REFERENCE_DISTANCE, referenceDistance)
        alSourcef(sourceId, AL_MAX_DISTANCE, maxDistance)
        alSourcef(sourceId, AL_ROLLOFF_FACTOR, rolloffFactor)
        alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_FALSE)
        alSource3f(sourceId, AL_POSITION, positionX, positionY, 0f)
        alSourcePlay(sourceId)
        return sourceId
    }

    override fun setVolumeAll(volume: Float) {
        
    }

    override fun setVolume(id: Int, volume: Float) {
        
    }

    override fun setPositionAll(positionX: Float, positionY: Float) {
        
    }

    override fun setPosition(id: Int, positionX: Float, positionY: Float) {
        alSource3f(id, AL_POSITION, positionX, positionY, 0f)
    }

    override fun stopAll() {
        
    }

    override fun stop(id: Int) {
        
    }

    override fun stop() = withDevice { context.stopSourceViaBufferID(bufferID) }
    override fun pauseAll() {
        
    }

    override fun pause(id: Int) {
        
    }

    override fun resume() = withDevice { context.resumeSourceViaBufferID(bufferID) }

    override fun pause() = withDevice { context.pauseSourceViaBufferID(bufferID) }
    override fun resumeAll() {
        
    }

    override fun resume(id: Int) {
        
    }

    override fun setPlaybackRateAll(playbackRate: Float) {
        
    }

    override fun setPlaybackRate(id: Int, playbackRate: Float) {
        
    }

    override fun setLoop(id: Int, loop: Boolean) {

    }

    override fun release() = withDevice {
        if (bufferID == -1) return

        context.disposeSourceViaBufferID(bufferID)
        bufferID = -1
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    private inline fun withDeviceReturning(block: () -> Int): Int {
        if (NO_DEVICE) return -1
        return block()
    }

    companion object {
        private val logger = Logger<OpenALAudioClip>()
    }
}
