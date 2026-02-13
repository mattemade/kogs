package net.mattemade.bigmode.scene

import com.littlekt.audio.AudioClipEx
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.ceilToInt
import com.littlekt.math.clamp
import com.littlekt.util.fastForEach
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.FOOD_SECTION_BOTTOM
import net.mattemade.bigmode.FOOD_SECTION_TOP
import net.mattemade.bigmode.HALF_WORLD_HEIGHT_FLOAT
import net.mattemade.bigmode.HEART_SCALE
import net.mattemade.bigmode.MIN_RESTAURANT_WIDTH
import net.mattemade.bigmode.PLAYER_SCALE
import net.mattemade.bigmode.RESTAURANT_HEIGHT
import net.mattemade.bigmode.RESTAURANT_HEIGHT_RATIO
import net.mattemade.bigmode.RESTAURANT_HEIGHT_REVERSE_RATIO
import net.mattemade.bigmode.RESTAURANT_TOP_BORDER
import net.mattemade.bigmode.RESTAURANT_WIDTH
import net.mattemade.bigmode.RESTAURANT_WIDTH_DIFF_PER_LEVEL
import net.mattemade.bigmode.RESTAURANT_WIDTH_RATIO
import net.mattemade.bigmode.TEXTURE_SCALE
import net.mattemade.bigmode.WORLD_HEIGHT_FLOAT
import net.mattemade.bigmode.WORLD_WIDTH_FLOAT
import net.mattemade.bigmode.resources.Level
import net.mattemade.bigmode.resources.PowerUpSpec
import net.mattemade.bigmode.resources.ResourceSound
import net.mattemade.bigmode.resources.Sound
import net.mattemade.bigmode.resources.sumOf
import net.mattemade.bigmode.stuff.Being
import net.mattemade.bigmode.stuff.Obstacle
import net.mattemade.bigmode.stuff.PowerUp
import net.mattemade.bigmode.stuff.Sprite
import net.mattemade.bigmode.stuff.Stage
import net.mattemade.bigmode.stuff.Table
import net.mattemade.bigmode.stuff.TakeawayArea
import net.mattemade.bigmode.stuff.Thrown
import net.mattemade.utils.msdf.MsdfFontRenderer
import kotlin.math.abs
import kotlin.math.log
import kotlin.random.Random

class RollerRestarauntScene(private val gameContext: BigmodeGameContext, private val level: Level) : Scene {

    private val keyCursorPositionInWorldCoordinates = MutableVec2f()
    private val cursorPositionInWorldCoordinates: Vec2f = gameContext.cursorPositionInWorldCoordinates

    private val floorBackground = gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Floor")!!.file]!!
    private val brickWall = gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Brick wall")!!.file]!!
    private val curtain = gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Curtain")!!.file]!!
    private val foodStation = gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Food station")!!.file]!!
    private val drinkStation =
        gameContext.assets.textureFiles.map[gameContext.assets.spriteDef("Drink station")!!.file]!!
    private val hitTableSound = gameContext.assets.sound("Hit table")!!
    private val hitSnakeSound = gameContext.assets.sound("Hit snake table")!!
    private val hitChairSound = gameContext.assets.sound("Hit chair")!!
    private val hitWallSound = gameContext.assets.sound("Hit wall")!!
    private val rollingSound = gameContext.assets.sound("Roll")!!.clip()
    private val bananaThrowSound = gameContext.assets.sound("Banana throw")!!
    private val bananaLandSound = gameContext.assets.sound("Banana land")!!
    private val bananaSlipSound = gameContext.assets.sound("Banana slip")!!
    private val coinThrowSound = gameContext.assets.sound("Coin throw")!!
    private val coinLandSound = gameContext.assets.sound("Coin lands")!!
    private val powerSpawn = gameContext.assets.sound("Power spawn")!!
    private val powerPick = gameContext.assets.sound("Power pick")!!
    private val foodPick = gameContext.assets.sound("Deliver")!!
    private val drinkPick = gameContext.assets.sound("Pick drink")!!
    private val drinkDeliver = gameContext.assets.sound("Deliver drink")!!
    private val fanfare = gameContext.assets.sound("Level ends")!!
    private val stackSound = gameContext.assets.sound("Stack")!!
    private val coinPickSound = gameContext.assets.sound("Coin pick")!!
    private val plateCrashSound = gameContext.assets.sound("Plate crash")!!
    private var rollingSoundId: Int = -1
    private val brakeSound = gameContext.assets.sound("Brake")!!
    private var brakeSoundClip: Pair<ResourceSound, AudioClipEx>? = null
    private var brakeSoundId: Int = -1
    private val deliverSound = gameContext.assets.sound("Deliver")!!
    private val powerUps = mutableListOf<PowerUp>()
    private val throws = mutableListOf<Thrown>()
    private val tables = mutableListOf<Table>()
    private val obstacles = mutableListOf<Obstacle>()
    private val beings = mutableListOf<Being>()
    private val stage = Stage(gameContext, level, ::spawnPowerup)

