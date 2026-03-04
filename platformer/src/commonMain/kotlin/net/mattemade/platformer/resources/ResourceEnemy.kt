package net.mattemade.platformer.resources

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.littlekt.graphics.Color
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.system.Box2DPhysicsSystem

class ResourceEnemy(
    val name: String,
    val idleName: String,
    val walkName: String = idleName,
    val jumpName: String = idleName,
    val fallName: String = idleName,
    val swimName: String = idleName,
    val wallSlideName: String = idleName,
    val size: Vec2f = Vec2f(0.98f, 0.98f),
) {

    private val halfSize = Vec2f(size.x * 0.5f, size.y * 0.5f)

    fun EntityCreateContext.createEnemy(
        gameContext: PlatformerGameContext,
        entity: Entity,
        physicsSystem: Box2DPhysicsSystem,
        cx: Float,
        cy: Float
    ) {
        entity += SpriteComponent(
            idleAnimation = gameContext.assets.animation(idleName),
            walkAnimation = gameContext.assets.animation(walkName),
            jumpAnimation = gameContext.assets.animation(jumpName),
            fallAnimation = gameContext.assets.animation(fallName),
            swimAnimation = gameContext.assets.animation(swimName),
            wallSlideAnimation = gameContext.assets.animation(wallSlideName),
            animationEventCallback = { it, _ -> println(it) },
            // baking offset into the bounds, maybe it should be a separate property?
            bounds = Rect(
                -halfSize.x,
                -halfSize.y,
                size.x,
                size.y
            ),
            tint = Color.Companion.RED.toMutableColor().apply { a = 0.2f }.toFloatBits(),
            priority = 0,
        )
        entity += PositionComponent().also {
            it.position.set(cx, cy)
        }
        entity += RotationComponent(maxRotationVelocity = 0.1f)
        entity += MoveComponent()
        entity += JumpComponent()
        entity += FloatUpComponent()
        entity += MomentaryForceComponent()
        entity += ContextComponent()
        physicsSystem.createEnemyBody(
            this,
            entity,
            cx,
            cy,
            size.x,
            size.y,
        )
    }

}