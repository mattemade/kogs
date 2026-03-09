package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityComponentContext
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.EnemyComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import kotlin.math.sign
import kotlin.random.Random

class EnemyBehaviourSystem(
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family {
    all(
        Box2DPhysicsComponent, MoveComponent, JumpComponent, ContextComponent, EnemyComponent
    )
}, interval = interval) {


    private fun createRandomIntent(): Intent =
        when (Random.nextInt(2)) {
            0 -> Intent.Move(1f * (Random.nextFloat() - 0.5f).sign, 0f, limit = 1f + Random.nextFloat() * 3f)
            else -> Intent.Idle(0.5f + Random.nextFloat() * 3f)
        }

    override fun onTickEntity(entity: Entity) {
        val enemyComponent = entity[EnemyComponent]

        enemyComponent.currentIntent = enemyComponent.currentIntent?.run { switch(entity) }

        if (enemyComponent.currentIntent == null) {
            enemyComponent.currentIntent = createRandomIntent()
        }


        when (val intent = enemyComponent.currentIntent) {
            is Intent.Move -> entity[MoveComponent].moveDirection.set(intent.x, intent.y)
            is Intent.Idle -> entity[MoveComponent].moveDirection.set(0f, 0f)
            null -> { /* should be impossible at this moment */}
        }

        //println(entity[EnemyComponent].enemy.name)
    }


    sealed interface Intent {

        fun IteratingSystem.switch(entity: Entity): Intent?

        fun IteratingSystem.shouldStop(entity: Entity): Boolean

        class Idle(var limit: Float = 0f) : Intent {
            override fun IteratingSystem.switch(entity: Entity): Intent? =
                if (shouldStop(entity)) null else this@Idle

            override fun IteratingSystem.shouldStop(entity: Entity): Boolean {
                limit -= deltaTime
                return limit <= 0f
            }

        }

        class Move(var x: Float, var y: Float, var keep: Float = 0f, var limit: Float = 0f): Intent {

            override fun IteratingSystem.switch(entity: Entity): Intent? {
                if (keep > 0f) {
                    keep -= deltaTime
                    return this@Move
                }
                val context = entity[ContextComponent]
                return if (shouldStop(entity)) {
                    null
                } else if (context.standing) {
                    if ((!context.standingRightFoot || context.touchingRightWall) && x > 0f || (!context.standingLeftFoot || context.touchingLeftWall) && x < 0f) {
                        x = -x
                        keep = 0.5f // move the opposite way for some time
                    }
                    this@Move
                } else {
                    this@Move
                }
            }

            override fun IteratingSystem.shouldStop(entity: Entity): Boolean {
                limit -= deltaTime
                return limit <= 0f || entity[ContextComponent].swimming
            }

        }

    }
}