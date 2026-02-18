package net.mattemade.platformer.world

import com.github.quillraven.fleks.configureWorld
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.littlekt.math.Rect
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PhysicsComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.SpriteComponent
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

class Room(val gameContext: PlatformerGameContext, private val map: TiledMap): Releasing by Self() {

    private val initialPlayerBounds = (map.layer("player-spawn") as TiledObjectLayer).objects.first().bounds
    private val unitSize = 1f / map.tileWidth
    private val solidMap = Array(map.width) { BooleanArray(map.height) }

    private val physics: B2dWorld = B2dWorld().rememberTo {
        var body = it.bodyList
        while (body != null) {
            it.destroyBody(body)
            body = body.getNext()
        }
    }

    private val ecs = configureWorld {
        injectables {
            add(physics)
            add(gameContext)
            add(gameContext.context)
            add(map)
        }
        systems {
            add(ControlsSystem())
            add(PhysicsSystem())
            add(RenderingSystem())
        }

    }.rememberTo {

    }

    init {
        ecs.entity {
            it += SpriteComponent(gameContext.assets.textureFiles.whitePixel, Rect(0f, 0f, initialPlayerBounds.width * unitSize, initialPlayerBounds.height * unitSize))
            it += PositionComponent().also { it.position.set(initialPlayerBounds.x * unitSize, initialPlayerBounds.y * unitSize) }
            it += MoveComponent()
            it += JumpComponent()
            it += PhysicsComponent(
                body = physics.createBody(BodyDef().apply {
                    type = BodyType.DYNAMIC
                    position.set(initialPlayerBounds.x * unitSize, initialPlayerBounds.y * unitSize)
                    gravityScale = 4f
                }).apply {
                    createFixture(FixtureDef().apply {
                        friction = 0f
                        shape = PolygonShape().apply {
                            setAsBox(initialPlayerBounds.width * 0.5f * unitSize * 0.9f, initialPlayerBounds.height * 0.5f * unitSize * 0.9f)
                        }
                    })
                }
            )
        }

        var solidTileId = 0
        map.tileSets.forEach { tileset ->
            tileset.tiles.forEach { tile ->
                if (tile.objectGroup?.objects?.any { it.name == "solid" } == true) {
                    solidTileId = tile.id
                    return@forEach
                }
            }
        }

        map.layers.forEach { layer ->
            if (layer is TiledTilesLayer) {
                for (x in 0..<map.width) {
                    for (y in 0..<map.height) {
                        solidMap[x][y] = solidMap[x][y] or (layer.getTileId(x, y) == solidTileId)
                    }
                }
            }
        }

        solidMap.findBounds(object: BoundsListener {
            val accumulatedVertices = mutableListOf<Vec2>()

            override fun startPath() = accumulatedVertices.clear()


            override fun addPoint(x: Float, y: Float) {
                accumulatedVertices += Vec2(x-0.5f, y-1f)
            }

            override fun endPath() {
                physics.createBody(BodyDef()).apply {
                    createFixture(FixtureDef().apply {
                        shape = ChainShape().apply {
                            createLoop(accumulatedVertices.toTypedArray(), accumulatedVertices.size)
                        }
                    })
                }
            }
        })
    }

    fun render(dt: Float) {
        ecs.update(dt)
    }

    fun reset() {

    }
}