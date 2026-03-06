package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.MascotComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import kotlin.math.abs
import kotlin.math.sqrt

class MascotSystem(
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(family { all(MascotComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val (player, target) = entity[MascotComponent]
        val position = entity[PositionComponent]
        val context = entity[ContextComponent]

        val playerContext = player[ContextComponent]
        val playerPosition = player[PositionComponent].position
        val playerRotation = player[RotationComponent].currentRotation
        target.set(if (playerContext.facingRight) -1f else 1f, -1f).rotate(playerRotation.radians).add(playerPosition)
        //target.set(playerPosition.x + if (playerContext.facingRight) -1f else 1f, playerPosition.y - 1f)

        //tempVec2f.set(target).subtract(position.position)
        //tempVec2f.setLength(sqrt(tempVec2f.length())).add(playerPosition)
        position.position.set(target)
        context.facingRight = playerContext.facingRight
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

    companion object {
        private val tempVec2f = MutableVec2f()
    }
}