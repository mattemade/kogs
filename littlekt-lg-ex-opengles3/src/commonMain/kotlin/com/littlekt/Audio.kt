package com.littlekt

import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStreamEx

interface Audio {

    fun isReady(): Boolean
    /**
     * Set the listener position for spatial sound. This position is used to control the pan and
     * volume of all the spatial sounds ([AudioClipEx], [AudioStreamEx]).
     */
    fun setListenerPosition(x: Float, y: Float, z: Float)

    fun suspend()

    fun resume()

    fun currentTime(): Double

    suspend fun loadModule(filename: String, tag: String)
}
