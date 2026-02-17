package net.mattemade.platformer.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.world.Room
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self

class PlatformingScene(val gameContext: PlatformerGameContext): Scene, Releasing by Self() {

    val room = Room(
        map = gameContext.assets.levels.map[gameContext.assets.resourceSheet.levels[1].file]!!,
        gameContext = gameContext,
    ).releasing()

    override fun update(seconds: Float) {
        room.render(seconds)
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {

    }
}
