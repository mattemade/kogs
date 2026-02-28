package net.mattemade.platformer.scene

import com.github.quillraven.fleks.Entity
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Rect
import com.littlekt.math.floorToInt
import com.littlekt.util.seconds
import net.mattemade.platformer.FIRST_LEVEL_NAME
import net.mattemade.platformer.PIXEL_PER_UNIT_FLOAT
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.world.Room
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import org.jbox2d.common.Vec2
import kotlin.math.roundToInt

class PlatformingScene(val gameContext: PlatformerGameContext) : Scene, Releasing by Self() {

    private val fullWorldRect = gameContext.worldSize
    private val rooms = gameContext.assets.resourceSheet.levelByName.map { (key, it) ->
        fullWorldRect.x = minOf(fullWorldRect.x, it.worldArea.x)
        fullWorldRect.y = minOf(fullWorldRect.y, it.worldArea.y)
        fullWorldRect.x2 = maxOf(fullWorldRect.x2, it.worldArea.x2)
        fullWorldRect.y2 = maxOf(fullWorldRect.y2, it.worldArea.y2)
        Room(
            map = gameContext.assets.levels.map[it.file]!!,
            gameContext = gameContext,
            worldArea = it.worldArea,
            name = it.file,
            switchRoom = ::switchRoom,
        ).releasing()
    }
    val mapTexture = PixelRender(
        gameContext.context,
        targetWidth = fullWorldRect.width.roundToInt(),
        targetHeight = fullWorldRect.height.roundToInt(),
        virtualWidth = fullWorldRect.width,
        virtualHeight = fullWorldRect.height,
        preRenderCall = { dt, camera ->
            camera.position.set(fullWorldRect.cx, fullWorldRect.cy, 0f)
        },
        renderCall = { dt, camera, batch, shapeRenderer ->
            /*shapeRenderer.filledRectangle(
                rect = fullWorldRect,
                //color = Color.BLACK.toFloatBits()
            )*/
            rooms.forEach { room ->
                room.tileTypeMap["solid"]?.forEachIndexed { x, row ->
                    row.forEachIndexed { y, value ->
                        if (value) {
                            shapeRenderer.filledRectangle(
                                x = room.worldArea.x + x.toFloat(),
                                y = room.worldArea.y + y.toFloat(),
                                width = 1f,
                                height = 1f,
                                color = mapColor,
                            )
                        }
                    }
                }
            }
        }
    ).run {
        render(0f.seconds)
        texture.also { mapTexture ->
            rooms.forEach { it.mapTexture = mapTexture }
        }
    }

    private var currentRoom: Room = rooms.first { it.name == FIRST_LEVEL_NAME }

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
                // TODO: really? maybe all of that should be arguments?
                val playerPosition = player[PositionComponent].position
                tempVec2f.set(
                    worldArea.x + playerPosition.x,
                    worldArea.y + playerPosition.y,
                )
                // TODO: is there a way to do that better than O(N)? maybe we can prepare a world graph ahead of time
                rooms.forEach {
                    if (it.worldArea.contains(tempVec2f)) {
                        val worldPositionDiff =
                            Vec2(it.worldArea.x - currentRoom.worldArea.x, it.worldArea.y - currentRoom.worldArea.y)
                        // TODO: how to do that tidy, without exposing too much of Player outside of ECS?
                        // translate player's position to the new room's local coordinates
                        player[PositionComponent].position.set(
                            playerPosition.x - worldPositionDiff.x,
                            playerPosition.y - worldPositionDiff.y,
                        )

                        it.enter(
                            player[SpriteComponent],
                            player[PositionComponent],
                            player[RotationComponent],
                            player[MoveComponent],
                            player[JumpComponent],
                            player[ContextComponent],
                            player[Box2DPhysicsComponent], // just to copy velocity and stuff, SHOULD NOT BE REUSED THERE as it's connected to the Room's B2D World
                        )
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
        private val mapColor = Color.WHITE.toMutableColor().apply { a = 1f }.toFloatBits()
    }
}