    private val playerSprite = Sprite(0f, 0f, gameContext, "Player skating", PLAYER_SCALE)
    private val playerHitSprite = Sprite(0f, 0f, gameContext, "Player hit", PLAYER_SCALE)
    private val grayHeart = Sprite(0f,  RESTAURANT_HEIGHT - 10f, gameContext, "Gray heart", HEART_SCALE)
    private val plant = Sprite(0f,  0f, gameContext, "Plant", TEXTURE_SCALE)

    private val possibleOrder = listOfNotNull(
        listOf(
            "Cheesecake",
            "Flan",
            "Lettuce",
            "Soup",
            "Spaghetti",
            "Sushi",
            "Steak",
            "Tacos",
            "Burger",
            "Fries",
        ).takeIf { level.food || !level.drink },
        listOf(
            "Milkshake",
            "Berry boba",
            "Choco boba",
            "Water",
            "Cola",
            "Sus",
            "Forbidden",
            "Wine",
        ).takeIf { level.drink },
    )
    //private val orderSprites = possibleOrder.flatten().associateWith { createOrderSprite(it) }

    private fun createOrderSprite(string: String): Sprite = Sprite(0f, 0f, gameContext, string, TEXTURE_SCALE)

    private val playerPosition = MutableVec2f(STATION_WIDTH + 10f, RESTAURANT_HEIGHT * 0.5f)
    private val visiblePlayerPosition =
        MutableVec2f(playerPosition.x, playerPosition.y * RESTAURANT_HEIGHT_REVERSE_RATIO)
    private val playerHolding = mutableListOf<Pair<String, Sprite>>()
    private var holdingAngle: Float = 0f
    private fun addToHold(value: Pair<String, Sprite>) {
        playerHolding += value
        updateActiveTables()
    }

    private fun updateActiveTables() {
        tables.forEach {
            it.active = it.hasOrder(playerHolding)
        }
    }

    private var showHitSpriteFor: Float = 0f
    //private val goalPosition = MutableVec2f()

    private val velocity = MutableVec2f()
    private val targetVelocity = MutableVec2f()
    private val tempVec2f = MutableVec2f()
    private val tempVec2f2 = MutableVec2f()

    private val textRenderer = MsdfFontRenderer(gameContext.assets.font.fredokaMsdf)

    private var timeToShowPlusOne = 0f
    private var timeToShowHit = 0f
    private var timeToSpawn = 1f
    private var timeToOrder = level.minTimeToOrder + (level.maxTimeToOrder - level.minTimeToOrder) * Random.nextFloat() + 2f

    private var cameraOffsetX = 0f
    private var leftTrackingBorder = WORLD_WIDTH_FLOAT * 0.5f
    private val rightTrackingBorder get() = RESTAURANT_WIDTH - WORLD_WIDTH_FLOAT * 0.5f

    private var calmDownActive = false
    private val activePowerups = mutableListOf<ActivePowerup>()

    private val takeawaySegments: Int = 5
    private val takeawayHeight = (FOOD_SECTION_BOTTOM - FOOD_SECTION_TOP) / takeawaySegments.toFloat()
    private var takeawayAreas = createTakeawayAreas()
    private var ordersQueue = mutableListOf<String>()

    private fun createTakeawayAreas(): Array<TakeawayArea> =
        Array(takeawaySegments * (if (level.food && level.drink) 2 else 1)) {
            val stationIndex = it / takeawaySegments
            val index = it % takeawaySegments
            TakeawayArea(
                x = if (stationIndex == 0) 0f else RESTAURANT_WIDTH - 50f,
                y = FOOD_SECTION_TOP + takeawayHeight * index,
                width = 50f,
                height = takeawayHeight,
                station = stationIndex,
                item = null,
            )
        }

    private var ordersToDeliver = level.orders
    private val progressBarWidth = level.orders * grayHeart.width
    private val progressBarHalfWidth = progressBarWidth * 0.5f
    private val progressBarDiff = grayHeart.width
    private val levelProgress = Array<Sprite>(level.orders) {
        Sprite(0f,  RESTAURANT_HEIGHT - 10f, gameContext, "Gray heart", HEART_SCALE)
    }
    private var delivering = 0

    init {
        gameContext.log("s${level.level}")
        //spawnTables()
        //spawnRandomGoal()
        resetLevel(stage = level.level - 1, animate = false)
        playerPosition.set(RIGHT_BORDER - 5f, RESTAURANT_HEIGHT * 0.9f)
        gameContext.sceneReady()
    }

