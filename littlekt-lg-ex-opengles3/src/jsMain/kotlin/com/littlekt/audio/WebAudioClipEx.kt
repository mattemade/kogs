package com.littlekt.audio

import com.littlekt.util.datastructure.BiMap
import kotlin.math.max
import kotlin.time.Duration

internal class WebAudioClipEx(private val audio: WebAudioEx) : AudioClipEx {
    val id = idx++
    override var volume: Float
        get() = audio.volume
        set(value) {
            audio.volume = value
        }
    override val duration: Duration
        get() = audio.duration

    // don't care about ID overflow, it could only become a problem if someone play Int.MAX_VALUE*2 looping sounds
    private var nextPipelineId = 0
    private val activePipelines = BiMap<Int, WebAudioPipeline>()

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
    ): Int {
        if (onlyIfNoSameSounds && !activePipelines.isEmpty()) {
            return -1
        }
        audio.preparePipeline()
        val currentPipelineId = nextPipelineId++
        activePipelines.put(currentPipelineId, audio.pipeline)
        audio.play(
            volume,
            positionX,
            positionY,
            referenceDistance,
            maxDistance,
            rolloffFactor,
            loop,
            seek,
            scheduleAt,
        )
        audio.setOnEnded { pipeline ->
            activePipelines.removeValue(pipeline)
            pipeline.release()
            onEnded?.invoke(currentPipelineId)
        }
        return currentPipelineId
    }

    override fun play(volume: Float, loop: Boolean) {
        play(volume, 0f, 0f, 10000f, 10000f, 0f, loop)
    }

    override fun stop() {
        audio.stop()
    }

    override fun resume() {
        audio.resume()
    }

    override fun pause() {
        audio.pause()
    }

    override fun release() {
        audio.release()
        activePipelines.values.forEach { it.release() }
        activePipelines.clear()
    }

    override fun setVolumeAll(volume: Float) {
        activePipelines.values.forEach { it.gain.gain.value = max(0f, volume) }
    }

    override fun setVolume(id: Int, volume: Float) {
        activePipelines.get(id)?.let { it.gain.gain.value = max(0f, volume) }
    }

    override fun setPositionAll(positionX: Float, positionY: Float) {
        activePipelines.values.forEach {
            it.panner.setPositionCompat(positionX, positionY)
        }
    }

    override fun setPosition(id: Int, positionX: Float, positionY: Float) {
        activePipelines.get(id)?.panner?.setPositionCompat(positionX, positionY)
    }

    override fun stopAll() {
        activePipelines.values.forEach { pipeline ->
            audio.stop(pipeline)
            pipeline.release()
        }
        activePipelines.clear()
    }

    override fun stop(id: Int) {
        activePipelines.removeKey(id)?.let {
            audio.stop(it)
            it.release()
        }
    }

    override fun pauseAll() {
        activePipelines.values.forEach(audio::pause)
    }

    override fun pause(id: Int) {
        activePipelines.get(id)?.let { audio.pause(it) }
    }

    override fun resumeAll() {
        activePipelines.values.forEach(audio::resume)
    }

    override fun resume(id: Int) {
        activePipelines.get(id)?.let { audio.resume(it) }
    }

    override fun setPlaybackRateAll(playbackRate: Float) {
        activePipelines.values.forEach { audio.setPlaybackRate(it, playbackRate) }
    }

    override fun setPlaybackRate(id: Int, playbackRate: Float) {
        activePipelines.get(id)?.let { audio.setPlaybackRate(it, playbackRate) }
    }

    override fun setLoop(id: Int, loop: Boolean) {
        activePipelines.get(id)?.let { audio.setLoop(it, loop) }
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is WebAudioClipEx && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        private var idx = 0
    }
}
