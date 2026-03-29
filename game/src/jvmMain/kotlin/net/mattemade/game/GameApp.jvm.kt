package net.mattemade.game

import com.littlekt.createLittleKtApp
import net.mattemade.concurrent.currentWorkerContext
import net.mattemade.concurrent.repeatEvery
import net.mattemade.concurrent.spawnWorker

fun main() {

    val currentWorkerContext = currentWorkerContext()
    if (currentWorkerContext != null) {
        println("bg thread open!!")
        repeatEvery(20) {
            println("bg thread continues!!")
            while (currentWorkerContext.hasNext()) {
                println("next int: ${currentWorkerContext.readInt()}")
            }
            Thread.sleep(1000)
        }
    } else {
        println("main thread open!!")
        val channel = spawnWorker("bbb", "aaa", ::main)
        createLittleKtApp {
            width = 1920
            height = 1080
            title = "game"
        }.start {
            Game(
                it,
                externalMessages = channel,
            )
        }
    }
}
