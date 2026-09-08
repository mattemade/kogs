package net.mattemade.gfxtest

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 1920
        height = 1080
        title = "game"
    }.start {
        GfxTest(
            context = it,
        )
    }
}
