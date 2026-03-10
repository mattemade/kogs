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
): Component<EnemyComponent> {
    override fun type() = EnemyComponent
    companion object: ComponentType<EnemyComponent>()

    val nextIntent: IteratingSystem.(entity: Entity) -> Intent =
        when (enemy.name) {
            // what a syntax!!
            "crab" -> { {this.createRandomCrabIntent(it)} }
            "snake" -> { {this.createSnakeSequence(it)} }
            "cat" -> { {this.createCatSequence(it)} }
            "wildcat" -> { {this.createCatSequence(it)} }
            "wildcat2" -> { {this.createCatSequence(it)} }
            else -> { { Intent.Idle(50000000f) } }
        }

    private var counter: Int = 0


    private fun IteratingSystem.createCatSequence(entity: Entity): Intent =
        when (counter++) {
            0 -> Intent.WaitForPlayerAppear()
            1 -> {
                val playerPosition = spottedPlayerPosition
                val playerX = playerPosition?.x ?: 0f
                val catPosition = entity[Box2DPhysicsComponent].body.position
                val catX = catPosition.x
                Intent.Move(4f * (playerX - catX).sign, 0f, limit = 5f, jumpOnWalls = 0.05f, fallFromEdges = true, triggerProximity = {
                    //(it as? Intent.Move)?.let { it.x = -it.x }
                    Intent.JumpForward(8f * ((playerPosition?.x ?: 0f) - catPosition.x).sign, -0.1f, holdJumpFor = 0.025f, onEnd = it)
                })
                //Intent.JumpForward(6f * (playerX - catX).sign, 0f, holdJumpFor = 0.025f)
            }
            else -> {
                spottedPlayerPosition = null
                counter = 1
                Intent.WaitForPlayerAppear()
            }
        }

    private fun IteratingSystem.createSnakeSequence(entity: Entity): Intent =
        when (counter++) {
            0 -> Intent.WaitForPlayerComeClose()
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
                Intent.WaitForPlayerComeClose()
            }
        }

    private fun IteratingSystem.createRandomCrabIntent(entity: Entity): Intent =
        when (Random.nextInt(2)) {
            0 -> Intent.Move(1f * (Random.nextFloat() - 0.5f).sign, 0f, limit = 1f + Random.nextFloat() * 3f)
            else -> Intent.Idle(0.5f + Random.nextFloat() * 3f)
        }
}