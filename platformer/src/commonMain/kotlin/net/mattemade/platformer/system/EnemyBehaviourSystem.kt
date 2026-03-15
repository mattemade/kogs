package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
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
import kotlin.math.sin

class EnemyBehaviourSystem(
    private val gameContext: PlatformerGameContext = inject(),
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family {
    all(
        Box2DPhysicsComponent, MoveComponent, JumpComponent, ContextComponent, EnemyComponent
    )
}, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val enemyComponent = entity[EnemyComponent]

        enemyComponent.currentIntent = enemyComponent.currentIntent?.run { switch(entity) }

        if (enemyComponent.currentIntent == null) {
            // what a syntax!!
            val nextIntent = enemyComponent.nextIntent
            enemyComponent.currentIntent = nextIntent(entity)
        }

        when (val intent = enemyComponent.currentIntent) {
            is Intent.Move -> entity[MoveComponent].moveDirection.set(intent.x, intent.y)
//            is Intent.MoveToPlatformEndAndBackWhileLookingForPlayer -> entity[MoveComponent].moveDirection.set(intent.x, intent.y)
//            is Intent.MoveToPlatformEndAndBackWhileWainingForPlayerClose -> entity[MoveComponent].moveDirection.set(intent.x, intent.y)
            is Intent.JumpForward -> {
                entity[MoveComponent].moveDirection.set(intent.x, intent.y)
                entity[JumpComponent].jumping = intent.holdJumpFor > 0f
                entity[JumpComponent].canHoldJumpForTicks = JumpComponent.MAX_JUMP_TICKS // monster's jump is unlimited!!
            }
            is Intent.FlyInSine -> {
                entity[MoveComponent].moveDirection.set(intent.x, intent.y)
            }
            is Intent.FlyTo -> {
                entity[MoveComponent].moveDirection.set(0f, intent.dy)
            }
            is Intent.Idle,
            is Intent.WaitForPlayerAppear,
            is Intent.WaitForPlayerComeClose -> entity[MoveComponent].moveDirection.set(0f, 0f)

            null -> { /* should be impossible at this moment */
            }
        }

        //println(entity[EnemyComponent].enemy.name)
    }


    sealed interface Intent {

        fun IteratingSystem.switch(entity: Entity): Intent?

        //fun IteratingSystem.shouldStop(entity: Entity): Boolean


        class Idle(var limit: Float = 0f) : Intent {
            override fun IteratingSystem.switch(entity: Entity): Intent? =
                if (shouldStop(entity)) null else this@Idle

            fun IteratingSystem.shouldStop(entity: Entity): Boolean {
                limit -= deltaTime
                return limit <= 0f
            }
        }

        class WaitForPlayerAppear : Intent {
            override fun IteratingSystem.switch(entity: Entity): Intent? =
                if (shouldStop(entity)) null else this@WaitForPlayerAppear

            fun IteratingSystem.shouldStop(entity: Entity): Boolean =
                entity[EnemyComponent].spottedPlayerPosition != null
        }

        class WaitForPlayerComeClose : Intent {
            override fun IteratingSystem.switch(entity: Entity): Intent? =
                if (entity[EnemyComponent].nearPlayer != null) null else this@WaitForPlayerComeClose
        }

        class FlyTo(val y: () -> Float, val dy: Float, val next: () -> Intent) : Intent {
            override fun IteratingSystem.switch(entity: Entity): Intent? {
                val context = entity[ContextComponent]
                if (context.swimming) {
                    return this@FlyTo
                }
                val physics = entity[Box2DPhysicsComponent]
                if (context.standing || physics.body.position.y >= y()) {
                    return next()
                }
                return this@FlyTo
            }

        }

        class FlyInSine(var x: Float, var dy: Float, var timeScale: Float = 5f, var y: Float = 0f, val swimOnly: Boolean = false): Intent {

            private var time: Float = 0f

            override fun IteratingSystem.switch(entity: Entity): Intent? {
                val context = entity[ContextComponent]
                if (context.swimming && !swimOnly) {
                    y = 0f
                    dy = 0f
                    x = 0f
                    return this@FlyInSine
                }

                time += deltaTime * timeScale
                y = sin(time) * dy
                if (context.touchingRightWall && x > 0f || context.touchingLeftWall && x < 0f) {
                    x = -x
                }
                return this@FlyInSine
            }
        }

        class Move(var x: Float, var y: Float, var keep: Float = 0f, var limit: Float = 0f, val fallFromEdges: Boolean = false, val jumpOnWalls: Float = 0f, val triggerVision: ((Intent) -> Intent)? = null, val triggerProximity: ((Intent) -> Intent)? = null, val swim: Boolean = false) :
            Intent {

            override fun IteratingSystem.switch(entity: Entity): Intent? {
                if (keep > 0f) {
                    keep -= deltaTime
                    return this@Move
                }
                val context = entity[ContextComponent]
                return if (shouldStop(entity)) {
                    null
                } else if (triggerVision != null && entity[EnemyComponent].spottedPlayerPosition != null) {
                    triggerVision(this@Move).also { entity[EnemyComponent].spottedPlayerPosition = null }
                } else if (triggerProximity != null && entity[EnemyComponent].nearPlayer != null) {
                    triggerProximity(this@Move).also { entity[EnemyComponent].nearPlayer = null }
                } else if (swim) {
                    if (context.touchingRightWall && x > 0f || context.touchingLeftWall && x < 0f) {
                        x = -x
                        keep = 0.5f // move the opposite way for some time
                    }
                    this@Move
                } else if (context.standing) {
                    if (!context.standingRightFoot && x > 0f || !context.standingLeftFoot && x < 0f) {
                        if (fallFromEdges) {
                            this@Move
                        } else {
                            x = -x
                            keep = 0.5f // move the opposite way for some time
                            this@Move
                        }
                    } else if (context.touchingRightWall && x > 0f || context.touchingLeftWall && x < 0f) {
                        if (jumpOnWalls > 0f) {
                            JumpForward(x, y, holdJumpFor = jumpOnWalls, onEnd = this@Move, onNeverWasFree = {
                                x = -x // jump was no successful - reverse the motion
                            })
                        } else {
                            x = -x
                            keep = 0.5f // move the opposite way for some time
                            this@Move
                        }
                    } else {
                        this@Move
                    }
                } else {
                    this@Move
                }
            }

            fun IteratingSystem.shouldStop(entity: Entity): Boolean {
                limit -= deltaTime
                return limit <= 0f || entity[ContextComponent].swimming.let { swim && !it || !swim && it }
            }
        }

        class JumpForward(var x: Float, var y: Float, var holdJumpFor: Float = 0f, val onEnd: Intent? = null, val onNeverWasFree: (() -> Unit)? = null) : Intent {

            private var wasJumping = false
            private var everWasFree = false

            override fun IteratingSystem.switch(entity: Entity): Intent? =
                if (shouldStop(entity)) onEnd else this@JumpForward

            fun IteratingSystem.shouldStop(entity: Entity): Boolean {
                val context = entity[ContextComponent]
                if (!everWasFree) {
                    everWasFree = !context.touchingLeftWall && !context.touchingRightWall
                }
                holdJumpFor -= deltaTime
                wasJumping = wasJumping || !context.standing
                val stoppedJumping = wasJumping && context.standing
                val result = context.swimming || stoppedJumping
                if (result && !everWasFree) {
                    onNeverWasFree?.invoke()
                }
                return result
            }

        }

    }
}