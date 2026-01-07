package com.littlekt

import com.littlekt.audio.AudioWorkletNode
import com.littlekt.audio.globalAudioContext
import com.littlekt.audio.setPositionCompat
import kotlinx.coroutines.await

class WebAudio: Audio {

    override fun isReady(): Boolean =
        globalAudioContext != null

    override fun setListenerPosition(x: Float, y: Float, z: Float) {
        globalAudioContext?.listener?.apply {
            setPositionCompat(x, y, z)
        }
    }

    override fun suspend() {
        globalAudioContext?.suspend()
    }

    override fun resume() {
        globalAudioContext?.resume()
    }

    override fun currentTime(): Double =
        globalAudioContext?.currentTime ?: 0.0

    override suspend fun loadModule(filename: String, tag: String) {
        globalAudioContext?.let {
            it.audioWorklet.addModule(filename).await()
            val node = AudioWorkletNode(it, tag)
            node.connect(it.destination)
        }
    }
}
