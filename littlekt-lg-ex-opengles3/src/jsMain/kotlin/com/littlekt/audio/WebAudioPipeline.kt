package com.littlekt.audio

import com.littlekt.Releasable

internal class WebAudioPipeline(
    jsSound: JsSound,
    audioContext: AudioContext
) : Releasable {

    private val hash = index++
    val panner: JsAudioPannerNode = audioContext.createPanner().apply {
        distanceModelType = DistanceModelType.linear
        panningModelType = PanningModelType.HRTF
        connect(audioContext.destination)
    }
    val gain: JsAudioGainNode = audioContext.createGain().apply {
        connect(panner)
    }
    val source: JsAudioBufferSourceNode = audioContext.createBufferSource().apply {
        buffer = jsSound
        connect(gain)
    }

    override fun release() {
        source.disconnect()
        gain.disconnect()
        panner.disconnect()
    }

    override fun hashCode(): Int  = hash

    override fun equals(other: Any?): Boolean {
        return other != null && other is WebAudioPipeline && other.hash == hash
    }

    companion object {
        private var index = 0
    }
}