    private fun resetLevel(stage: Int = 0, animate: Boolean = true) {
        tables.clear()
        obstacles.clear()
        takeawayAreas.forEach { it.item = null }
        playerHolding.clear()
        val targetWidth = MIN_RESTAURANT_WIDTH + stage * RESTAURANT_WIDTH_DIFF_PER_LEVEL
        if (animate) {
            val currentWidth = RESTAURANT_WIDTH
            val diff = targetWidth - currentWidth
            gameContext.scheduler.schedule().then(2f) {
                RESTAURANT_WIDTH = currentWidth + diff * it
                this.stage.onRestaurantWidthChanged()
            }.then {
                ordersToDeliver = level.orders
                spawnTables(stage)
                takeawayAreas = createTakeawayAreas()
            }
        } else {
            RESTAURANT_WIDTH = targetWidth
            this.stage.onRestaurantWidthChanged()
            ordersToDeliver = level.orders
            spawnTables(stage)
            takeawayAreas = createTakeawayAreas()
        }
    }

    private fun spawnTables(stage: Int = 0) {
        val tableVerticalSpace = (RESTAURANT_HEIGHT - RESTAURANT_TOP_BORDER)
        val layout = level.layout//tableLayout[stage]
        val columns = layout.size
        val availableWidth = RIGHT_BORDER - LEFT_BORDER
        val horizontalDistance = availableWidth / (columns + 1)
        val offset = LEFT_BORDER * level.horizontalBias
        for (i in 0..<columns) {
            val rows = layout[i]
            val verticalDistance = tableVerticalSpace / (rows + 1)
            for (j in 0..<rows) {
                spawnTable(
                    offset + horizontalDistance * (i + 1),
                    RESTAURANT_TOP_BORDER + verticalDistance * (j + 1)
                )
            }
        }
        tables
    }

    private fun <T : Being> T.being(): T {
        beings.add(this)
        return this
    }

    private fun spawnTable(x: Float, y: Float) {
        tables += Table(x, y, gameContext, level, ::addObstacle, ::addThrow, ::calmDownActive).also {
            it.placeChair(0)
            it.placeChair(1)
            it.placeRandomCustomer()
        }.being()
    }

    private fun addObstacle(obstacle: Obstacle) {
        obstacles += obstacle
    }

    private fun addThrow(
        x: Float,
        y: Float,
        hazard: Boolean,
        overrideOrder: String? = null,
        overrideHeight: Float = 0f
    ) {
        if (!gameContext.inputEnabled) {
            return
        }
        val sound = if (!hazard) coinThrowSound else if (overrideOrder == null) bananaThrowSound else null
        sound?.play()
        throws += Thrown(
            gameContext,
            x,
            y,
            hazard,
            Vec2f((Random.nextFloat() - 0.5f) * 50f, (Random.nextFloat() - 0.5f) * 50f),
            LEFT_BORDER,
            RIGHT_BORDER,
            overrideOrder,
            overrideHeight,
            { item ->
                val sound = if (!hazard) coinLandSound else if (overrideOrder == null) bananaLandSound else null
                sound?.play()
                if (overrideOrder != null) {
                    plateCrashSound.play()
                    gameContext.scheduler.schedule().then {
                        throws.remove(item)
                        beings.remove(item)
                    }
                }
            }).being()
    }

    private fun spawnRandomGoal() {
        val table = tables.randomOrNull() ?: return
        if (!table.hasOrder /*&& playerPosition.distance(table.position) > GRAB_DISTANCE*/) {
            val station = possibleOrder.indices.random()
            val order = possibleOrder[station].random()
            if (tryToCook(station, order)) {
                table.placeOrder(order.let { it to createOrderSprite(it) })
            }
        }
        /*tables.random().let {
            goalPosition.set(it)
        }*/
    }

    private fun tryToCook(station: Int, order: String, shouldQueue: Boolean = true): Boolean {
        val placement = takeawayAreas.indexOfFirst { it.station == station && it.item == null }
        return if (placement >= 0) {
            takeawayAreas[placement].item = order to createOrderSprite(order)
            true
        } else if (shouldQueue) {
            ordersQueue += order
            true
        } else {
            false
        }
    }

    private fun spawnPowerup(bandMember: Int, x: Float,) {
        if (!gameContext.inputEnabled) {
            return
        }
        powerSpawn.play()
        powerUps += PowerUp(
            gameContext,
            gameContext.assets.resourceSheet.powerUpById[bandMember]!!,
            x,
            RESTAURANT_HEIGHT * 0.2f,
            Vec2f((Random.nextFloat() - 0.5f) * 20f, Random.nextFloat() * 20f)
        ).being().also { it.update(0f) }
    }

