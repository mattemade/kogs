package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import net.mattemade.platformer.resources.ResourceEnemy
import net.mattemade.platformer.system.EnemyBehaviourSystem.Intent
import org.jbox2d.common.Vec2
import kotlin.math.sign
import kotlin.random.Random

class EnemyComponent(
    val enemy: ResourceEnemy,
    var currentIntent: Intent? = null,
    var spottedPlayerPosition: Vec2? = null,
    var nearPlayer: Vec2? = null,
    val superAggressive: Boolean = false,
): Component<EnemyComponent> {
    override fun type() = EnemyComponent
    companion object: ComponentType<EnemyComponent>()

    val nextIntent: IteratingSystem.(entity: Entity) -> Intent =
        when (enemy.name) {
            // what a syntax!!
            "bat" -> { {this.createBatSequence(it)} }
            "crab" -> { {this.createRandomCrabIntent(it)} }
            "snake" -> { {this.createSnakeSequence(it)} }
            "cat" -> { {this.createCatSequence(it)} }
            "wildcat" -> { {this.createCatSequence(it)} }
            "wildcat2" -> { {this.createCatSequence(it)} }
            "eel" -> { {this.createEelSequence(it)} }
            "copydora" -> { {this.createCopydoraSequence(it)} }
            "jellyfish" -> { {this.createJellyfishSequence(it)} }
            "boss" -> { {this.createCatSequence(it)} }
            else -> { { Intent.Idle(50000000f) } }
        }

    private var counter: Int = 0

    private fun IteratingSystem.createEelSequence(entity: Entity): Intent =
        when (Random.nextInt(4)) {
            0, 1, 2 -> Intent.Move(0.4f * (Random.nextFloat() - 0.5f).sign, 0f, limit = 0.5f + Random.nextFloat() * 2f, swim = true)
            else -> Intent.Idle(0.4f + Random.nextFloat() * 2f)
        }

    private fun IteratingSystem.createCopydoraSequence(entity: Entity): Intent =
        Intent.Move(-0.2f, 0f, limit = Float.MAX_VALUE, swim = true)

    private fun IteratingSystem.createJellyfishSequence(entity: Entity): Intent =
        Intent.FlyInSine(x = 0f, dy = 0.4f, timeScale = 2f)

    private fun IteratingSystem.createBatSequence(entity: Entity): Intent =
        when (counter++) {
            0 -> {
                entity[Box2DPhysicsComponent].gravityScaleOverride = 0f
                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    Intent.WaitForPlayerAppear()
                }

            }
            1 -> {
                val playerPosition = spottedPlayerPosition
                val batPosition = entity[Box2DPhysicsComponent].body.position
                Intent.FlyTo( { (playerPosition?.y ?: 1000f) - 3f }, dy = 10f) {
                    Intent.FlyInSine(x = 3f * ((playerPosition?.x ?: 0f) - batPosition.x).sign, dy = 12f)
                }
            }
            else -> {
                spottedPlayerPosition = null
                counter = 1

                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    Intent.WaitForPlayerAppear()
                }
            }
        }

    private fun IteratingSystem.createCatSequence(entity: Entity): Intent =
        when (counter++) {
            0 -> {
                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    Intent.WaitForPlayerAppear()
                }
            }
            1 -> {
                val playerPosition = spottedPlayerPosition
                val playerX = playerPosition?.x ?: 0f
                val catPosition = entity[Box2DPhysicsComponent].body.position
                val catX = catPosition.x
                Intent.Move(4f * (playerX - catX).sign, 0f, limit = 5f, jumpOnWalls = 0.05f, fallFromEdges = true, triggerProximity = {
                    //(it as? Intent.Move)?.let { it.x = -it.x }
                    Intent.JumpForward(8f * ((playerPosition?.x ?: 0f) - catPosition.x).sign, 0f, holdJumpFor = 0.025f, onEnd = it)
                })
                //Intent.JumpForward(6f * (playerX - catX).sign, 0f, holdJumpFor = 0.025f)
            }
            else -> {
                counter = 1

                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    spottedPlayerPosition = null
                    Intent.WaitForPlayerAppear()
                }
            }
        }

    private fun IteratingSystem.createSnakeSequence(entity: Entity): Intent =
        when (counter++) {
            0 -> {
                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    Intent.WaitForPlayerComeClose()
                }
            }
            1 -> {
                val playerX = nearPlayer?.x ?: 0f
                val snakeX = entity[Box2DPhysicsComponent].body.position.x
                Intent.JumpForward(6f * (playerX - snakeX).sign, 0f, holdJumpFor = 0.025f)
            }
            2 -> {
                val playerPosition = nearPlayer
                val playerX = playerPosition?.x ?: 0f
                val snakePosition = entity[Box2DPhysicsComponent].body.position
                val snakeX = snakePosition.x
                nearPlayer = null
                Intent.Move(6f * (snakeX - playerX).sign, 0f, limit = 2f, fallFromEdges = true, triggerProximity = {
                    Intent.JumpForward(6f * ((playerPosition?.x ?: 0f) - snakePosition.x).sign, 0f, holdJumpFor = 0.025f, onEnd = it)
                })
            }
            else -> {
                counter = 1

                if (superAggressive) {
                    Intent.Idle(0f)
                } else {
                    Intent.WaitForPlayerComeClose()
                }
            }
        }

    private fun IteratingSystem.createRandomCrabIntent(entity: Entity): Intent =
        when (Random.nextInt(2)) {
            0 -> Intent.Move(1f * (Random.nextFloat() - 0.5f).sign, 0f, limit = 1f + Random.nextFloat() * 3f)
            else -> Intent.Idle(0.5f + Random.nextFloat() * 3f)
        }
}