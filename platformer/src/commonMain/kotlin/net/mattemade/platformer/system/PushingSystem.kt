package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.Interval
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.PositionComponent

class PushingSystem(
    val push: Array<Array<BooleanArray>>,
    val water: Array<BooleanArray>,
    interval: Interval = Fixed(1 / 200f),
) : IteratingSystem(family { all(PositionComponent, MomentaryForceComponent) }, interval = interval) {

    override fun onTickEntity(entity: Entity) {
        val position = entity[PositionComponent].position
        val x1 = position.x.floorToInt()
        val x2 = if (position.x % 1f >= 0.5f) x1 + 1 else x1 - 1
        val y1 = position.y.floorToInt()
        val y2 = if (position.y % 1f >= 0.5f) y1 + 1 else y1 - 1

        directions.forEachIndexed { index, direction ->
            val pushInDirection = push[index]
            checkDirection(pushInDirection, x1, y1, entity, index, direction)
            checkDirection(pushInDirection, x1, y2, entity, index, direction)
            checkDirection(pushInDirection, x2, y1, entity, index, direction)
            checkDirection(pushInDirection, x2, y2, entity, index, direction)
            /*if (pushInDirection.getOrNull(x1)?.getOrNull(y1) == true
                || pushInDirection.getOrNull(x1)?.getOrNull(y2) == true
                || pushInDirection.getOrNull(x2)?.getOrNull(y1) == true
                || pushInDirection.getOrNull(x2)?.getOrNull(y2) == true) {
                entity[MomentaryForceComponent].forces.add(direction)
            }*/
        }
    }

    private fun checkDirection(
        pushInDirection: Array<BooleanArray>,
        x: Int,
        y: Int,
        entity: Entity,
        index: Int,
        direction: Vec2f
    ) {
        if (pushInDirection.getOrNull(x)?.getOrNull(y) == true) {
            if (water[x][y]) {
                entity[MomentaryForceComponent].forces.add(waterDirections[index])
            } else {
                entity[MomentaryForceComponent].forces.add(direction)
            }
        }
    }

    companion object {
        val directions = listOf(
            Vec2f(-2f, 0f),
            Vec2f(2f, 0f),
            Vec2f(0f, -2f),
            Vec2f(0f, 2f),
        )
        val waterDirections = listOf(
            Vec2f(-0.25f, 0f),
            Vec2f(0.25f, 0f),
            Vec2f(0f, -0.25f),
            Vec2f(0f, 0.25f),
        )
    }
}