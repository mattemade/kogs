package net.mattemade.platformer.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import com.littlekt.util.datastructure.FloatArrayList
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.system.ControlsSystem
import net.mattemade.platformer.system.Box2DPhysicsSystem
import net.mattemade.platformer.system.RenderingSystem
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.tiled.BoundsListener
import net.mattemade.utils.tiled.findBounds
import org.jbox2d.common.Vec2

class Room(
    private val gameContext: PlatformerGameContext,
    private val map: TiledMap,
    val worldArea: Rect,
    val name: String,
    private val switchRoom: (player: Entity) -> Unit,
) : Releasing by Self() {

    private val unitSize = 1f / map.tileWidth
    private val initialPlayerBounds = (map.layer("player-spawn") as? TiledObjectLayer)?.objects?.firstOrNull()?.bounds?.let {
        Rect(
            x = it.cx * unitSize - 0.5f,
            y = it.cy * unitSize - 1f,
            width = 1f,
            height = 2f,
        )
    } ?: Rect()

    private val solidMap = Array(map.width) { BooleanArray(map.height) }

    private lateinit var physicsSystem: Box2DPhysicsSystem

    val ecs = configureWorld {
        injectables {
            add(gameContext)
            add(gameContext.context)
            add(map)
        }
        systems {
            add(ControlsSystem())
            add(Box2DPhysicsSystem().also { physicsSystem = it }.releasing())
            add(RenderingSystem())
        }
    }

    private lateinit var playerPosition: Vec2f
    private val playerEntity = ecs.entity {
        it += SpriteComponent(
            gameContext.assets.textureFiles.whitePixel,
            // baking offset into the bounds, maybe it should be a separate property?
            Rect(-0.45f.px, -0.9f.px, initialPlayerBounds.width * 0.91f, initialPlayerBounds.height * 0.91f)
        )
        it += PositionComponent().also {
            it.position.set(initialPlayerBounds.cx, initialPlayerBounds.cy)
            playerPosition = it.position
        }
        it += MoveComponent()
        it += JumpComponent()
        physicsSystem.createPlayerBody(this, it, initialPlayerBounds)
    }

    init {
        val solidTileIds = mutableSetOf<Int>()
        map.tileSets.forEach { tileset ->
            tileset.tiles.forEach { tile ->
                if (tile.objectGroup?.objects?.any { it.name == "solid" } == true) {
                    solidTileIds += tile.id
                    return@forEach
                }
            }
        }

        map.layers.forEach { layer ->
            if (layer is TiledTilesLayer) {
                for (x in 0..<map.width) {
                    for (y in 0..<map.height) {
                        solidMap[x][y] = solidMap[x][y] or (solidTileIds.contains(layer.getTileId(x, y)))
                    }
                }
            }
        }

        solidMap.findBounds(object : BoundsListener {
            val accumulatedVertices = mutableListOf<Vec2>()

            override fun startPath() = accumulatedVertices.clear()


            override fun addPoint(x: Float, y: Float) {
                accumulatedVertices += Vec2(x, y)
            }

            override fun endPath() {
                physicsSystem.createWall(accumulatedVertices.toTypedArray())
            }
        })
    }


    fun enter(
        spriteComponent: SpriteComponent,
        positionComponent: PositionComponent,
        moveComponent: MoveComponent,
        jumpComponent: JumpComponent,
        physicsComponent: Box2DPhysicsComponent
    ) {
        ecs.apply {
            playerEntity.configure {
                // all the components are replaced
                it += spriteComponent
                it += positionComponent.also {
                    playerPosition = it.position
                }
                it += moveComponent
                it += jumpComponent
            }
            physicsSystem.teleport(playerEntity, playerPosition, physicsComponent)
        }
    }

    fun render(dt: Float) {
        ecs.update(dt)

        if (playerPosition.x < 0f || playerPosition.y < 0f || playerPosition.x > worldArea.width || playerPosition.y > worldArea.height) {
            switchRoom(playerEntity)
        }

    }
}