    override fun update(seconds: Float) {
        timeToSpawn -= seconds
        if (timeToSpawn <= 0f) {
            timeToSpawn += 1f
            //spawnRandomEnemy()
        }

        timeToOrder -= seconds
        if (timeToOrder <= 0f) {
            timeToOrder += level.minTimeToOrder + (level.maxTimeToOrder - level.minTimeToOrder) * Random.nextFloat()
            spawnRandomGoal()
        }

        if (showHitSpriteFor > 0f) {
            showHitSpriteFor -= seconds
        }

        timeToShowPlusOne = maxOf(0f, timeToShowPlusOne - seconds)
        timeToShowHit = maxOf(0f, timeToShowHit - seconds)

        keyCursorPositionInWorldCoordinates.set(gameContext.stickPosition).scale(50f)
        val targetPosition =
            if (keyCursorPositionInWorldCoordinates.length() > 0f || gameContext.usingKeyPosition) {
                keyCursorPositionInWorldCoordinates.add(playerPosition).scale(1f, RESTAURANT_HEIGHT_REVERSE_RATIO)
                    .subtract(cameraOffsetX - leftTrackingBorder, 0f)
            } else {
                cursorPositionInWorldCoordinates
            }

        if (gameContext.brakePressed) {
            brakeSoundClip?.let { clip ->
                clip.second.setVolume(brakeSoundId, clip.first.soundVolume(velocity.length()))
            } ?: run {
                brakeSoundClip = brakeSound.clip().also {
                    brakeSoundId = it.second.play(volume = it.first.soundVolume(velocity.length()), onEnded = null)
                }
            }
        } else {
            brakeSoundClip?.let { clip ->
                val id = brakeSoundId
                val startVolume = clip.second.getVolume(id)
                gameContext.scheduler.schedule().then(0.1f) { clip.second.setVolume(id, startVolume * (1f - it)) }
                brakeSoundClip = null
            }
        }

        if (gameContext.brakePressed) {
            targetVelocity.set(0f, 0f)
        }/* else if (gameContext.rocketPressed) {
            targetVelocity.set(targetPosition).scale(1f, RESTAURANT_HEIGHT_RATIO)
                .add(cameraOffsetX - leftTrackingBorder, 0f).subtract(playerPosition)
            if (targetVelocity.x == 0f && targetVelocity.y == 0f) {
                targetVelocity.x = Random.nextFloat()
                targetVelocity.y = Random.nextFloat()
            }
            if (targetVelocity.length() < MIN_ROCKET_VELOCITY) {
                targetVelocity.setLength(MIN_ROCKET_VELOCITY)
            }
        }*/ else {
            targetVelocity.set(targetPosition).scale(1f, RESTAURANT_HEIGHT_RATIO)
                .add(cameraOffsetX - leftTrackingBorder, 0f).subtract(playerPosition)
            /*if (gameContext.breakPressed && targetVelocity.length() > MAX_BREAK_VELOCITY) {
            targetVelocity.setLength(MAX_BREAK_VELOCITY)
        }*/
        }

        tempVec2f.set(targetVelocity).subtract(velocity)

        val accelerationScaling =
            ((VELOCITY_EFFECT_ON_ACCELERATION - velocity.length()) / VELOCITY_EFFECT_ON_ACCELERATION).let { it * it }

        val maxAcceleration =
            (if (activePowerups.any { it.spec.speedBoost }) MAX_ROCKET_PER_SECOND else if (gameContext.brakePressed) MAX_BREAK_PER_SECOND else MAX_ACCELERATION_PER_SECOND) * accelerationScaling * seconds
        val minAcceleration = 0f * seconds
        val potentialAcceleration = tempVec2f.length()
        if (potentialAcceleration > maxAcceleration) {
            tempVec2f.setLength(maxAcceleration)
        } else if (potentialAcceleration < minAcceleration) {
            if (tempVec2f.x == 0f && tempVec2f.y == 0f) {
                tempVec2f.x = Random.nextFloat()
                tempVec2f.y = Random.nextFloat()
            }
            tempVec2f.setLength(minAcceleration)
        }

        if (gameContext.inputEnabled) {
            velocity.add(tempVec2f)
        } else {
            velocity.set(0f, 0f)
        }

        tempVec2f.set(velocity).scale(seconds * VELOCITY_RATIO)
        playerPosition.add(tempVec2f)

        holdingAngle = -holdingAngle * seconds * 0.1f
        if (playerHolding.size > 1) {
            holdingAngle += tempVec2f.x * 0.25f
        } else {
            holdingAngle = 0f
        }

        // empty hand and not being hit - pick up maybe?
        if (/*playerHolding == null && */showHitSpriteFor <= 0f) {
            for (area in takeawayAreas) {
                area.item?.let { (type, sprite) ->
                    if (sprite.visiblePosition.x != 0f && sprite.visiblePosition.y != 0f && playerPosition.x > area.x && playerPosition.x < area.x2 && playerPosition.y > area.y && playerPosition.y < area.y2) {
                        addToHold(type to createOrderSprite(type).also {
                            it.travelFrom(sprite.visiblePosition)
                        })
                        area.item = null
                        if (area.station == 0) {
                            foodPick.play()
                        } else {
                            drinkPick.play()
                        }
                    }
                }
            }
        }

        while (ordersQueue.isNotEmpty() && playerPosition.x > LEFT_BORDER + 40f && playerPosition.x < RIGHT_BORDER - 40f) {
            val order = ordersQueue.first()
            val station = possibleOrder.indexOfFirst { it.contains(order) }
            if (tryToCook(station, order, shouldQueue = false)) {
                ordersQueue.removeFirst()
            } else {
                break
            }
        }

        playerHolding.forEach { it.second.update(seconds) }

        if (activePowerups.none { it.spec.stackStabilizer }) {
            val keepItems = abs((DROP_ANGLE / holdingAngle).ceilToInt())
            var remove = playerHolding.size - keepItems
            val updateTables = remove > 0
            while (remove-- > 0) {
                releaseHolding(playerHolding.removeLast())
            }
            if (updateTables) {
                updateActiveTables()
            }
        }

        keepPlayerWithinRestaurant()

        for (table in tables) {
            table.update(seconds)

            val playerHolding = playerHolding
            if (playerHolding.isNotEmpty() && table.hasOrder(playerHolding) && playerPosition.distance(table.position) <= GRAB_DISTANCE) {
                val delivered = table.deliver()
                playerHolding.remove(playerHolding.first { it.first == delivered })
                progress(coin = false)
                updateActiveTables()
                val station = possibleOrder.indexOfFirst { it.contains(delivered) }
                if (station == 0) {
                    deliverSound.play()
                } else {
                    drinkDeliver.play()
                }
                timeToShowPlusOne = 2f
            }
        }

        activePowerups.removeAll {
            it.timeLeft -= seconds
            it.timeLeft <= 0f
        }
        calmDownActive = activePowerups.any { it.spec.calmDown }

        levelProgress.forEach {
            it.update(seconds)
        }

        powerUps.removeAll {
            val remove =
                if (it.position.x < -100f || it.position.x > 1000f || it.position.y < -10f || it.position.y > 400f) {
                    true
                } else if (it.position.distance(playerPosition) < 15f) {
                    powerPick.play()
                    activePowerups += ActivePowerup(it.spec, it.spec.activeTimer).also {
                        if (it.spec.cleanup) {
                            beings.removeAll(throws)
                            throws.clear()
                        }
                    }
                    true
                } else {
                    false
                }


            if (remove) {
                beings.remove(it)
            } else {
                it.update(seconds)


            }
            remove
        }

        throws.removeAll {
            val remove =
                if (it.landed) {
                    val hit = it.position.distance(playerPosition) < 10f
                    if (hit) {
                        onHit(
                            velocity.length(),
                            if (it.hazard) bananaSlipSound else coinPickSound,
                            forceLose = true,
                            pickCoin = !it.hazard
                        )
                    }
                    hit
                } else {
                    false
                }

            if (remove) {
                beings.remove(it)
            } else {
                it.update(seconds)
            }
            remove
        }

        stage.update(seconds)

        for (obstacle in obstacles) {
            val bumpDistance = obstacle.radius + PLAYER_RADIUS
            if (playerPosition.distance(obstacle.position) <= bumpDistance) {
                playerPosition.subtract(obstacle.position).setLength(bumpDistance).add(obstacle.position)
                // lose the velocity component towards the table
                // project the velocity vector to the tangent line between Player and Table

                // normal
                tempVec2f.set(playerPosition).subtract(obstacle.position).norm()
                val projectionOnNormal = tempVec2f2.set(velocity).dot(tempVec2f)
                // project
                tempVec2f.scale(projectionOnNormal)

                val oldSpeed = velocity.length()
                velocity.subtract(tempVec2f)
                val speedDiff = abs(oldSpeed - velocity.length())
                onHit(
                    speedDiff, when (obstacle.type) {
                        Obstacle.Type.CHAIR -> hitChairSound
                        Obstacle.Type.TABLE -> hitTableSound
                        Obstacle.Type.SNAKE_TABLE -> hitSnakeSound
                    }
                )
            }
        }

        visiblePlayerPosition.set(playerPosition).scale(1f, RESTAURANT_HEIGHT_REVERSE_RATIO)

        if (gameContext.context.audio.isReady()) {
            if (rollingSoundId == -1) {
                rollingSoundId = rollingSound.play(0f, loop = true)
            }
            rollingSound.second.setVolume(rollingSoundId, rollingSound.first.soundVolume(velocity.length()))

        }

        if (RESTAURANT_WIDTH >= WORLD_WIDTH_FLOAT) {
            if (playerPosition.x < leftTrackingBorder) {
                cameraOffsetX = leftTrackingBorder
            } else if (playerPosition.x > rightTrackingBorder) {
                cameraOffsetX = rightTrackingBorder
            } else {
                cameraOffsetX = playerPosition.x
            }
        } else {
            cameraOffsetX = RESTAURANT_WIDTH * 0.5f
        }

        gameContext.camera.position.set(cameraOffsetX, HALF_WORLD_HEIGHT_FLOAT, 0f)
    }

