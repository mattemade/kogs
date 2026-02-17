package net.mattemade.platformer.resources

import net.mattemade.platformer.PlatformerGameContext

class Music(private val resourceMusic: ResourceMusic, private val gameContext: PlatformerGameContext) {

    private val clip = gameContext.assets.musicFiles.map[resourceMusic.file]!!

    fun play() {
        clip.play(loop = true)
    }

}