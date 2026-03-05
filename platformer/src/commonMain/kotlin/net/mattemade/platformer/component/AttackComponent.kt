package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class AttackComponent(
    val specs: List<AttackSpec>,
    var currentAttackIndex: Int = 0,
    var currentCooldown: Float = 0f,
    var resetCooldown: Float = 0f,
    var maxResetCooldown: Float = 0.5f,
    var activated: Boolean = false,
    var spamming: Boolean = false,
    var requestingPhysicsToSpawnAttack: Float = 0f,
): Component<AttackComponent> {
    override fun type() = AttackComponent
    companion object: ComponentType<AttackComponent>()

    data class AttackSpec(
        val shortCooldown: Float,
        val longCooldown: Float,
        val damage: Float,
    )
}