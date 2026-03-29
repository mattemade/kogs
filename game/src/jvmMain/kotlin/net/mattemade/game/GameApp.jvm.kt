package net.mattemade.game

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 1920
        height = 1080
        title = "game"
    }.start {
        Game(
            it,
        )
    }
}
