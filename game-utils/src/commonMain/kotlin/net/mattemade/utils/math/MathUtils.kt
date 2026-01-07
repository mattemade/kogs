package net.mattemade.utils.math

import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.cos
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.sin
import kotlin.math.abs

private val mathTempVec2f = MutableVec2f()
fun belongsToEllipse(
    x: Float,
    y: Float,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    extra: Float = 0f
): Boolean {
    mathTempVec2f.set(x, y).subtract(cx, cy)
    val distance = mathTempVec2f.length()
    val angle = mathTempVec2f.angleTo(Vec2f.X_AXIS)
    val referenceDistance = mathTempVec2f.set(
        rx * cos(angle),
        ry * sin(angle)
    ).length()
    return distance <= referenceDistance + extra
}


fun FloatArray.fill(vararg value: Float) {
    for (i in indices) {
        this[i] = value[i % value.size]
    }
}

fun FloatArray.fillPart(vararg value: Float) {
    for (i in value.indices) {
        this[i] = value[i % size]
    }
}

fun FloatArray.fill(block: (Int) -> Float) {
    for (i in indices) {
        this[i] = block(i)
    }
}


val NO_ROTATION = Vec2f(100f, 0f)

fun minByAbs(a: Float, b: Float): Float = if (abs(a) < abs(b)) a else b

fun minimalRotation(angleFrom: Float, angleTo: Float): Float {
    val diff = angleTo - angleFrom
    return minByAbs(minByAbs(diff, diff + PI2_F), diff - PI2_F)
}

fun MutableVec2f.rotateTowards(target: Vec2f, limit: Float = Float.MAX_VALUE - 10f) {
    val originalRotation = this.angleTo(NO_ROTATION).radians
    val newRotation = target.angleTo(NO_ROTATION).radians
    val difference = minimalRotation(originalRotation, newRotation)
    rotate(difference.clamp(-limit, limit).radians)
}

public inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
    var sum: Float = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
