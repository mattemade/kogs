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
import net.mattemade.platformer.component.PhysicsComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.system.ControlsSystem
import net.mattemade.platformer.system.PhysicsSystem
import net.mattemade.platformer.system.RenderingSystem
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.tiled.BoundsListener
import net.mattemade.utils.tiled.findBounds
import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World as B2dWorld

class Room(
    private val gameContext: PlatformerGameContext,
    private val map: TiledMap,
    val worldArea: Rect,
    val name: String,
    private val switchRoom: (player: Entity) -> Unit,
) : Releasing by Self() {

    private val unitSize = 1f / map.tileWidth
    private val initialPlayerBounds = (map.layer("player-spawn") as TiledObjectLayer).objects.first().bounds.let {
        Rect(
            x = it.x * unitSize,
            y = (it.y - it.height) * unitSize,
            width = it.width * unitSize,
            height = it.height * unitSize,
        )
    }

    private val solidMap = Array(map.width) { BooleanArray(map.height) }

    private val physics: B2dWorld = B2dWorld().rememberTo {
        var body = it.bodyList
        while (body != null) {
            it.destroyBody(body)
            body = body.getNext()
        }
    }

    private val testPolygons = mutableListOf<FloatArrayList>()

    val ecs = configureWorld {
        injectables {
            add(physics)
            add(gameContext)
            add(gameContext.context)
            add(map)
        }
        systems {
            add(ControlsSystem())
            add(PhysicsSystem())
            add(RenderingSystem(testPolygons = testPolygons))
        }

    }.rememberTo {

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
        it += PhysicsComponent(
            body = physics.createBody(BodyDef().apply {
                type = BodyType.DYNAMIC
                position.set(initialPlayerBounds.cx, initialPlayerBounds.cy)
                gravityScale = 12f
            }).apply {
                createFixture(FixtureDef().apply {
                    friction = 0f
                    shape = PolygonShape().apply {
                        setAsBox(
                            initialPlayerBounds.width * 0.5f * 0.9f,
                            initialPlayerBounds.height * 0.5f * 0.9f
                        )
                    }
                })
            },
        )
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
                physics.createBody(BodyDef()).apply {
                    createFixture(FixtureDef().apply {
                        shape = ChainShape().apply {
                            createLoop(accumulatedVertices.toTypedArray(), accumulatedVertices.size)
                        }
                    })
                }

                /*testPolygons += FloatArrayList(capacity = accumulatedVertices.size * 2).apply {
                    accumulatedVertices.forEachIndexed { index, vec2 ->
                        set(index*2, vec2.x)
                        set(index*2 + 1, vec2.y)
                    }
                }*/
            }
        })
    }


    fun enter(
        spriteComponent: SpriteComponent,
        positionComponent: PositionComponent,
        moveComponent: MoveComponent,
        jumpComponent: JumpComponent,
        physicsComponent: PhysicsComponent
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
            playerEntity[PhysicsComponent].apply {
                previousPosition.set(playerPosition)
                body.setTransformDegrees(Vec2(playerPosition.x, playerPosition.y), 0f)
                body.linearVelocityX = physicsComponent.body.linearVelocityX
                body.linearVelocityY = physicsComponent.body.linearVelocityY
            }
        }
    }

    fun render(dt: Float) {
        ecs.update(dt)

        // TODO: these padding should really be half of body size, but in this case we need to compensate that when switching rooms to not stuck in the wall right after the entrance
        if (playerPosition.x < -ROOM_TELEPORT_HORIZONTAL_PADDING || playerPosition.y < -ROOM_TELEPORT_VERTICAL_PADDING || playerPosition.x > worldArea.width - ROOM_TELEPORT_HORIZONTAL_PADDING || playerPosition.y > worldArea.height - ROOM_TELEPORT_VERTICAL_PADDING) {
            switchRoom(playerEntity)
        }

    }

    fun reset() {

    }

    companion object {
        const val ROOM_TELEPORT_HORIZONTAL_PADDING = 0f//0.5f
        const val ROOM_TELEPORT_VERTICAL_PADDING = 0f
    }
}