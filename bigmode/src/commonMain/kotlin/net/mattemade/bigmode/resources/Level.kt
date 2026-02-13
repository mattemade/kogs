package net.mattemade.bigmode.resources

class Level(
    val level: Int,
    val orders: Int,
    val layout: List<Int>,
    val food: Boolean,
    val drink: Boolean,
    val horizontalBias: Float,
    val impatienceTimer: Float,
    val timeToThrow: Float,
    val minTimeToOrder: Float,
    val maxTimeToOrder: Float,
    val timeBetweenOrders: Float,
)