    private fun decreaseOrdersCounter() {
        gameContext.log("ol$ordersToDeliver")
        if (--ordersToDeliver == 0) { // oh, that's dangerous, but well
            stage.stopAllMusic()
            fanfare.play()
            gameContext.scheduler.schedule().then(1f).then {
                gameContext.openScene(Scene.Type.Cutscene(level.level))
            }
        }
    }

    private fun keepPlayerWithinRestaurant() {
        if (playerPosition.x < LEFT_BORDER && velocity.x < 0f) {
            onHit(-velocity.x * 0.5f, hitWallSound)
            velocity.x = 0f
        } else if (playerPosition.x > RIGHT_BORDER && velocity.x > 0f) {
            onHit(velocity.x * 0.5f, hitWallSound)
            velocity.x = 0f
        }

        if (playerPosition.y < RESTAURANT_TOP_BORDER && velocity.y < 0f) {
            onHit(-velocity.y * 0.5f, hitWallSound)
            velocity.y = 0f
        } else if (playerPosition.y > RESTAURANT_HEIGHT && velocity.y > 0f) {
            onHit(velocity.y * 0.5f, hitWallSound)
            velocity.y = 0f
        }

        playerPosition.x = playerPosition.x.clamp(LEFT_BORDER, RIGHT_BORDER)
        playerPosition.y = playerPosition.y.clamp(RESTAURANT_TOP_BORDER, RESTAURANT_HEIGHT)
    }

