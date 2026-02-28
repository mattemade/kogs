package net.mattemade.platformer.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.component.UiComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.system.Box2DPhysicsSystem
import net.mattemade.platformer.system.ControlsSystem
import net.mattemade.platformer.system.FloatingSystem
import net.mattemade.platformer.system.RenderingSystem
import net.mattemade.platformer.system.RotationSystem
import net.mattemade.platformer.system.UiControlsSystem
import net.mattemade.platformer.system.UiRenderingSystem
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
    private val initialPlayerBounds =
        (map.layer("player-spawn") as? TiledObjectLayer)?.objects?.firstOrNull()?.bounds?.let {
            Rect(
                x = it.cx * unitSize - 0.5f,
                y = it.cy * unitSize - 1f,
                width = 1f,
                height = 2f,
            )
        } ?: Rect()

    var mapTexture: Texture? = null
    var addedToMap: Boolean = false
    val tileTypeMap = listOf("solid", "platform", "water").associateWith {
        Array(map.width) { BooleanArray(map.height) }
    }

    private lateinit var physicsSystem: Box2DPhysicsSystem

    val ecs = configureWorld {
        injectables {
            add(gameContext)
            add(gameContext.context)
            add(map)
        }
        systems {
            add(ControlsSystem())
            add(UiControlsSystem())
            add(Box2DPhysicsSystem().also { physicsSystem = it }.releasing())
            add(FloatingSystem())
            add(RotationSystem())
            add(RenderingSystem())
            add(UiRenderingSystem(worldArea = worldArea, mapTexture = { mapTexture }))
        }
    }.apply {
        entity {
            it += UiComponent()
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
        it += RotationComponent(maxRotationVelocity = 0.1f)
        it += MoveComponent()
        it += JumpComponent()
        it += FloatUpComponent()
        it += MomentaryForceComponent()
        it += ContextComponent()
        it += PlayerComponent()
        physicsSystem.createPlayerBody(this, it, initialPlayerBounds)
    }

    init {
        val typedTileIds = tileTypeMap.keys.associateWith { mutableSetOf<Int>() }
        map.tileSets.forEach { tileset ->
            tileset.tiles.forEach { tile ->
                for (type in typedTileIds.keys) {
                    if (tile.objectGroup?.objects?.any { it.name == type } == true) {
                        typedTileIds[type]?.add(tile.id)
                    }
                }
            }
        }

        map.layers.forEach { layer ->
            if (layer is TiledTilesLayer) {
                for (x in 0 until map.width) {
                    for (y in 0 until map.height) {
                        for ((type, array) in tileTypeMap) {
                            array[x][y] = array[x][y] or (typedTileIds[type]?.contains(layer.getTileId(x, y)) == true)
                        }
                    }
                }
            }
        }

        tileTypeMap["solid"]?.findBounds(object : BoundsListener {
            val accumulatedVertices = mutableListOf<Vec2>()

            override fun startPath() = accumulatedVertices.clear()


            override fun addPoint(x: Float, y: Float) {
                accumulatedVertices += Vec2(x, y)
            }

            override fun endPath() {
                physicsSystem.createWall(accumulatedVertices.toTypedArray())
            }
        })

        tileTypeMap["platform"]?.let {
            for (y in 0 until map.height) {
                var followingPlatformFrom = -1
                for (x in 0 until map.width) {
                    if (it[x][y]) {
                        if (followingPlatformFrom == -1) {
                            followingPlatformFrom = x
                        }
                    } else if (followingPlatformFrom != -1) {
                        physicsSystem.createPlatform(followingPlatformFrom.toFloat(), x.toFloat(), y.toFloat())
                        followingPlatformFrom = -1
                    }
                }
                if (followingPlatformFrom != -1) {
                    physicsSystem.createPlatform(followingPlatformFrom.toFloat(), map.width.toFloat(), y.toFloat())
                }
            }
        }

        tileTypeMap["water"]?.let {
            for (x in 0 until map.width) {
                var followingWaterFrom = -1
                for (y in 0 until map.height) {
                    if (it[x][y]) {
                        if (followingWaterFrom == -1) {
                            followingWaterFrom = y
                        }
                    } else if (followingWaterFrom != -1) {
                        placeSwimmableWaterBlock(followingWaterFrom, y, x)
                        followingWaterFrom = -1
                    }
                }
                if (followingWaterFrom != -1) {
                    placeSwimmableWaterBlock(followingWaterFrom, map.height, x)
                }
            }
        }

        map.layers.forEach {
            if (it is TiledObjectLayer) {
                it.objects.forEach { spawn ->
                    when (spawn.name) {
                        "crab" -> {
                            ecs.entity {
                                it += SpriteComponent(
                                    gameContext.assets.textureFiles.whitePixel,
                                    // baking offset into the bounds, maybe it should be a separate property?
                                    Rect(
                                        spawn.bounds.width * unitSize * -0.48f,
                                        spawn.bounds.height * unitSize * -0.48f,
                                        spawn.bounds.width * unitSize,
                                        spawn.bounds.height * unitSize
                                    )
                                )
                                it += PositionComponent().also {
                                    it.position.set(spawn.bounds.cx * unitSize, spawn.bounds.cy * unitSize)
                                }
                                it += RotationComponent(maxRotationVelocity = 0.1f)
                                it += MoveComponent()
                                it += JumpComponent()
                                it += FloatUpComponent()
                                it += MomentaryForceComponent()
                                it += ContextComponent()
                                physicsSystem.createCrabBody(
                                    this,
                                    it,
                                    spawn.bounds.cx * unitSize,
                                    spawn.bounds.cy * unitSize,
                                    spawn.bounds.width * unitSize,
                                    spawn.bounds.height * unitSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun placeSwimmableWaterBlock(fromY: Int, toY: Int, x: Int) {
        if (toY - fromY == 1) {
            // if that's a single tile with a ground below it - don't make it swimmable
            // TODO: but make it splashable!
            if (tileTypeMap["solid"]?.getOrNull(x)?.getOrNull(toY) == true) {
                return
            }
        }
        physicsSystem.createWater(
            if (fromY == 0 || tileTypeMap["solid"]?.getOrNull(x)?.getOrNull(fromY - 1) == true) {
                fromY - 1.1f // to ensure water goes well above the screen, to make the person dive up
            } else {
                fromY.toFloat()
            }, toY.toFloat(), x.toFloat()
        )
    }


    fun enter(
        spriteComponent: SpriteComponent,
        positionComponent: PositionComponent,
        rotationComponent: RotationComponent,
        moveComponent: MoveComponent,
        jumpComponent: JumpComponent,
        floatUpComponent: FloatUpComponent,
        contextComponent: ContextComponent,
        physicsComponent: Box2DPhysicsComponent
    ) {
        ecs.apply {
            playerEntity.configure {
                // all the components are replaced
                it += spriteComponent
                it += positionComponent.also {
                    playerPosition = it.position
                }
                it += rotationComponent
                it += moveComponent
                it += jumpComponent
                it += floatUpComponent
                it += contextComponent
            }
            physicsSystem.teleport(playerEntity, playerPosition, physicsComponent)
            contextComponent.swimming = false // next room should switch body parameters for swimming if needed
        }
    }

    fun render(dt: Float) {
        ecs.update(dt)

        if (playerPosition.x < 0f || playerPosition.y < 0f || playerPosition.x > worldArea.width || playerPosition.y > worldArea.height) {
            switchRoom(playerEntity)
        }

    }
}
