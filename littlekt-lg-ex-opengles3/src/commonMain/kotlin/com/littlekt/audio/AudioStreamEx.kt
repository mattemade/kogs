package com.littlekt.audio

interface AudioStreamEx : AudioStream {

    val paused: Boolean

    /**
     * Play this spatial audio stream.
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param positionX the X world coordinate of the audio. Defaults to 0.
     * @param positionY the Y world coordinate of the audio. Defaults to 0.
     * @param referenceDistance the distance at which volume starts to reduce. Defaults to 1.
     * @param maxDistance the distance at which sound is silenced completely. Defaults to 10000.
     * @param rolloffFactor the speed of sound getting quieter with distance. Defaults to 1.
     * @param loop whether to loop this audio.
     */
    suspend fun play(
        volume: Float = this.volume,
        positionX: Float = 0f,
        positionY: Float = 0f,
        referenceDistance: Float = 10000f,
        maxDistance: Float = 10000f,
        rolloffFactor: Float = 0f,
        loop: Boolean = false,
        seek: Float = 0f,
    )

    fun setPosition(positionX: Float, positionY: Float)

    fun fullStop()
}
