package com.littlekt.audio

import org.khronos.webgl.Float32Array

class MixingWorkletProcessor: AudioWorkletProcessor {
    override fun process(
        inputs: Array<Array<Float32Array>>,
        outputs: Array<Array<Float32Array>>,
        parameters: Map<String, Float32Array>
    ): Boolean {
        println("MixingWorkletProcessor processes something")
        return true
    }
}