package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.PI2_F
import com.littlekt.math.clamp
import net.mattemade.platformer.component.RotationComponent
import kotlin.math.abs

class RotationSystem(
    interval: Interval = Fixed(1 / 100f)
) : IteratingSystem(family { all(RotationComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val (current, target, limit) = entity[RotationComponent]
        if (current == target) {
            return
        }

        val rotation = minOfAbs(
            target - current,
            target + PI2_F - current,
            target - PI2_F - current,
        ).clamp(-limit, limit)
        entity[RotationComponent].currentRotation = current + rotation
    }

    private fun minOfAbs(a: Float, b: Float): Float =
        if (abs(a) < abs(b)) a else b

    private fun minOfAbs(a: Float, b: Float, c: Float): Float {
        val absA = abs(a)
        val absB = abs(b)
        val absC = abs(c)
        return if (absA < absB) {
            if (absA < absC) {
                a
            } else {
                c
            }
        } else if (absB < absC) {
            b
        } else
            c
    }
}