package net.mattemade.gametemplate.resources

import net.mattemade.gametemplate.TemplateGameContext

class Music(private val resourceMusic: ResourceMusic, private val gameContext: TemplateGameContext) {

    private val clip = gameContext.assets.musicFiles.map[resourceMusic.file]!!

    fun play() {
        clip.play(loop = true)
    }

}