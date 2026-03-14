package net.mattemade.platformer.scene

import com.github.quillraven.fleks.Entity
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.util.seconds
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.InvincibilityComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.StaminaDamageComponent
import net.mattemade.platformer.component.StoryComponent
import net.mattemade.platformer.system.UiRenderingSystem
import net.mattemade.platformer.world.Room
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import org.jbox2d.common.Vec2
import kotlin.math.roundToInt

class PlatformingScene(val gameContext: PlatformerGameContext) : Scene, Releasing by Self() {

    private val mapSize = gameContext.worldSize
    private val rooms = gameContext.assets.resourceSheet.levelByName.map { (key, it) ->
        if (it.visibleOnMap) {
            // x2/y2 are dynamically calculated based on x/y, so we need to cache them beforehand
            val currentX2 = mapSize.x2
            val currentY2 = mapSize.y2
            mapSize.x = minOf(mapSize.x, it.worldArea.x)
            mapSize.y = minOf(mapSize.y, it.worldArea.y)
            mapSize.x2 = maxOf(currentX2, it.worldArea.x2)
            mapSize.y2 = maxOf(currentY2, it.worldArea.y2)

            totalRooms++
        }
        Room(
            map = gameContext.assets.levels.map[it.file]!!,
            gameContext = gameContext,
            worldArea = it.worldArea,
            name = it.file,
            visibleOnMap = it.visibleOnMap,
            mapSize = mapSize,
            switchRoom = ::switchRoom,
        ).releasing()
    }
    private var currentRoom: Room = getCurrentRoom()

    private fun getCurrentRoom(): Room =
        rooms.firstOrNull { it.name == gameContext.gameState.currentRoom } ?: rooms.first().also {
            // we can't find the room that was saved as the current one! it might be that the save state is corrupted
            gameContext.load(reset = true)
        }

    private var initialMapDraw: Boolean = true
    private val sharedMapRenderer = PixelRender(
        gameContext.context,
        targetWidth = mapSize.width.roundToInt(),
        targetHeight = mapSize.height.roundToInt(),
        virtualWidth = mapSize.width,
        virtualHeight = mapSize.height,
        preRenderCall = { dt, camera ->
            camera.position.set(mapSize.cx, mapSize.cy, 0f)
        },
        renderCall = { dt, camera, batch, shapeRenderer ->
            if (initialMapDraw) {
                gameContext.context.gl.clearColor(Color.CLEAR)
                gameContext.context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
                rooms.forEach {
                    if (gameContext.gameState.roomStates[it.name]?.isVisited == true) {
                        addRoomToMap(it, shapeRenderer)
                    }
                }
                initialMapDraw = false
            }
            addRoomToMap(currentRoom, shapeRenderer)
        }
    ).apply {
        gameContext.gameState.roomStates.getOrPut(currentRoom.name) { PlatformerGameContext.RoomState() }.isVisited = true
        render(0f.seconds)
        texture.also { mapTexture ->
            rooms.forEach { it.mapTexture = mapTexture }
        }
    }

    init {
        updateCollectionCounter()
    }

