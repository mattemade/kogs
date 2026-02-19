package net.mattemade.utils.util

import com.littlekt.math.floorToInt

class FpsCounter {

    private var timeInPeriod: Float = 0f
    private var framesInPeriod: Int = 0
    private var totalTime: Float = 0f
    private var totalFrames: Int = 0


    fun update(seconds: Float) {
        totalTime += seconds
        timeInPeriod += seconds
        while (timeInPeriod > 1) {
            timeInPeriod -= 1f
            totalFrames += framesInPeriod
            println("FPS: ${framesInPeriod}, avg: ${(totalFrames / totalTime).floorToInt()}")
            framesInPeriod = 0
        }
        framesInPeriod++
    }
}