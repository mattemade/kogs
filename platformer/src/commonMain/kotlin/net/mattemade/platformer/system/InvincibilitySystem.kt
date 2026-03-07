package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.PI2_F
import com.littlekt.math.clamp
import net.mattemade.platformer.component.InvincibilityComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import kotlin.math.abs

class InvincibilitySystem(
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(family { all(InvincibilityComponent, SpriteComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val invincibilityComponent = entity[InvincibilityComponent]
        val spriteComponent = entity[SpriteComponent]
        invincibilityComponent.timeLeft -= deltaTime
        if (invincibilityComponent.timeLeft <= 0f) {
            entity.configure {
                it -= InvincibilityComponent
            }
            spriteComponent.visible = true
        } else {
            spriteComponent.visible = !spriteComponent.visible
        }
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