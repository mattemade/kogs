package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.EnemyComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.SpriteComponent
import kotlin.math.abs

class MusicSwitchingSystem(
    private val musicType: String? = null,
    var activeGauntletNearby: Boolean = false,
    var gauntletInProgress: Boolean = false,
    var isBoss: Boolean = false,
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f)
) : IteratingSystem(family { all(SpriteComponent) }, interval = interval) {


    private var enemies: Int = 0

    override fun onTick() {
        gameContext.musicType =
            if (activeGauntletNearby) {
                if (gauntletInProgress) {
                    if (isBoss) {
                        boss
                    } else {
                        gauntlet
                    }
                } else {
                    silence
                }
            } else {
                musicType
            }
        enemies = 0
        super.onTick()
        gameContext.enemiesInTheRoom = enemies > 0
    }

    override fun onTickEntity(entity: Entity) {
        entity.getOrNull(PlayerComponent)?.let { player ->
            entity.getOrNull(ContextComponent)?.let { context ->
                gameContext.swimmingMusic = context.swimming
            }
            entity.getOrNull(HealthComponent)?.let {
                gameContext.lowHealth = it.health <= 1.5f
            }
        }

        entity.getOrNull(EnemyComponent)?.let {
            enemies++
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

    companion object {
        val silence = "silence"
        val gauntlet = "gauntlet"
        val boss = "boss"
    }
}