package net.mattemade.bigmode

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readAudioClipEx
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import net.mattemade.bigmode.resources.Music
import net.mattemade.bigmode.resources.ResourceSprite
import net.mattemade.bigmode.resources.Sound
import net.mattemade.bigmode.resources.Sprite
import net.mattemade.bigmode.resources.TemplateResourceSheet
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import net.mattemade.utils.msdf.MsdfFont
import net.mattemade.utils.msdf.MsdfFontShader

class BigmodeAssets(
    context: Context,
    private val gameContext: BigmodeGameContext,
    getFromUrl: (String) -> List<String>?,
    overrideFromSheets: String? = null,
) : AssetPack(context) {
    private val runtimeTextureAtlasPacker = RuntimeTextureAtlasPacker(context, useMiMaps = true, allowFiltering = true).releasing()

    val shaders by pack(order = 0) { Shaders(context) }
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

    val font by pack(order = 3) { Fonts(context, textureFiles) }

    fun spriteDef(id: String): ResourceSprite? =
        resourceSheet.spriteById[id]

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
    val bungee256 by "font/bungee_256_uppercase_0.png".pack()
    val fredokaMedium128 by "font/fredoka_medium_128_0.png".pack()

    val floorPurple by "texture/FLOOR_purple.png".pack()
/*    val floorYellow by "texture/FLOOR_yellow.png".pack()
    val stage by "texture/Stage.png".pack()*/
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


class Fonts(context: Context, private val textures: TextureFiles) : AssetPack(context) {

    val bungee256 by preparePlain {
        context.resourcesVfs["font/bungee_256_uppercase.fnt"].readBitmapFont(
            preloadedTextures = listOf(
                textures.bungee256
            )
        )
    }
    val fredokaMedium128 by preparePlain {
        context.resourcesVfs["font/fredoka_medium_128.fnt"].readBitmapFont(
            preloadedTextures = listOf(
                textures.fredokaMedium128
            )
        )
    }


    private fun loadMsdfFont(name: String, lineHeight: Float, descender: Float): PreparableGameAsset<MsdfFont> =
        preparePlain { MsdfFont(
            atlas = context.resourcesVfs["font/$name.png"].readTexture(
                minFilter = TexMinFilter.LINEAR,
                magFilter = TexMagFilter.LINEAR,
                mipmaps = false,
            ),
            lineHeight = lineHeight,
            descender = descender,
            csvSpecs = context.resourcesVfs["font/$name.csv"].readLines(),
        ) }

    val fredokaMsdf by loadMsdfFont("fredoka", 1.2f, 0.236f)
    val jbMonoMsdf by loadMsdfFont("jbmono", 1.32f, 0.3f)
}

class Shaders(context: Context) : AssetPack(context) {
    val msdfShader by preparePlain {
        MsdfFontShader.prepare(context)
        MsdfFontShader.program
    }
}
