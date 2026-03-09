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
            enemyComponent.currentIntent = Intent.Move(2f, 0f)
            /*val context = entity[ContextComponent]
            val moveComponent = entity[MoveComponent]
            if (context.swimming) {
                moveComponent.moveDirection.set(0f, 2f)
            } else if (context.standing) {
                moveComponent.moveDirection.set(2f, 0f)
            } else {
                moveComponent.moveDirection.set(0f, 0f)
            }*/
        }


        when (val intent = enemyComponent.currentIntent) {
            is Intent.Move -> entity[MoveComponent].moveDirection.set(intent.x, intent.y)
            null -> { /* should be impossible at this moment */}
        }


        //println(entity[EnemyComponent].enemy.name)
    }


    sealed interface Intent {

        fun EntityComponentContext.switch(entity: Entity): Intent?

        fun EntityComponentContext.shouldStop(entity: Entity): Boolean

        class Move(var x: Float, var y: Float): Intent {

            override fun EntityComponentContext.switch(entity: Entity): Intent? {
                val context = entity[ContextComponent]
                return if (shouldStop(entity)) {
                    null
                } else if (context.standing) {
                    if ((!context.standingRightFoot || context.touchingRightWall) && x > 0f || (!context.standingLeftFoot || context.touchingLeftWall) && x < 0f) {
                        x = -x
                    }
                    this@Move
                } else {
                    this@Move
                }

            }


            override fun EntityComponentContext.shouldStop(entity: Entity): Boolean =
                entity[ContextComponent].swimming

        }

    }
}