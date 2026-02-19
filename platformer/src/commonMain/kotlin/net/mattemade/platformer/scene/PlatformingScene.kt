package net.mattemade.platformer.scene

import com.github.quillraven.fleks.Entity
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PhysicsComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.world.Room
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import org.jbox2d.common.Vec2

class PlatformingScene(val gameContext: PlatformerGameContext) : Scene, Releasing by Self() {

    private val rooms = gameContext.assets.resourceSheet.levels.map {
        Room(
            map = gameContext.assets.levels.map[it.file]!!,
            gameContext = gameContext,
            worldArea = it.worldArea,
            name = it.file,
            switchRoom = ::switchRoom,
        ).releasing()
    }

    var currentRoom: Room = rooms[1]

    override fun update(seconds: Float) {
        currentRoom.render(seconds)
    }

    override fun render(
        batch: Batch, shapeRenderer: ShapeRenderer
    ) {

    }


    private fun switchRoom(player: Entity) {
        currentRoom.apply {
            ecs.apply {
                val playerPosition = player[PositionComponent].position
                tempVec2f.set(
                    worldArea.x + playerPosition.x,
                    worldArea.y + playerPosition.y,
                )
                rooms.forEach {
                    if (it.worldArea.contains(tempVec2f)) {
                        val worldPositionDiff =
                            Vec2(it.worldArea.x - currentRoom.worldArea.x, it.worldArea.y - currentRoom.worldArea.y)
                        // TODO: how to do that tidy, without exposing too much of Player outside of ECS?
                        player[PositionComponent].position.set(
                            playerPosition.x - worldPositionDiff.x,
                            playerPosition.y - worldPositionDiff.y,
                        )

                        it.enter(
                            player[SpriteComponent],
                            player[PositionComponent],
                            player[MoveComponent],
                            player[JumpComponent],
                            player[PhysicsComponent], // just to copy velocity and stuff
                        )
                        //player.remove()
                        currentRoom = it
                        return
                    }
                }
                throw IllegalStateException("No room found at world coordinates (${tempVec2f.x}, ${tempVec2f.y}); moving from ${currentRoom.name} with bounds (${currentRoom.worldArea}), local coordinates: (${playerPosition.x}, ${playerPosition.y})")
            }
        }
    }

    companion object {
        private val tempVec2f = MutableVec2f()
    }
}
