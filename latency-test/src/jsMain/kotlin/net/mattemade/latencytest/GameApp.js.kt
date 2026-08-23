package net.mattemade.latencytest

import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

fun main() {
    createLittleKtApp {
        width = 960
        height = 540
        title = "game"
        canvas = document.getElementById("canvas") as HTMLCanvasElement
    }.start {
        Logger.setLevels(Logger.Level.NONE)

        Game(
            context = it,
            fmodFolderPrefix = "",
        )
    }
}