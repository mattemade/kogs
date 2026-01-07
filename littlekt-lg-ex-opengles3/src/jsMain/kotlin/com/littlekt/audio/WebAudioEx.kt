package com.littlekt.audio

import com.littlekt.Releasable
import com.littlekt.file.WebVfs
import com.littlekt.log.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class WebAudioEx private constructor(
    private val createPipeline: () -> WebAudioPipeline,
    private val durationProvider: () -> Duration
) : Releasable {

    companion object {
        suspend fun create(
            job: Job,
            url: String,
            logger: Logger,
        ): WebAudioEx? {
            val audioContext = globalAudioContext
            if (audioContext == null) {
                logger.error { "Failed accessing audio context to load $url" }
                return null
            }
            val data = CompletableDeferred<WebAudioEx?>(job)
            WebVfs.loadRaw(
                job = job,
                url = url,
                processRawData = { it },
                onError = { event ->
                    logger.error { "Failed loading audio data $url: $event" }
                    data.complete(null)
                },
            )?.let { arrayBuffer ->
                audioContext.decodeAudioData(arrayBuffer, onLoad = { buffer ->
                    val sourceGenerator: () -> WebAudioPipeline = {
                        WebAudioPipeline(buffer, audioContext)
                    }
                    data.complete(
                        WebAudioEx(
                            createPipeline = sourceGenerator,
                            durationProvider = { buffer.duration.seconds })
                    )
                }, onError = { event ->
                    logger.error { "Failed decoding audio data $url: $event" }
                    data.complete(null)
                })
            }
            return data.await()
        }
    }

    internal var volume: Float = 1f
        set(value) {
            val newValue = max(0f, value)
            pipeline.gain.gain.value = newValue
            field = newValue
        }
    internal var looping: Boolean = false
        set(value) {
            pipeline.source.loop = value
            field = value
        }
    private var isPlaying: Boolean = false
    internal val playing: Boolean
        get() = isPlaying

    internal lateinit var pipeline: WebAudioPipeline

    internal val duration: Duration by lazy { durationProvider() }

    fun preparePipeline() {
        pipeline = createPipeline()
    }

    fun setOnEnded(onEnded: (WebAudioPipeline) -> Unit) {
        val currentPipeline = pipeline
        currentPipeline.source.onended = {
            onEnded(currentPipeline)
        }
    }

    fun play(
        volume: Float,
        positionX: Float,
        positionY: Float,
        referenceDistance: Float,
        maxDistance: Float,
        rolloffFactor: Float,
        loop: Boolean,
        seek: Float,
        scheduleAt: Float,
    ) {
        this.volume = volume
        pipeline.panner.setOrientationCompat(0f, 0f, -1f) // face from the screen
        pipeline.panner.setPositionCompat(positionX, positionY)
        pipeline.panner.refDistance = referenceDistance
        pipeline.panner.maxDistance = maxDistance
        pipeline.panner.rolloffFactor = rolloffFactor
        pipeline.source.loop = loop
        pipeline.source.playbackRate.value = 1f
        pipeline.source.start(whenTime = scheduleAt, offset = seek)
        isPlaying = true
    }

    internal fun stop() = stop(pipeline)

    internal fun stop(pipeline: WebAudioPipeline) {
        pipeline.source.playbackRate.value = 0f
        isPlaying = false
    }

    internal fun fullStop() {
        pipeline.source.stop(0.0)
    }

    internal fun resume() = resume(pipeline)

    internal fun resume(pipeline: WebAudioPipeline) {
        pipeline.source.playbackRate.value = 1f
        isPlaying = true
    }

    internal fun pause() = pause(pipeline)

    internal fun pause(pipeline: WebAudioPipeline) {
        pipeline.source.playbackRate.value = 0f
        isPlaying = false
    }

    internal fun setPlaybackRate(pipeline: WebAudioPipeline, playbackRate: Float) {
        pipeline.source.playbackRate.value = playbackRate
    }

    internal fun setLoop(pipeline: WebAudioPipeline, loop: Boolean) {
        pipeline.source.loop = loop
    }

    override fun release() {
        pipeline.release()
    }
}