    private fun onHit(velocity: Float, sound: Sound, forceLose: Boolean = false, pickCoin: Boolean = false) {
        if (forceLose || pickCoin) {
            sound.play()
        } else {
            sound.clip().play(velocity, limitMinVolume = true)
        }
        if (pickCoin) {
            progress(coin = true)
            return
        }
        if (velocity > 5f || forceLose) {
            showHitSpriteFor = 0.5f
            playerHitSprite.flipX = playerSprite.flipX
        }
        if ((velocity > 10f || forceLose) && playerHolding.isNotEmpty() && activePowerups.none { it.spec.stickyFingers }) {
            playerHitSprite.blopX()
            playerHolding.forEach {
                releaseHolding(it)
            }
            playerHolding.clear()
            updateActiveTables()
        }
    }

    private fun progress(coin: Boolean) {
        if (levelProgress.size > delivering) {
            levelProgress[delivering++] = Sprite(0f, RESTAURANT_HEIGHT - 10f, gameContext, if (coin) "Coin heart" else "Pink heart", HEART_SCALE).also {
                it.blopY()
                it.blopX()
            }
        }
        decreaseOrdersCounter()
    }

    private fun releaseHolding(pair: Pair<String, Sprite>) {
        val order = pair.first
        val station = possibleOrder.indexOfFirst { it.contains(order) }
        tryToCook(station, order)
        gameContext.scheduler.schedule().then {
            addThrow(
                playerPosition.x,
                playerPosition.y,
                hazard = false,
                overrideOrder = order,
                overrideHeight = 10f
            )
        }
    }

    private fun Pair<ResourceSound, AudioClipEx>.play(
        velocity: Float,
        loop: Boolean = false,
        limitMinVolume: Boolean = false
    ): Int =
        second.play(volume = first.soundVolume(velocity, limitMinVolume), loop = loop, onEnded = null)

    private fun ResourceSound.soundVolume(speed: Float, limitMinVolume: Boolean = false): Float =
        (log(speed, 1.5f) * this.maxVolume * SOUND_EFFECT_OF_VELOCITY).let {
            if (!limitMinVolume || it > this.minVolume) {
                it
            } else {
                0f
            }
        }


    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        renderRestaurant(batch, shapeRenderer)
        takeawayAreas.forEach {
            it.render(batch)
        }
        //renderDebugVelocity(shapeRenderer)
        beings.sortBy { it.depth }
        beings.forEach { it.renderShadow(shapeRenderer) }
        renderBeigns(shapeRenderer, batch, 0f, playerPosition.y)
        renderPlayer(batch)
        renderBeigns(shapeRenderer, batch, playerPosition.y, RESTAURANT_HEIGHT)
        renderProgress(batch)

