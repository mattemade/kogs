package net.mattemade.gametemplate

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readAudioClipEx
import com.littlekt.graphics.g2d.TextureSlice
import net.mattemade.gametemplate.resources.Music
import net.mattemade.gametemplate.resources.Sound
import net.mattemade.gametemplate.resources.Sprite
import net.mattemade.gametemplate.resources.TemplateResourceSheet
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker

class TemplateAssets(
    context: Context,
    private val gameContext: TemplateGameContext,
    getFromUrl: (String) -> List<String>?,
    overrideFromSheets: String? = null,
) : AssetPack(context) {
    private val runtimeTextureAtlasPacker = RuntimeTextureAtlasPacker(context).releasing()

    val resourceSheet by preparePlain(order = 0) {
        TemplateResourceSheet(
            if (overrideFromSheets != null) {
                getFromUrl(
                    "https://demo.mattemade.net/sheets?id=$overrideFromSheets${
                        TemplateResourceSheet.ranges(gameContext.encodeUrlComponent)
                    }"
                ) ?: try {
                    context.vfs["https://demo.mattemade.net/sheets?id=$overrideFromSheets${
                        TemplateResourceSheet.ranges(gameContext.encodeUrlComponent)
                    }"].readLines()
                } catch (e: Exception) {
                    context.resourcesVfs["resources.csv"].readLines().map { it.trim() }
                }
            } else {
                context.resourcesVfs["resources.csv"].readLines().map { it.trim() }
            }
        )
    }

    val textureFiles by pack(order = 1) { TextureFiles(context, runtimeTextureAtlasPacker, resourceSheet) }
    val soundFiles by pack(order = 1) { SoundFiles(context, resourceSheet) }
    val musicFiles by pack(order = 1) { MusicFiles(context, resourceSheet) }

    private val atlas by prepare(2) { runtimeTextureAtlasPacker.packAtlas() }


    fun sprite(id: String): Sprite? =
        resourceSheet.spriteById[id]?.let { Sprite(it, gameContext) }

    fun sound(id: String): Sound? =
        resourceSheet.soundsById[id]?.let { Sound(id, it, gameContext) }

    fun music(id: String): Music? =
        resourceSheet.musicById[id]?.let { Music(it, gameContext) }
}

class TextureFiles(
    context: Context,
    private val packer: RuntimeTextureAtlasPacker,
    private val resourceSheet: TemplateResourceSheet,
) :
    AssetPack(context) {
    val map = ConcurrentMutableMap<String, TextureSlice>()

    private fun String.pack(): PreparableGameAsset<TextureSlice> =
        preparePlain { packer.pack(this).await() }

    private val preparation = resourceSheet.textures.forEach { file ->
        preparePlain {
            packer.pack("texture/$file").await()
                .also { map[file] = it }
        }
    }

    val whitePixel by "texture/white_pixel.png".pack()
}

class SoundFiles(context: Context, private val resourceSheet: TemplateResourceSheet) :
    AssetPack(context) {
    val map = ConcurrentMutableMap<String, AudioClipEx>()

    private val preparation = resourceSheet.soundFiles.forEach { file ->
        preparePlain {
            context.resourcesVfs["sound/$file"].readAudioClipEx().also {
                map[file] = it
            }
        }
    }

//    val sound by prepare { context.resourcesVfs["sound/sound.wav"].readAudioClipEx() }
}

class MusicFiles(context: Context, private val resourceSheet: TemplateResourceSheet) :
    AssetPack(context) {
    val map = ConcurrentMutableMap<String, AudioClipEx>()

    private val preparation = resourceSheet.musicFiles.forEach { file ->
        preparePlain {
            context.resourcesVfs["music/$file"].readAudioClipEx().also {
                map[file] = it
            }
        }
    }

//    val music by prepare { context.resourcesVfs["music/music.ogg"].readAudioClipEx() }
}
