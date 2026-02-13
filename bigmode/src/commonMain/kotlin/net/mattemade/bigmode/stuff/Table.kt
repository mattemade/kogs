package net.mattemade.bigmode.stuff

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.util.fastForEach
import korlibs.datastructure.iterators.fastForEachWithIndex
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.FROG_SCALE
import net.mattemade.bigmode.RESTAURANT_HEIGHT_REVERSE_RATIO
import net.mattemade.bigmode.SNAKE_SCALE
import net.mattemade.bigmode.TEXTURE_SCALE
import net.mattemade.bigmode.resources.Level
import net.mattemade.bigmode.scene.RollerRestarauntScene.Companion.GOAL_RADIUS
import kotlin.math.sin

class Table(
    x: Float,
    y: Float,
    private val gameContext: BigmodeGameContext,
    private val level: Level,
    private val addObstacle: (Obstacle) -> Unit,
    private val thro: (x: Float, y: Float, hazard: Boolean) -> Unit,
    private val isCalmDown: () -> Boolean,
): Being {

    val sprite = Sprite(x, y, gameContext, "Table", TEXTURE_SCALE)
    val bubble = Sprite(x, y, gameContext, "Bubble", TEXTURE_SCALE)
    val heart = Sprite(x, y, gameContext, "Heart", TEXTURE_SCALE)
    private val snakeSoLong = gameContext.assets.sound("Snake so long")!!
    private val deliveredForTips = gameContext.assets.sound("Nicely delivered")!!
    val position = sprite.position
    val visiblePosition = sprite.visiblePosition
    var active: Boolean = false

    override val depth: Float = position.y


    val orders = Array<Pair<String, Sprite>?>(2) { null }
    var hasOrder = false
    fun hasOrder(holding: List<Pair<String, Sprite>>) = orders.any { order -> holding.any { it.first == order?.first } }
    fun hasOrder(order: String) = orders.any { it?.first == order }

    val chairs = Array<Sprite?>(2) { null }
    val customers = Array<Sprite?>(2) { null }
    private val tableType = Obstacle(position.toVec2(), TABLE_RADIUS, Obstacle.Type.TABLE)
    private var orderPlacedFor: Float = 0f
    private var failedDelivery: Boolean = false
    private var bubbleShake = 0f

    init {
        addObstacle(tableType)
    }

    override fun update(dt: Float) {
        if (hasOrder && orders.none { it?.second == heart }) {
            orderPlacedFor += dt
        }
        if (isCalmDown()) {
            orderPlacedFor = 0f
        }

        if (orderPlacedFor > level.timeToThrow) {
            bubbleShake = 0f
            orderPlacedFor = 0f
            failedDelivery = true
            customers.firstOrNull { it != null }?.let {
                it.blopY()
                thro(it.position.x, it.position.y, true)
            }
        } else if (orderPlacedFor > level.impatienceTimer) {
            bubbleShake = sin(orderPlacedFor * 50f) * 0.25f

        } else {
            bubbleShake = 0f
        }

        sprite.update(dt)
        chairs.fastForEach {
            it?.update(dt)
        }
        customers.fastForEach {
            it?.update(dt)
        }
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        sprite.render(batch)
        chairs.fastForEach {
            it?.render(batch)
        }
        customers.fastForEach {
            it?.render(batch)
        }
        orders.fastForEachWithIndex { index, order ->
            order?.let { (order, sprite) ->
                chairs[index]?.let { chair ->
                    bubble.visiblePosition.x = chair.visiblePosition.x + bubbleShake
                    bubble.visiblePosition.y = chair.visiblePosition.y - ORDER_DISTANCE
                    bubble.flipX = index == 0
                    bubble.render(batch)
                    sprite.let {
                        sprite.visiblePosition.x = chair.visiblePosition.x + bubbleShake
                        sprite.visiblePosition.y = chair.visiblePosition.y - ORDER_DISTANCE
                        if (sprite != heart) {
                            sprite.bpmScaling = 0.8f
                        }
                        sprite.render(batch)
                    }
                }
            }
        }
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        if (active) {
            shapeRenderer.filledEllipse(
                x = visiblePosition.x,
                y = visiblePosition.y,
                rx = GOAL_RADIUS,
                ry = GOAL_RADIUS * RESTAURANT_HEIGHT_REVERSE_RATIO,
                innerColor = tableColor,
                outerColor = tableColor
            )
        }
    }

    fun placeChair(index: Int) {
        chairs[index] = Sprite(
            x = sprite.position.x - CHAIR_DISTANCE + index * CHAIR_DOUBLE_DISTANCE,
            y = sprite.position.y,
            gameContext = gameContext,
            name = "Chair",
            textureScale = TEXTURE_SCALE,
        ).also {
            addObstacle(Obstacle(it.position.toVec2(), CHAIR_RADIUS, Obstacle.Type.CHAIR))
        }
    }

    fun placeOrder(order: Pair<String, Sprite>) {
        orderPlacedFor = 0f
        val index = customers.indexOfFirst { it != null }
        if (index >= 0) {
            hasOrder = true
            orders[index] = order
        }
    }

    fun placeRandomCustomer() {
        val index = chairs.indices.random()
        if (index >= 0) {
            val customerIndex = currentCustomer
            currentCustomer = (currentCustomer + 1) % customerSprites.size
            val (customer, scale) = customerSprites[customerIndex]
            tableType.type = if (customer == "Snake") Obstacle.Type.SNAKE_TABLE else Obstacle.Type.TABLE
            customers[index] = Sprite(
                x = sprite.position.x - CHAIR_DISTANCE + index * CHAIR_DOUBLE_DISTANCE,
                y = sprite.position.y - CHAIR_Y_OFFSET,
                gameContext = gameContext,
                name = customer,
                textureScale = scale,
                flipX = index % 2 == 0
            )
        }
    }

    fun deliver(): String? {
        val index = orders.indexOfFirst { it != null }
        var result: String? = null
        if (index >= 0) {
            customers[index]?.let { customer ->
                customer.blopY()
                if (orderPlacedFor <= level.impatienceTimer * 0.5f && !failedDelivery) {
                    deliveredForTips.play()
                    gameContext.scheduler.schedule().then(0.5f).then {
                        thro(customer.position.x, customer.position.y, false)
                    }
                }
                if (tableType.type == Obstacle.Type.SNAKE_TABLE && orderPlacedFor > level.impatienceTimer) {
                    snakeSoLong.play()
                }
            }
            result = orders[index]?.first
            orders[index] = "" to heart

            gameContext.scheduler.schedule().then(2f).then {
                orders[index] = null
                hasOrder = orders.any { it != null }
            }
        }
        orderPlacedFor = 0f
        failedDelivery = false
        return result
    }

    companion object {
        private val tableColor = Color(r = 0.976f, g= 0.922f, b = 0.675f, a = 1f).toFloatBits()
        private const val CHAIR_DISTANCE = 20f
        private const val ORDER_DISTANCE = 30f
        private const val CHAIR_Y_OFFSET = 10f
        private const val CHAIR_RADIUS = 5f
        private const val TABLE_RADIUS = 10f
        private const val CHAIR_DOUBLE_DISTANCE = CHAIR_DISTANCE * 2f

        private val customerSprites =
            listOf(
                "Frog" to FROG_SCALE,
                "Snake" to SNAKE_SCALE,
                "Stote" to TEXTURE_SCALE,
                "White dog" to TEXTURE_SCALE,
                "Donkey" to TEXTURE_SCALE,
                "Crow" to TEXTURE_SCALE,
                "Lizard" to TEXTURE_SCALE,
                "Fox" to TEXTURE_SCALE,
                "Hedgehog" to TEXTURE_SCALE,
            ).shuffled()

        private var currentCustomer = 0
    }

}