        /*textRenderer.drawAllTextAtOnce(batch) {
            if (timeToShowPlusOne > 0f) {
                draw(GOAL_TEXT, visiblePlayerPosition.x, visiblePlayerPosition.y, 10f, batch)
            }
            if (timeToShowHit > 0f) {
                draw(HIT_TEXT, visiblePlayerPosition.x, visiblePlayerPosition.y, 10f, batch)
            }
            //draw("$ordersToDeliver", visiblePlayerPosition.x - 1f, visiblePlayerPosition.y - 40f, 10f, batch)
            //draw("Speed: ${velocity.length()}", 0f, 10f, 10f, batch)
        }*/
    }

    private fun renderProgress(batch: Batch) {
        val offset = cameraOffsetX - progressBarHalfWidth
        levelProgress.forEachIndexed { index, sprite ->
            sprite.visiblePosition.x = offset + index * progressBarDiff
            sprite.render(batch)
        }
    }

    private fun renderRestaurant(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            floorBackground,
            x = 0f,
            y = 0f,
            width = RESTAURANT_WIDTH,
            height = WORLD_HEIGHT_FLOAT
        )
        batch.draw(
            brickWall,
            x = 0f,
            y = 0f,
            width = RESTAURANT_WIDTH,
            height = WORLD_HEIGHT_FLOAT * (brickWall.height.toFloat() / brickWall.width) * RESTAURANT_WIDTH_RATIO
        )

        stage.render(batch)
        batch.draw(
            curtain,
            x = 0f,
            y = 0f,
            width = 80f,
            height = 78f
        )
        batch.draw(
            curtain,
            x = RESTAURANT_WIDTH - 80f,
            y = 0f,
            width = 80f,
            height = 78f,
            flipX = true
        )
        if (level.food || !level.drink) {
            plant.visiblePosition.x = FOOD_STATION_OFFSET + 25f
            plant.visiblePosition.y = STATION_VERTICAL_OFFSET - 1f
            plant.render(batch)
            batch.draw(
                foodStation,
                x = FOOD_STATION_OFFSET,
                y = STATION_VERTICAL_OFFSET,
                width = STATION_WIDTH,
                height = STATION_HEIGHT
            )
        }
        if (level.drink) {
            plant.visiblePosition.x = DRINK_STATION_OFFSET + 15f
            plant.visiblePosition.y = STATION_VERTICAL_OFFSET - 1f
            plant.render(batch)
            batch.draw(
                drinkStation,
                x = DRINK_STATION_OFFSET,
                y = STATION_VERTICAL_OFFSET,
                width = STATION_WIDTH,
                height = STATION_HEIGHT
            )
        }
        shapeRenderer.filledRectangle(x = -50f, y = -10f, width = 50f, height = 340f, color = blackColor)
        shapeRenderer.filledRectangle(x = RESTAURANT_WIDTH, y = -10f, width = 50f, height = 340f, color = blackColor)
    }

    private fun renderPlayer(batch: Batch) {
        val playerSprite = if (showHitSpriteFor > 0f) playerHitSprite else playerSprite

        if (activePowerups.isEmpty()) {
            playerSprite.tint.r = 1f
            playerSprite.tint.g = 1f
            playerSprite.tint.b = 1f
        } else {
            playerSprite.tint.r = activePowerups.sumOf { it.spec.tint.r }
            playerSprite.tint.g = activePowerups.sumOf { it.spec.tint.g }
            playerSprite.tint.b = activePowerups.sumOf { it.spec.tint.b }
            val max = maxOf(playerSprite.tint.r, maxOf(playerSprite.tint.g, playerSprite.tint.b))
            playerSprite.tint.r /= max
            playerSprite.tint.g /= max
            playerSprite.tint.b /= max
        }

        playerSprite.visiblePosition.set(visiblePlayerPosition)
        if (velocity.x != 0f) {
            playerSprite.flipX = velocity.x < 0f
        }
        playerSprite.render(batch)
        playerHolding.forEachIndexed { index, hold ->
            hold.second.let {
                it.visiblePosition.x =
                    visiblePlayerPosition.x + (if (playerSprite.flipX) -HOLDING_OFFSET_X else HOLDING_OFFSET_X) + index * holdingAngle * 5f
                it.visiblePosition.y =
                    visiblePlayerPosition.y - HOLDING_OFFSET_Y - index * STACK_OFFSET_Y + abs(index * holdingAngle * 2f)
                it.angle = index * holdingAngle
                it.render(batch)
            }
        }
    }

    private fun renderDebugVelocity(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(center = visiblePlayerPosition, radius = PLAYER_RADIUS, color = playerColor)
        tempVec2f.set(playerPosition).add(velocity).scale(1f, RESTAURANT_HEIGHT_REVERSE_RATIO)
        shapeRenderer.line(v1 = visiblePlayerPosition, v2 = tempVec2f, colorBits = velocityColor, thickness = 0.5f)
        tempVec2f.set(playerPosition).add(targetVelocity).scale(1f, RESTAURANT_HEIGHT_REVERSE_RATIO)
        shapeRenderer.line(
            v1 = visiblePlayerPosition,
            v2 = tempVec2f,
            colorBits = targetVelocityColor,
            thickness = 0.5f
        )
    }

    private fun renderBeigns(
        shapeRenderer: ShapeRenderer,
        batch: Batch, minY: Float, maxY: Float
    ) {
        val holding = playerHolding
        beings.fastForEach {
            if (it.depth > minY && it.depth <= maxY) {

                it.render(batch, shapeRenderer)
            }
        }
    }

    private val RIGHT_BORDER get() = if (level.food && level.drink) RESTAURANT_WIDTH - 25f else RESTAURANT_WIDTH - 5f

    class ActivePowerup(val spec: PowerUpSpec, var timeLeft: Float)

    companion object {
        private val playerTint = Color.WHITE.toMutableColor()
        private val obstacleColor = Color.RED.toFloatBits()
        private val playerColor = Color.WHITE.toFloatBits()
        private val goalColor = Color.GREEN.toFloatBits()
        private val velocityColor = Color.CYAN.toMutableColor().toFloatBits()
        private val targetVelocityColor = Color.YELLOW.toMutableColor().apply { a = 0.5f }.toFloatBits()
        private val debugColor = Color.GRAY.toFloatBits()
        private val blackColor = Color.BLACK.toFloatBits()

        private const val OBSTACLE_RADIUS = 2f
        private const val PLAYER_RADIUS = 2f
        const val GOAL_RADIUS = 20f
        const val TABLE_RADIUS = 10f

        private const val GRAB_DISTANCE = GOAL_RADIUS + PLAYER_RADIUS
        private const val HIT_DISTANCE = OBSTACLE_RADIUS + PLAYER_RADIUS
        private const val BUMP_DISTANCE = TABLE_RADIUS + PLAYER_RADIUS
        private const val TABLE_SPACING = TABLE_RADIUS * 3f

        private const val INITIAL_MAGIC_NUMBER = 10f
        var MAGIC_NUMBER = INITIAL_MAGIC_NUMBER
            set(value) {
                field = value
                SOUND_EFFECT_OF_VELOCITY = value / INITIAL_MAGIC_NUMBER
            }
        var SOUND_EFFECT_OF_VELOCITY = MAGIC_NUMBER / INITIAL_MAGIC_NUMBER

        private val MAX_BREAK_PER_SECOND get() = 600f / MAGIC_NUMBER
        private const val MAX_BREAK_VELOCITY = 1f
        private val MAX_ROCKET_PER_SECOND get() = 900f / MAGIC_NUMBER
        private val MAX_ACCELERATION_PER_SECOND get() = 300f / MAGIC_NUMBER
        private val VELOCITY_RATIO get() = 1f * MAGIC_NUMBER

        private val VELOCITY_EFFECT_ON_ACCELERATION get() = 200f / MAGIC_NUMBER
        private val BOOST_EFFECT_ON_ACCELERATION get() = 200f / MAGIC_NUMBER

        private const val MIN_ROCKET_VELOCITY = 10f
        private const val MIN_ROCKET_PER_SECOND = 10f

        private const val TIME_TO_ORDER = 2f

        private const val HOLDING_OFFSET_X = 8f
        private const val HOLDING_OFFSET_Y = 14f
        private const val STACK_OFFSET_Y = 4f

        private const val STATION_VERTICAL_OFFSET = 60f
        private const val STATION_WIDTH = 40f
        private const val STATION_HEIGHT_RATIO = 3.425770308123249f
        private const val STATION_HEIGHT = STATION_WIDTH * STATION_HEIGHT_RATIO
        private const val FOOD_STATION_OFFSET = -10f
        private val DRINK_STATION_OFFSET get() = RESTAURANT_WIDTH - STATION_WIDTH - FOOD_STATION_OFFSET

        const val LEFT_BORDER = 30f

        private const val DROP_ANGLE = PI_F * 0.33f

        private const val HIT_TEXT = "hit"
        private const val GOAL_TEXT = "+1"
    }
}