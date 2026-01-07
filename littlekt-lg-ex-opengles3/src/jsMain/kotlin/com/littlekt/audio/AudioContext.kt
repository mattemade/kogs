package com.littlekt.audio

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.w3c.dom.events.Event
import kotlin.js.Promise

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
internal external class JsSound {
    var sampleRate: Float = definedExternally
    var length: Int = definedExternally
    var duration: Double = definedExternally
    var numberOfChannels: Int = definedExternally
    fun getChannelData(channel: Int): Float32Array = definedExternally
    fun copyFromChannel(destination: Float32Array, channel: Int, startInChannel: Int): Float32Array = definedExternally
}

internal external interface JsSoundDestination

internal external class JsAudioParam<T> {
    var value: T
}

internal open external class JsAudioNode {
    fun connect(destination: JsSoundDestination)
    fun disconnect()
}

internal external class JsAudioGainNode : JsAudioNode, JsSoundDestination {
    val gain: JsAudioParam<Float>
}

internal enum class DistanceModelType {
    linear,
    inverse,
    exponential
}

internal enum class PanningModelType {
    equalpower,
    HRTF
}

internal external class JsAudioPannerNode : JsAudioNode, JsSoundDestination {
    val positionX: JsAudioParam<Float>?
    val positionY: JsAudioParam<Float>?

    val orientationX: JsAudioParam<Float>?
    val orientationY: JsAudioParam<Float>?
    val orientationZ: JsAudioParam<Float>?

    // deprecated, but usable in firefox
    fun setPosition(x: Float, y: Float)

    // deprecated, but usable in firefox
    fun setOrientation(x: Float, y: Float, z: Float)

    var refDistance: Float = definedExternally
    var maxDistance: Float = definedExternally
    var rolloffFactor: Float = definedExternally
    var distanceModelType: DistanceModelType = definedExternally
    var panningModelType: PanningModelType = definedExternally
}

internal fun JsAudioPannerNode.setPositionCompat(x: Float, y: Float) {
    if (positionX == null || positionY == null) {
        // firefox hack
        setPosition(x, y)
        return
    }
    this.positionX.value = x
    this.positionY.value = y
}

internal fun JsAudioPannerNode.setOrientationCompat(x: Float, y: Float, z: Float) {
    if (orientationX == null || orientationY == null || orientationZ == null) {
        // firefox hack
        setOrientation(x, y, z)
        return
    }
    this.orientationX.value = x
    this.orientationY.value = y
    this.orientationZ.value = z
}

internal external class JsAudioBufferSourceNode : JsAudioNode {
    var buffer: JsSound

    var loop: Boolean

    val playbackRate: JsAudioParam<Float>

    var onended: () -> Unit

    fun start(
        whenTime: Float = definedExternally,
        offset: Float = definedExternally,
        duration: Float = definedExternally
    )

    fun stop(time: Double)
}

internal external class JsAudioListener {
    val positionX: JsAudioParam<Float>?
    val positionY: JsAudioParam<Float>?
    val positionZ: JsAudioParam<Float>?
    val forwardX: JsAudioParam<Float>?
    val forwardY: JsAudioParam<Float>?
    val forwardZ: JsAudioParam<Float>?
    val upX: JsAudioParam<Float>?
    val upY: JsAudioParam<Float>?
    val upZ: JsAudioParam<Float>?

    // deprecated, but usable in firefox
    fun setPosition(x: Float, y: Float, z: Float)
    fun setOrientation(x: Float, y: Float, z: Float, upX: Float, upY: Float, upZ: Float)
}

internal fun JsAudioListener.setPositionCompat(x: Float, y: Float, z: Float) {
    if (positionX == null || positionY == null || positionZ == null) {
        // firefox hack
        setPosition(x, y, z)
        return
    }
    this.positionX.value = x
    this.positionY.value = y
    this.positionZ.value = z
}

internal fun JsAudioListener.setOrientationCompat(x: Float, y: Float, z: Float, uX: Float, uY: Float, uZ: Float) {
    if (forwardX == null || forwardY == null || forwardZ == null || upX == null || upY == null || upZ == null) {
        // firefox hack
        setOrientation(x, y, z, uX, uY, uZ)
        return
    }
    this.forwardX.value = x
    this.forwardY.value = y
    this.forwardZ.value = z
    this.upX.value = uX
    this.upY.value = uY
    this.upZ.value = uZ
}

external interface AudioWorkletProcessor {
    fun process(
        inputs: Array<Array<Float32Array>>,
        outputs: Array<Array<Float32Array>>,
        parameters: Map<String, Float32Array>
    ): Boolean
}

internal external class AudioContext {

    val state: String

    val currentTime: Double

    fun resume()

    fun suspend()

    val destination: JsSoundDestination

    var listener: JsAudioListener

    fun decodeAudioData(
        bytes: ArrayBuffer,
        onLoad: (buffer: JsSound) -> Unit,
        onError: (event: Event) -> Unit
    )

    fun createBufferSource(): JsAudioBufferSourceNode
    fun createGain(): JsAudioGainNode
    fun createPanner(): JsAudioPannerNode

    val audioWorklet: AudioWorklet
}

internal external class AudioWorklet {
    fun addModule(filename: String): Promise<Any>
}

internal open external class AudioWorkletNode(audioContext: AudioContext, tag: String) : JsAudioNode

//external fun registerProcessor(name: String, clazz: JsClass<out AudioWorkletProcessor>)

// Certain web browsers only allow playing sounds if it happens in response to a limited list of user actions.
// Once the AudioContext is correctly resumed, the app could play any sound anytime without restrictions.
// This method ensures that AudioContext is resumed as early as possible.
// TODO mobile Firefox doesn't allow creating an AudioContext before any tap is done
// how to fix that? maybe don't init the context until any of these events happen?
private var globalAudioContextCache: AudioContext? = null
internal val globalAudioContext: AudioContext?
    get() = globalAudioContextCache ?: (js(
        """
            var AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                return new AudioContextClass();
            }
            return null;
            """
    ) as? AudioContext)?.also { globalAudioContextCache = it }
