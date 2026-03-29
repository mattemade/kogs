package net.mattemade.game

import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger

fun main() {
    createLittleKtApp {
        width = 960
        height = 540
        title = "game"
        canvasId = "canvas"
    }.start {
        Logger.setLevels(Logger.Level.NONE)

        Game(
            context = it,
        )
    }
}