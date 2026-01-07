package com.littlekt.util.viewport

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * A base [Viewport] that handles scaling to a virtual width and height.
 * @author Colton Daily
 * @date 12/21/2021
 */
open class ScalingViewport(
    val scaler: Scaler,
    width: Int,
    height: Int,
    virtualWidth: Float = width.toFloat(),
    virtualHeight: Float = height.toFloat(),
    camera: Camera = OrthographicCamera(),
) :
    Viewport(0, 0, width, height, camera) {

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
    }

    override fun update(width: Int, height: Int, context: Context, centerCamera: Boolean) {
        val scaled = scaler.apply(virtualWidth, virtualHeight, width.toFloat(), height.toFloat())
        val viewportWidth = scaled.x.roundToInt()
        val viewportHeight = scaled.y.roundToInt()

        x = (width - viewportWidth) / 2
        y = (height - viewportHeight) / 2
        this.width = viewportWidth
        this.height = viewportHeight

        apply(context, centerCamera)
    }
}

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport will maintain its aspect ratio while attempting to fit as much as possible onto the screen.
 * Black bars may appear.
 */
open class FitViewport(
    virtualWidth: Int,
    virtualHeight: Int,
    camera: Camera = OrthographicCamera()
) :
    ScalingViewport(
        Scaler.Fit(), virtualWidth,
        virtualHeight, virtualWidth.toFloat(), virtualHeight.toFloat(), camera
    )

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport is stretched to fit the screen. There are no black bars and the aspect ratio can change after scaling.
 */
open class StretchViewport(
    virtualWidth: Int,
    virtualHeight: Int,
    camera: Camera = OrthographicCamera()
) :
    ScalingViewport(
        Scaler.Stretch(), virtualWidth,
        virtualHeight, virtualWidth.toFloat(), virtualHeight.toFloat(), camera
    )

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport will maintain its aspect ratio but in an attempt to fill
 * the screen parts of the viewport may be cut off.
 * No black bars may appear.
 */
open class FillViewport(
    virtualWidth: Int,
    virtualHeight: Int,
    camera: Camera = OrthographicCamera()
) :
    ScalingViewport(
        Scaler.Fill(), virtualWidth,
        virtualHeight, virtualWidth.toFloat(), virtualHeight.toFloat(), camera
    )