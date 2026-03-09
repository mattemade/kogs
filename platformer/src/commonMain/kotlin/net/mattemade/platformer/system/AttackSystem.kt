package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.littlekt.math.PI2_F
import com.littlekt.math.clamp
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.RotationComponent
import kotlin.math.abs

class AttackSystem(
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(family { all(AttackComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val attack = entity[AttackComponent]

        if (attack.resetCooldown > 0f) {
            attack.resetCooldown -= deltaTime
            if (attack.resetCooldown <= 0f) {
                attack.currentCooldown = 0f
                attack.currentAttackIndex = 0
            }
        }

        if (attack.currentCooldown > 0f) {
            attack.currentCooldown -= deltaTime
        }

        if (attack.activated) {
            attack.activated = false
            if (attack.currentCooldown <= 0) {
                entity[Box2DPhysicsComponent].playSound(
                    when (attack.currentAttackIndex) {
                        1 -> gameContext.fmodAssets.secondAttack
                        2 -> gameContext.fmodAssets.thirdAttack
                        else -> gameContext.fmodAssets.firstAttack
                    }
                )
                attack.spamming = false
                attack.requestingPhysicsToSpawnAttack = attack.specs[attack.currentAttackIndex].damage
                attack.resetCooldown = attack.maxResetCooldown
                attack.currentCooldown = attack.specs[attack.currentAttackIndex].shortCooldown
                attack.currentAttackIndex = (attack.currentAttackIndex + 1) % attack.specs.size
            } else if (!attack.spamming) {
                attack.spamming = true
                val previousAttack = (attack.currentAttackIndex + attack.specs.size - 1) % attack.specs.size
                attack.currentCooldown = attack.specs[previousAttack].longCooldown
                attack.resetCooldown = maxOf(attack.currentCooldown, attack.maxResetCooldown)
            }
        }
    }
}