    private fun addRoomToMap(room: Room, shapeRenderer: ShapeRenderer) {
        if (!room.addedToMap && room.visibleOnMap) {
            room.tileTypeMap["water"]?.forEachIndexed { x, row ->
                row.forEachIndexed { y, value ->
                    if (value) {
                        shapeRenderer.filledRectangle(
                            x = room.worldArea.x + x.toFloat(),
                            y = room.worldArea.y + y.toFloat(),
                            width = 1f,
                            height = 1f,
                            color = mapWaterColor,
                        )
                    }
                }
            }
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
            room.tileTypeMap["fake"]?.forEachIndexed { x, row ->
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
            room.addedToMap = true
        }
    }

    fun reset() {
        nextPearlId = 0
        collectedPearls = 0
        UiRenderingSystem.collectionText = null
        nextCheckpointId = 0
        rooms.forEach {
            it.reset(full = true)
        }
        currentRoom = getCurrentRoom()
        initialMapDraw = true
        sharedMapRenderer.render(0f.seconds)
    }

    private fun updateCollectionCounter() {
        visitedRooms = rooms.count { it.visibleOnMap && gameContext.gameState.roomStates[it.name]?.isVisited == true }
        collectedPearls = gameContext.gameState.pearls.count { it }
    }

    override fun update(seconds: Float) {
        currentRoom.render(seconds)
    }

    override fun render(
        batch: Batch, shapeRenderer: ShapeRenderer
    ) {

    }


    private fun switchRoom(player: Entity, offsetX: Float?, offsetY: Float?, dx: Float, dy: Float, forceLeave: Boolean) {
        println("switch $offsetX $offsetY $dx $dy")
        currentRoom.apply {
            ecs.apply {
                // TODO: really? maybe all of that should be arguments?
                val playerPosition = player[PositionComponent].position
                tempVec2f.set(
                    worldArea.x + playerPosition.x + (offsetX ?: 0f),
                    worldArea.y + playerPosition.y + (offsetY ?: 0f),
                )
                println("probe ${tempVec2f}")
                // TODO: is there a way to do that better than O(N)? maybe we can prepare a world graph ahead of time
                rooms.forEach {
                    if ((!forceLeave || it.name != currentRoom.name) && it.worldArea.contains(tempVec2f)) {
                        val worldPositionDiff =
                            Vec2(it.worldArea.x - currentRoom.worldArea.x, it.worldArea.y - currentRoom.worldArea.y)
                        // TODO: how to do that tidy, without exposing too much of Player outside of ECS?
                        // translate player's position to the new room's local coordinates
                        player[PositionComponent].position.set(
                            playerPosition.x + (offsetX ?: 0f) - worldPositionDiff.x,
                            playerPosition.y + (offsetY ?: 0f) - worldPositionDiff.y,
                        )

                        it.enter(
                            player[SpriteComponent],
                            player[PositionComponent],
                            player[RotationComponent],
                            player[MoveComponent],
                            player[JumpComponent],
                            player[AttackComponent],
                            player[FloatUpComponent],
                            player[ContextComponent],
                            player[HealthComponent],
                            player[StaminaComponent],
                            player[StaminaDamageComponent],
                            player.getOrNull(InvincibilityComponent),
                            player[StoryComponent],
                            player[Box2DPhysicsComponent], // just to copy velocity and stuff, SHOULD NOT BE REUSED THERE as it's connected to the Room's B2D World
                        )
                        currentRoom = it
                        gameContext.gameState.roomStates.getOrPut(it.name) { PlatformerGameContext.RoomState() }.isVisited = true
                        gameContext.gameState.currentRoom = it.name
                        sharedMapRenderer.render(0f.seconds)
                        updateCollectionCounter()
                        return
                    }
                }
                if (offsetX == null && offsetY == null) {
                    if (dx != 0f) {
                        switchRoom(player, if (dx > 0) mapSize.x - tempVec2f.x + dx else mapSize.x2 - tempVec2f.x + dx, offsetY, dx, dy, forceLeave)
                    } else if (dy != 0f) {
                        switchRoom(player, offsetX, if (dy > 0f) mapSize.y - tempVec2f.y + dy else mapSize.y2 - tempVec2f.y + dy, dx, dy, forceLeave)
                    } else {
                        throw IllegalStateException("No room found at world coordinates (${tempVec2f.x}, ${tempVec2f.y}); moving from ${currentRoom.name} with bounds (${currentRoom.worldArea}), local coordinates: (${playerPosition.x}, ${playerPosition.y})")
                    }
                } else {
                    if (mapSize.contains(tempVec2f)) {
                        switchRoom(player, (offsetX ?: 0f) + dx, (offsetY ?: 0f) + dy, dx, dy, forceLeave)
                    } else {
                        throw IllegalStateException("No room found at world coordinates (${tempVec2f.x}, ${tempVec2f.y}); moving from ${currentRoom.name} with bounds (${currentRoom.worldArea}), local coordinates: (${playerPosition.x}, ${playerPosition.y})")
                    }
                }
            }
        }
    }

    companion object {
        private val tempVec2f = MutableVec2f()
        private val mapColor = Color.WHITE.toMutableColor().apply { a = 1f }.toFloatBits()
        private val mapWaterColor = Color.CYAN.toMutableColor().apply { a = 0.75f }.toFloatBits()
        var nextCheckpointId = 0
        var nextPearlId = 0
        var collectedPearls = 0
        var totalRooms = 0
        var visitedRooms = 0
    }
}
