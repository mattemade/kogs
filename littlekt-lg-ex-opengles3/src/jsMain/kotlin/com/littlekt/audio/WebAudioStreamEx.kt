package com.littlekt.audio

internal class WebAudioStreamEx(private val audio: WebAudioEx) : AudioStreamEx {
    override var volume: Float
        get() = audio.volume
        set(value) {
            audio.volume = value
        }
    override var looping: Boolean
        get() = audio.looping
        set(value) {
            audio.looping = value
        }
    override val playing: Boolean
        get() = audio.playing

    private var isPaused: Boolean = false
    override val paused: Boolean
        get() = isPaused

    init {
        audio.preparePipeline()
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
    ) {
        audio.play(
            volume,
            positionX,
            positionY,
            referenceDistance,
            maxDistance,
            rolloffFactor,
            loop,
            seek,
            scheduleAt = 0f
        )
        isPaused = false
    }

    override suspend fun play(volume: Float, loop: Boolean) {
        audio.play(volume, 0f, 0f, 10000f, 10000f, 0f, loop, seek = 0f, scheduleAt = 0f)
        isPaused = false
    }

    override fun setPosition(positionX: Float, positionY: Float) {
        audio.pipeline.panner.setPositionCompat(positionX, positionY)
    }

    override fun stop() {
        audio.stop()
        isPaused = false
    }

    override fun fullStop() {
        audio.fullStop()
    }

    override fun resume() {
        audio.resume()
        isPaused = false
    }

    override fun pause() {
        audio.pause()
        isPaused = true
    }

    override fun release() {
        audio.release()
    }
}
