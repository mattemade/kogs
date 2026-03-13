package net.mattemade.platformer.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.configureWorld
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.Rect
import com.littlekt.math.Vec2f
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.WALK_VELOCITY
import net.mattemade.platformer.component.AttackComponent
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.CheckpointComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.FloatUpComponent
import net.mattemade.platformer.component.HealthComponent
import net.mattemade.platformer.component.InvincibilityComponent
import net.mattemade.platformer.component.JumpComponent
import net.mattemade.platformer.component.MascotComponent
import net.mattemade.platformer.component.MomentaryForceComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.PushableComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.component.StaminaComponent
import net.mattemade.platformer.component.StaminaDamageComponent
import net.mattemade.platformer.component.TimeToLiveComponent
import net.mattemade.platformer.component.UiComponent
import net.mattemade.platformer.px
import net.mattemade.platformer.scene.PlatformingScene
import net.mattemade.platformer.system.AttackSystem
import net.mattemade.platformer.system.Box2DPhysicsSystem
import net.mattemade.platformer.system.ControlsSystem
import net.mattemade.platformer.system.EnemyBehaviourSystem
import net.mattemade.platformer.system.EnemyDeathSystem
import net.mattemade.platformer.system.InvincibilitySystem
import net.mattemade.platformer.system.LoadOnPlayerDeathSystem
import net.mattemade.platformer.system.LowStaminaDamageSystem
import net.mattemade.platformer.system.MascotSystem
import net.mattemade.platformer.system.MusicSwitchingSystem
import net.mattemade.platformer.system.PushingSystem
import net.mattemade.platformer.system.RenderingSystem
import net.mattemade.platformer.system.RotationSystem
import net.mattemade.platformer.system.StaminaBreathingSystem
import net.mattemade.platformer.system.StaminaRestorationSystem
import net.mattemade.platformer.system.TimeToLiveSystem
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
    val visibleOnMap: Boolean,
    private val switchRoom: (player: Entity, offsetX: Float?, offsetY: Float?, dx: Float, dy: Float, forceLeave: Boolean) -> Unit,
    private val mapSize: Rect,
) : Releasing by Self() {

    private val unitSize = 1f / map.tileWidth
    private val initialPlayerBounds = Rect(x = 0f, y = 0f, width = 1f, height = 2f)

    var mapTexture: Texture? = null
    var addedToMap: Boolean = false
    var firstCheckpointIdInThisRoom: Int = -1
    var nextCheckpointId: Int = -1
    var incrementGlobalCheckpoints: Boolean = true
    val tileTypeMap =
        listOf(
            "solid",
            "platform",
            "water",
            "fake",
            "push-up",
            "push-down",
            "push-left",
            "push-right",
            "spike"
        ).associateWith {
            Array(map.width) { BooleanArray(map.height) }
        }
    val teleports = map.layers.mapNotNull {
        (it as? TiledObjectLayer)?.objects?.filter { it.name == "teleport" }?.map {
            Rect(
                it.bounds.x * unitSize,
                it.bounds.y * unitSize,
                it.bounds.width * unitSize,
                it.bounds.height * unitSize
            )
        }
    }.flatten()

    private lateinit var physicsSystem: Box2DPhysicsSystem

    val ecs = configureWorld {
        injectables {
            add(gameContext)
            add(gameContext.context)
            add(map)
        }
        systems {
            add(TimeToLiveSystem())
            add(ControlsSystem())
            add(UiControlsSystem())
            add(AttackSystem())
            add(
                PushingSystem(
                    arrayOf(
                        tileTypeMap["push-left"]!!,
                        tileTypeMap["push-right"]!!,
                        tileTypeMap["push-up"]!!,
                        tileTypeMap["push-down"]!!,
                    ),
                    tileTypeMap["water"]!!,
                )
            )
            add(InvincibilitySystem())
            add(EnemyBehaviourSystem())
            add(Box2DPhysicsSystem(::spawnPlayerAttack, Vec2f(worldArea.width, worldArea.height)).also { physicsSystem = it }.releasing())
            add(StaminaBreathingSystem())
            add(LowStaminaDamageSystem())
            add(StaminaRestorationSystem())
            add(LoadOnPlayerDeathSystem())
            add(EnemyDeathSystem())
            //add(FloatingSystem())
            add(RotationSystem())
            add(MascotSystem())
            add(MusicSwitchingSystem(musicType = (map.properties["music"] as? TiledMap.Property.StringProp)?.value))
            add(RenderingSystem())
            add(UiRenderingSystem(worldArea = worldArea, mapVisible = visibleOnMap, mapTexture = { mapTexture }))
        }
    }

    private lateinit var uiEntity: Entity
    private lateinit var playerPosition: Vec2f
    private lateinit var playerEntity: Entity
    //private lateinit var mascotEntity: Entity
    private var currentlyActiveCheckpointInThisRoom: Entity? = null

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

        tileTypeMap["solid"]?.let {
            tileTypeMap["fake"]?.let { fake ->
                for (x in 0 until map.width) {
                    for (y in 0 until map.height) {
                        if (fake[x][y]) {
                            it[x][y] = false
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

        tileTypeMap["spike"]?.findBounds(object : BoundsListener {
            val accumulatedVertices = mutableListOf<Vec2>()

            override fun startPath() = accumulatedVertices.clear()


            override fun addPoint(x: Float, y: Float) {
                accumulatedVertices += Vec2(x, y)
            }

            override fun endPath() {
                physicsSystem.createSpike(accumulatedVertices.toTypedArray())
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

        respawnEntities()
    }

    private fun respawnEntities(fullReset: Boolean = false) {
        if (firstCheckpointIdInThisRoom < 0 || fullReset) {
            firstCheckpointIdInThisRoom = PlatformingScene.nextCheckpointId
        }
        nextCheckpointId = firstCheckpointIdInThisRoom
        (map.layerOrNull("player-spawn") as? TiledObjectLayer)?.objects?.firstOrNull()?.bounds?.let {
            initialPlayerBounds.x = it.cx * unitSize - 0.5f
            initialPlayerBounds.y = it.cy * unitSize - 1f
        }
        currentlyActiveCheckpointInThisRoom = null
        ecs.removeAll(clearRecycled = false)
        uiEntity = ecs.entity {
            it += UiComponent()
        }

        incrementGlobalCheckpoints = incrementGlobalCheckpoints || fullReset
        map.layers.forEach {
            if (it is TiledObjectLayer) {
                it.objects.forEach { spawn ->
                    when (spawn.name) {
                        "checkpoint" -> {
                            createCheckpoint(
                                spawn,
                                tint = Color.BLUE.toFloatBits(),
                                tintActive = Color.CYAN.toFloatBits()
                            )
                        }
                    }
                }
            }
        }
        incrementGlobalCheckpoints = false // to not do it during room reset!

        playerEntity = ecs.entity {
            attachDress(it)
            it += PositionComponent().also {
                it.position.set(initialPlayerBounds.cx, initialPlayerBounds.cy)
                playerPosition = it.position
            }
            it += RotationComponent(maxRotationVelocity = 0.05f)
            it += MoveComponent(maxMoveSpeed = WALK_VELOCITY * 0.75f)
            it += JumpComponent()
            it += AttackComponent(
                specs = listOf(
                    AttackComponent.AttackSpec(shortCooldown = 0.3f, longCooldown = 0.6f, damage = 1f),
                    AttackComponent.AttackSpec(shortCooldown = 0.3f, longCooldown = 0.6f, damage = 1f),
                    AttackComponent.AttackSpec(shortCooldown = 0.6f, longCooldown = 1.2f, damage = 2f),
                )
            )
            it += FloatUpComponent()
            it += MomentaryForceComponent()
            it += ContextComponent()
            it += HealthComponent()
            it += StaminaComponent()
            it += StaminaDamageComponent()
            it += PushableComponent()
            it += PlayerComponent()
            physicsSystem.createPlayerBody(this, it, initialPlayerBounds)
        }
        /*mascotEntity = */ecs.entity {
            it += SpriteComponent(
                idleAnimation = gameContext.assets.animation("Dragon idle"),
                animationEventCallback = { it, _ -> println(it) },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(0f, 0f, 0f, 0f),
                tint = Color.GRAY.toMutableColor().apply { a = 0.2f }.toFloatBits(),
            )
            it += PositionComponent()
            it += RotationComponent()
            it += ContextComponent()
            it += MascotComponent(Vec2f(0f, 0f), playerEntity)
        }
        ecs.entity {
            it += SpriteComponent(
                idleAnimation = gameContext.assets.animation("Celestial mascot"),
                animationEventCallback = { it, _ -> println(it) },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(0f, 0f, 0f, 0f),
                tint = Color.GRAY.toMutableColor().apply { a = 0.2f }.toFloatBits(),
            )
            it += PositionComponent()
            it += RotationComponent()
            it += ContextComponent()
            it += MascotComponent(Vec2f(0.2f, 1f), playerEntity)
        }
        map.layers.forEach {
            if (it is TiledObjectLayer) {
                it.objects.forEach { spawn ->
                    gameContext.assets.resourceSheet.enemies[spawn.name]?.let { enemySpec ->
                        ecs.entity { entity ->
                            with(enemySpec) {
                                createEnemy(
                                    gameContext,
                                    entity,
                                    physicsSystem,
                                    spawn.bounds.cx * unitSize,
                                    spawn.bounds.cy * unitSize
                                )
                            }
                        }
                    } ?: run {

                    }
                    when (spawn.name) {
                        "water-pearl" -> {
                            if (!gameContext.gameState.waterPearl) {
                                createPickup(spawn, "Water pearl", tint = Color.BLUE.toFloatBits()) {
                                    gameContext.fmodAssets.pickUpgrade.createInstance().apply {
                                        start()
                                        release()
                                    }
                                    gameContext.gameState.waterPearl = true
                                    ecs.apply { playerEntity.configure { attachDress(it) } }
                                    gameContext.save()
                                }
                            }
                        }

                        "air-pearl" -> {
                            if (!gameContext.gameState.airPearl) {
                                createPickup(spawn, "Air pearl", tint = Color.GREEN.toFloatBits()) {
                                    gameContext.fmodAssets.pickUpgrade.createInstance().apply {
                                        start()
                                        release()
                                    }
                                    gameContext.gameState.airPearl = true
                                    ecs.apply { playerEntity.configure { attachDress(it) } }
                                    gameContext.save()
                                }
                            }
                        }

                        "sword" -> {
                            if (!gameContext.gameState.sword) {
                                createPickup(spawn, "Sword idle", tint = Color.YELLOW.toFloatBits()) {
                                    gameContext.fmodAssets.pickUpgrade.createInstance().apply {
                                        start()
                                        release()
                                    }
                                    gameContext.gameState.sword = true
                                    gameContext.save()
                                }
                            }
                        }

                        "pearl" -> {
                            val pearlId = PlatformingScene.nextPearlId++
                            while (gameContext.gameState.pearls.size <= pearlId) {
                                gameContext.gameState.pearls.add(false)
                            }
                            if (gameContext.gameState.pearls[pearlId]) {
                                PlatformingScene.collectedPearls++
                            } else {
                                createPickup(spawn, "Normal pearl", tint = Color.WHITE.toFloatBits()) {
                                    gameContext.fmodAssets.pickCollectiblePearl.createInstance().apply {
                                        start()
                                        release()
                                    }
                                    gameContext.gameState.pearls[pearlId] = true
                                    PlatformingScene.collectedPearls++
                                    UiRenderingSystem.collectionText = null // to let it update
                                    gameContext.save()
                                }
                            }
                        }

                        "tutorial" -> {
                            val id = spawn.properties["id"]?.string
                            val text = spawn.properties["text"]?.string
                            if (id != null && text != null && gameContext.gameState.tutorials[id] != true) {
                                createPickup(
                                    spawn,
                                    "empty",
                                    tint = Color.WHITE.toMutableColor().apply { a = 0.2f }.toFloatBits()
                                ) {

                                    if (gameContext.gameState.tutorials[id] != true) { // in case player already covered that part somehow
                                        gameContext.gameState.tutorials[id] = false
                                        ecs.apply {
                                            uiEntity[UiComponent].showTutorial = text
                                        }
                                    }
                                }
                            }
                        }

                        "tutorial-end" -> {
                            spawn.properties["id"]?.string?.let { id ->
                                if (gameContext.gameState.tutorials[id] != true) {
                                    createPickup(
                                        spawn,
                                        "empty",
                                        tint = Color.WHITE.toMutableColor().apply { a = 0.2f }.toFloatBits()
                                    ) {
                                        gameContext.gameState.tutorials[id] = true
                                        ecs.apply {
                                            uiEntity[UiComponent].showTutorial = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun EntityCreateContext.attachDress(entity: Entity) {
        if (gameContext.gameState.airPearl) {
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation("C idle"),
                walkAnimation = gameContext.assets.animation("C walk"),
                jumpAnimation = gameContext.assets.animation("C jump"),
                fallAnimation = gameContext.assets.animation("C fall"),
                swimAnimation = gameContext.assets.animation("C swimming"),
                wallSlideAnimation = gameContext.assets.animation("C wall slide"),
                swimIdleAnimation = gameContext.assets.animation("C swim_idle"),
                swimDashAnimation = gameContext.assets.animation("C swim_dash"),
                airDashAnimation = gameContext.assets.animation("C air_dash"),
                hurtAnimation = gameContext.assets.animation("C hurt"),
                animationEventCallback = { it, component ->
                    when (it) {
                        "step" -> component.playSound(gameContext.fmodAssets.step)
                    }
                },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    -0.45f.px,
                    -0.9f.px,
                    initialPlayerBounds.width * 0.91f,
                    initialPlayerBounds.height * 0.91f
                ),
                tint = Color.ORANGE.toMutableColor().apply { a = 0.2f }.toFloatBits(),
                priority = 1,
            )
        } else if (gameContext.gameState.waterPearl) {
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation("B idle"),
                walkAnimation = gameContext.assets.animation("B walk"),
                jumpAnimation = gameContext.assets.animation("B jump"),
                fallAnimation = gameContext.assets.animation("B fall"),
                swimAnimation = gameContext.assets.animation("B swimming"),
                wallSlideAnimation = gameContext.assets.animation("B wall slide"),
                swimIdleAnimation = gameContext.assets.animation("B swim_idle"),
                swimDashAnimation = gameContext.assets.animation("B swim_dash"),
                airDashAnimation = gameContext.assets.animation("B air_dash"),
                hurtAnimation = gameContext.assets.animation("B hurt"),
                animationEventCallback = { it, component ->
                    when (it) {
                        "step" -> component.playSound(gameContext.fmodAssets.step)
                    }
                },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    -0.45f.px,
                    -0.9f.px,
                    initialPlayerBounds.width * 0.91f,
                    initialPlayerBounds.height * 0.91f
                ),
                tint = Color.ORANGE.toMutableColor().apply { a = 0.2f }.toFloatBits(),
                priority = 1,
            )
        } else {
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation("A idle"),
                walkAnimation = gameContext.assets.animation("A walk"),
                jumpAnimation = gameContext.assets.animation("A jump"),
                fallAnimation = gameContext.assets.animation("A fall"),
                swimAnimation = gameContext.assets.animation("A swimming"),
                wallSlideAnimation = gameContext.assets.animation("A wall slide"),
                swimIdleAnimation = gameContext.assets.animation("A swim_idle"),
                swimDashAnimation = gameContext.assets.animation("A swim_dash"),
                airDashAnimation = gameContext.assets.animation("A air_dash"),
                hurtAnimation = gameContext.assets.animation("A hurt"),
                animationEventCallback = { it, component ->
                    when (it) {
                        "step" -> component.playSound(gameContext.fmodAssets.step)
                    }
                },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    -0.45f.px,
                    -0.9f.px,
                    initialPlayerBounds.width * 0.91f,
                    initialPlayerBounds.height * 0.91f
                ),
                tint = Color.ORANGE.toMutableColor().apply { a = 0.2f }.toFloatBits(),
                priority = 1,
            )
        }
    }

    private fun createPickup(spawn: TiledMap.Object, animationName: String, tint: Float, collect: () -> Unit) {
        ecs.entity { entity ->
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation(animationName),
                animationEventCallback = { it, _ -> println(it) },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    spawn.bounds.width * unitSize * -0.48f,
                    spawn.bounds.height * unitSize * -0.48f,
                    spawn.bounds.width * unitSize,
                    spawn.bounds.height * unitSize
                ),
                tint = tint,
            )
            entity += PositionComponent().also {
                it.position.set(
                    spawn.bounds.cx * unitSize,
                    spawn.bounds.cy * unitSize
                )
            }
            entity += RotationComponent()
            entity += ContextComponent()
            physicsSystem.createPickupBody(
                this,
                entity,
                spawn.bounds.cx * unitSize,
                spawn.bounds.cy * unitSize,
                spawn.bounds.width * unitSize,
                spawn.bounds.height * unitSize,
            ) {
                collect()
                gameContext.scheduler.schedule().then {
                    entity.remove()
                }
            }
        }
    }

    private fun createCheckpoint(spawn: TiledMap.Object, tint: Float, tintActive: Float) {
        ecs.entity { entity ->
            val checkpointId = nextCheckpointId++
            if (incrementGlobalCheckpoints) {
                PlatformingScene.nextCheckpointId++
            }
            val isActive = gameContext.gameState.checkpoint == checkpointId
            if (isActive) {
                currentlyActiveCheckpointInThisRoom = entity
                initialPlayerBounds.cx = spawn.bounds.cx * unitSize
                initialPlayerBounds.cy = spawn.bounds.y2 * unitSize - initialPlayerBounds.height * 0.5f
            }
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation("empty"),
                animationEventCallback = { it, _ -> println(it) },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    spawn.bounds.width * unitSize * -0.48f,
                    spawn.bounds.height * unitSize * -0.48f,
                    spawn.bounds.width * unitSize,
                    spawn.bounds.height * unitSize
                ),
                tint = if (isActive) tintActive else tint,
            )
            entity += PositionComponent().also {
                it.position.set(
                    spawn.bounds.cx * unitSize,
                    spawn.bounds.cy * unitSize
                )
            }
            entity += RotationComponent()
            entity += CheckpointComponent(checkpointId, isActivated = isActive)

            physicsSystem.createCheckpoint(
                this,
                entity,
                spawn.bounds.cx * unitSize,
                spawn.bounds.cy * unitSize,
                spawn.bounds.width * unitSize,
                spawn.bounds.height * unitSize,
                id = checkpointId,
            ) {
                gameContext.fmodAssets.checkPointActivated.createInstance().apply {
                    start()
                    release()
                }
                val component = entity[CheckpointComponent]
                if (!component.isActivated) {
                    component.isActivated = true
                    entity[SpriteComponent].tint = tintActive
                }
                currentlyActiveCheckpointInThisRoom?.let {
                    if (it != entity) {
                        it[CheckpointComponent].isActivated = false
                        it[SpriteComponent].tint = tint
                    }
                }
                currentlyActiveCheckpointInThisRoom = entity
                gameContext.gameState.checkpoint = checkpointId
                gameContext.save()
            }
        }
    }

    private fun spawnPlayerAttack(x: Float, y: Float, vx: Float, vy: Float, damage: Float) {
        ecs.entity { entity ->
            val radius = 1f
            entity += SpriteComponent(
                idleAnimation = gameContext.assets.animation("MC idle"),
                animationEventCallback = { it, _ -> println(it) },
                // baking offset into the bounds, maybe it should be a separate property?
                bounds = Rect(
                    -radius,
                    -radius,
                    radius * 2f,
                    radius * 2f
                ),
                tint = Color.Companion.YELLOW.toMutableColor().apply { a = 0.5f }.toFloatBits(),
                priority = 0,
            )
            entity += PositionComponent().also { it.position.set(x, y) }
            entity += RotationComponent(maxRotationVelocity = 0.1f)
            entity += MomentaryForceComponent().apply { forces.add(Vec2f(vx, vy)) }
            entity += ContextComponent()
            entity += TimeToLiveComponent(0.2f)
            physicsSystem.createPlayerAttackBody(
                this,
                entity,
                x,
                y,
                radius,
                damage,
            )
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
        attackComponent: AttackComponent,
        floatUpComponent: FloatUpComponent,
        contextComponent: ContextComponent,
        healthComponent: HealthComponent,
        staminaComponent: StaminaComponent,
        staminaDamageComponent: StaminaDamageComponent,
        invincibilityComponent: InvincibilityComponent?,
        physicsComponent: Box2DPhysicsComponent
    ) {
        reset(full = false)
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
                it += attackComponent
                it += floatUpComponent
                it += contextComponent
                it += healthComponent
                it += staminaComponent
                it += staminaDamageComponent
                if (invincibilityComponent != null) {
                    it += invincibilityComponent
                } else {
                    it -= InvincibilityComponent
                }
            }
            physicsSystem.teleport(playerEntity, playerPosition, physicsComponent)
            contextComponent.swimming = false // next room should switch body parameters for swimming if needed

            val currentlyActiveCheckpointInThisRoom = currentlyActiveCheckpointInThisRoom
            if (currentlyActiveCheckpointInThisRoom != null) {
                val checkpointComponent = currentlyActiveCheckpointInThisRoom[CheckpointComponent]
                if (checkpointComponent.id != gameContext.gameState.checkpoint) {
                    checkpointComponent.isActivated = false
                    this@Room.currentlyActiveCheckpointInThisRoom = null
                } else {
                    checkpointComponent.isActivated = true
                }
            }
        }
    }

    fun render(dt: Float) {
        if (!gameContext.paused) {
            ecs.update(dt)
            if (playerPosition.x < 0f) {
                switchRoom(playerEntity, null, null, -1f, 0f, false)
            } else if (playerPosition.y < 0f) {
                switchRoom(playerEntity, null, null, 0f, -1f, false)
            } else if ( playerPosition.x > worldArea.width) {
                switchRoom(playerEntity, null, null, 1f, 0f, false)
            } else if (playerPosition.y > worldArea.height) {
                switchRoom(playerEntity, null, null, 0f, 1f, false)
            } else if (teleports.any { it.contains(playerPosition) }) {
                switchRoom(playerEntity, null, null, 0f, 0f, true)
            }
        }
    }

    fun reset(full: Boolean = false) {
        if (full) {
            addedToMap = false
        }
        respawnEntities(full)
    }
}
