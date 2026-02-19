package net.mattemade.utils.tiled

import com.littlekt.math.Vec2i


interface BoundsListener {
    fun startPath()
    fun addPoint(x: Float, y: Float)
    fun endPath()
}

private val edgeDirections = listOf(
    EdgeDirection( // up
        movement = Vec2i(0, 1),
        left = Vec2i(-1, 0),
        right = Vec2i(0, 0)
    ),
    EdgeDirection( // right
        movement = Vec2i(1, 0),
        left = Vec2i(0, 0),
        right = Vec2i(0, -1)
    ),
    EdgeDirection( // down
        movement = Vec2i(0, -1),
        left = Vec2i(0, -1),
        right = Vec2i(-1, -1)
    ),
    EdgeDirection( // left
        movement = Vec2i(-1, 0),
        left = Vec2i(-1, -1),
        right = Vec2i(-1, 0)
    ),
)

fun Array<BooleanArray>.findBounds(listener: BoundsListener) {
    val width = size
    if (width == 0) return
    val height = first().size

    val wasLeftVerticalEdgeOfTileIncludedInPath =
        Array(width + 1) { BooleanArray(height + 1) } // adding extra padding for the top/right bound edges

    var previousState = false
    for (y in 0 until height) {
        previousState = false
        for (x in 0 until width) {
            val currentState = get(x)[y]
            if (currentState != previousState) {
                previousState = currentState
                if (!wasLeftVerticalEdgeOfTileIncludedInPath[x][y]) {
                    walkAround(
                        this,
                        currentState,
                        wasLeftVerticalEdgeOfTileIncludedInPath,
                        listener,
                        x,
                        y,
                    )
                }
            }
        }
    }
}

private fun Array<BooleanArray>.isTrue(x: Int, y: Int): Boolean =
    if (x in indices && y in get(0).indices) get(x)[y] else false

private fun walkAround(
    data: Array<BooleanArray>,
    followingState: Boolean,
    wasLeftVerticalEdgeOfTileIncludedInPath: Array<BooleanArray>,
    listener: BoundsListener,
    x: Int,
    y: Int
) {
    listener.startPath()

    /**
     * General idea:
     * 1) start by "drawing" the boundary line from bottom-left corner of the current tile to top-left (by using the "up" direction of travel initially)
     * 2) make a step forward in the selected direction, and check two tiles that are in front of us:
     *   a) tile on the right hand: if it does not match the tile type we are tracing, then we should turn right
     *   b) tile on the left hand: if it does not match the tile type we are tracing, then we should continue moving in the same direction
     *   c) otherwise, we should turn left
     * 3) repeat such steps until we return to the starting point
     * 3) each time when we make a turn, send the current coordinates to the listener, so it can create the boundary path
     *
     * Important note: if the initial edge is placed on a vertical line, it will create a redundant point along the line (oh well, need to revise)
     */

    var directionIndex = 0
    var direction = edgeDirections[directionIndex]
    var currentX = x
    var currentY = y


    var sendPoint = true
    do {
        if (directionIndex == 0) { // vertical up - current's tile left edge is being processed
            wasLeftVerticalEdgeOfTileIncludedInPath[currentX][currentY] = true
        } else if (directionIndex == 2) { // vertical down - this is the left edge of the right tile
            wasLeftVerticalEdgeOfTileIncludedInPath[currentX][currentY - 1] = true
        }

        if (sendPoint) {
            sendPoint = false
            listener.addPoint(currentX.toFloat(), currentY.toFloat())
        }

        currentX += direction.movement.x
        currentY += direction.movement.y

        val shouldTurnRight = data.isTrue(currentX + direction.right.x, currentY + direction.right.y) != followingState
        if (shouldTurnRight) {
            sendPoint = true
            directionIndex = (directionIndex + 1) % edgeDirections.size
            direction = edgeDirections[directionIndex]
        }
        val shouldTurnLeft =
            !shouldTurnRight && data.isTrue(currentX + direction.left.x, currentY + direction.left.y) == followingState
        if (shouldTurnLeft) {
            sendPoint = true
            directionIndex = (directionIndex - 1 + edgeDirections.size) % edgeDirections.size
            direction = edgeDirections[directionIndex]
        }
    } while (currentX != x || currentY != y)

    listener.endPath()
}

private class EdgeDirection(
    val movement: Vec2i,
    val left: Vec2i,
    val right: Vec2i,
    // otherwise should go right
)