package net.mattemade.platformer

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readAudioClipEx
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.file.vfs.readTexture
import com.littlekt.file.vfs.readTiledMap
import com.littlekt.file.vfs.readTiledWorld
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import com.littlekt.math.Rect
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.FMOD_FS_createPreloadedFile
import net.mattemade.fmod.FMOD_Module_Create
import net.mattemade.fmod.FMOD_Studio_System_Create
import net.mattemade.fmod.FmodStudioSystem
import net.mattemade.platformer.resources.Music
import net.mattemade.platformer.resources.PlatformerResourceSheet
import net.mattemade.platformer.resources.ResourceLevel
import net.mattemade.platformer.resources.Sound
import net.mattemade.platformer.resources.Sprite
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import net.mattemade.utils.msdf.MsdfFont
import net.mattemade.utils.msdf.MsdfFontShader

class PlatformerAssets(
    context: Context,
    private val gameContext: PlatformerGameContext,
    getFromUrl: (String) -> List<String>?,
    fmodFolderPrefix: String,
    fmodLiveUpdate: Boolean,
    overrideFromSheets: String? = null,
) : AssetPack(context) {
    private val runtimeTextureAtlasPacker =
        RuntimeTextureAtlasPacker(context, useMiMaps = false, allowFiltering = true).releasing()

    val shaders by pack(order = 0) { Shaders(context) }
    val fmod by pack(order = 0) {
        Fmod(context, fmodFolderPrefix, fmodLiveUpdate)
    }
    val resourceSheet by preparePlain(order = 0) {
        PlatformerResourceSheet(
            if (overrideFromSheets != null) {
                getFromUrl(
                    "https://demo.mattemade.net/sheets?id=$overrideFromSheets${
                        PlatformerResourceSheet.ranges(gameContext.encodeUrlComponent)
                    }"
                ) ?: try {
                    context.vfs["https://demo.mattemade.net/sheets?id=$overrideFromSheets${
                        PlatformerResourceSheet.ranges(gameContext.encodeUrlComponent)
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
    val levels by pack(order = 3) { LevelFiles(context, resourceSheet, atlas) }

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
    private val resourceSheet: PlatformerResourceSheet,
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
    private val tilesetPreparation = resourceSheet.tilesets.forEach { file ->
        preparePlain {
            packer.pack("world/$file", normalizedPath = file).await()
                .also { map[file] = it }
        }
    }

    val whitePixel by "texture/white_pixel.png".pack()
    val bungee256 by "font/bungee_256_uppercase_0.png".pack()
    val fredokaMedium128 by "font/fredoka_medium_128_0.png".pack()
}

class SoundFiles(context: Context, private val resourceSheet: PlatformerResourceSheet) :
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

class MusicFiles(context: Context, private val resourceSheet: PlatformerResourceSheet) :
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

class LevelFiles(
    context: Context,
    private val resourceSheet: PlatformerResourceSheet,
    private val atlas: TextureAtlas
) : AssetPack(context) {
    val map = ConcurrentMutableMap<String, TiledMap>()

    private val assembly = preparePlain {
        resourceSheet.worlds.forEach { worldFile ->
            context.resourcesVfs["world/$worldFile"].readTiledWorld().apply {
                maps.forEach { mapDefinition ->
                    context.resourcesVfs["world/${mapDefinition.fileName}"].readTiledMap(
                        atlas = atlas,
                        tilesetBorder = 0
                    ).also { level ->
                        map[mapDefinition.fileName] = level
                        resourceSheet.levelByName[mapDefinition.fileName] = ResourceLevel(
                            mapDefinition.fileName, Rect(
                                mapDefinition.x.toFloat() / level.tileWidth,
                                mapDefinition.y.toFloat() / level.tileHeight,
                                mapDefinition.width.toFloat() / level.tileWidth,
                                mapDefinition.height.toFloat() / level.tileHeight,
                            )
                        )
                    }
                }
            }
        }
    }

    /*private val preparation = resourceSheet.levelFiles.forEach { file ->
        preparePlain {
            context.resourcesVfs["world/$file"].readTiledMap(atlas = atlas, tilesetBorder = 0).also {
                map[file] = it

                if (map.size == resourceSheet.levelFiles.size) {
                    resourceSheet.worlds.forEach { worldFile ->
                        context.resourcesVfs["world/$worldFile"].readTiledWorld().apply {
                            maps.forEach { mapDefinition ->
                                map[mapDefinition.fileName]?.let { level ->
                                    resourceSheet.levelByName[mapDefinition.fileName]?.worldArea?.set(
                                        mapDefinition.x.toFloat() / level.tileWidth,
                                        mapDefinition.y.toFloat() / level.tileHeight,
                                        mapDefinition.width.toFloat() / level.tileWidth,
                                        mapDefinition.height.toFloat() / level.tileHeight,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }*/
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
        preparePlain {
            MsdfFont(
                atlas = context.resourcesVfs["font/$name.png"].readTexture(
                    minFilter = TexMinFilter.LINEAR,
                    magFilter = TexMagFilter.LINEAR,
                    mipmaps = false,
                ),
                lineHeight = lineHeight,
                descender = descender,
                csvSpecs = context.resourcesVfs["font/$name.csv"].readLines(),
            )
        }

    val fredokaMsdf by loadMsdfFont("fredoka", 1.2f, 0.236f)
    val jbMonoMsdf by loadMsdfFont("jbmono", 1.32f, 0.3f)
}

class Shaders(context: Context) : AssetPack(context) {
    val msdfShader by preparePlain {
        MsdfFontShader.prepare(context)
        MsdfFontShader.program
    }
}

class Fmod(context: Context, fmodFolderPrefix: String, fmodLiveUpdate: Boolean) : AssetPack(context) {

    lateinit var studioSystem: FmodStudioSystem
    private var studioSystemReady = false
    private val module by selfPreparePlain(order = 0, tag= "module", {
        FMOD_Module_Create({
            FMOD_BANKS.forEach {
                FMOD_FS_createPreloadedFile("${fmodFolderPrefix}fmod/${it}")
            }
        }) {
            studioSystem = FMOD_Studio_System_Create()
            val core = studioSystem.coreSystem
            // 128 is much better!
            core.setDSPBufferSize(1024, 2)
            val driver = core.getDriverInfo(0)
            core.setSoftwareFormat(driver.systemRate, FMOD.SPEAKERMODE_DEFAULT, 0)
            studioSystem.initialize(
                maxChannels = 1024,
                studioInitFlags = if (fmodLiveUpdate) FMOD.STUDIO_INIT_LIVEUPDATE else FMOD.STUDIO_INIT_NORMAL,
                initFlags = FMOD.INIT_NORMAL,
                extraDriverData = null
            )

            studioSystemReady = true
        }
    }) {
        studioSystemReady
    }
}
