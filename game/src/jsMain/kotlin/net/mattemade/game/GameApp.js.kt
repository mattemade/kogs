package net.mattemade.game

import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import net.mattemade.concurrent.currentWorkerContext
import net.mattemade.concurrent.repeatEvery
import net.mattemade.concurrent.spawnWorker
import org.w3c.dom.HTMLCanvasElement

fun main() {
    val currentWorkerContext = currentWorkerContext()
    if (currentWorkerContext != null) {
        println("bg thread open!!")
        repeatEvery(1) {
            println("bg thread continues!!")
            while (currentWorkerContext.hasNext()) {
                println("next int: ${currentWorkerContext.readInt()}")
            }
        }
    } else {
        println("main thread open!!")
        val channel = spawnWorker("game.js", "aaa", {})

        createLittleKtApp {
            width = 960
            height = 540
            title = "game"
            canvas = document.getElementById("canvas") as HTMLCanvasElement
        }.start {
            Logger.setLevels(Logger.Level.NONE)

            Game(
                context = it,
                externalMessages = channel,
            )
        }
    }
}