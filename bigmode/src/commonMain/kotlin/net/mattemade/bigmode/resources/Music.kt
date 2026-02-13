package net.mattemade.bigmode.resources

import net.mattemade.bigmode.BigmodeGameContext

class Music(val resourceMusic: ResourceMusic, private val gameContext: BigmodeGameContext) {

    val clip = gameContext.assets.musicFiles.map[resourceMusic.file]!!

    fun play(volume: Float = 1f) {
        clip.play(volume = volume, loop = true)
    }

}