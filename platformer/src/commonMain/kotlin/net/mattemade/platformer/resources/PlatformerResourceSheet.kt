package net.mattemade.platformer.resources

import com.littlekt.math.Vec2f
import net.mattemade.platformer.WORLD_NAME
import net.mattemade.platformer.parameterOverride

class PlatformerResourceSheet(data: List<String>) {

    val sprites = mutableListOf<ResourceSprite>()
    val sounds = mutableListOf<ResourceSound>()
    val music = mutableListOf<ResourceMusic>()

    //val levels = mutableListOf<ResourceLevel>()
    val worlds = mutableListOf<String>()

    val tilesets = mutableSetOf<String>()
    val textures: Set<String>
    val animations = mutableSetOf<String>()
    val soundFiles: Set<String>
    val musicFiles: Set<String>
    val enemies = mutableMapOf<String, ResourceEnemy>()
    //val levelFiles: Set<String>

    val spriteById = mutableMapOf<String, ResourceSprite>()
    val animationById = mutableMapOf<String, ResourceAnimation>()
    val animationBySpecAndRegion = mutableMapOf<String, MutableMap<String, ResourceAnimation>>()
    val soundsById = mutableMapOf<String, MutableList<ResourceSound>>()
    val musicById = mutableMapOf<String, ResourceMusic>()
    val levelByName = mutableMapOf<String, ResourceLevel>()


    init {
        data.forEach {
//            println(it)
        }

        var index = 0
        val size = data.size
        while (index < size) {
            val table = data[index++].split("!").first()
            if (table.isBlank()) {
                continue
            }
            val count = data[index++].toInt()
            val header = data[index++].split("|")
            var row = 1 // to skip the header row
            while (row < count) {
                val currentLine = data[index++].split("|")
                val line = header.mapIndexed { index, value ->
                    value to currentLine.getOrElse(index) { "" }
                }.toMap()
                /*val line = data[index++].split("|").mapIndexed { index, value ->
                    header[index] to value
                }.toMap()*/
                when (table) {
                    "Animations" -> {
                        line["ID"]?.let { id ->
                            line["Animation spec"]?.let { file ->
                                line["Spec segment"]?.let { segment ->
                                    if (file.trim().isNotEmpty() && segment.trim().isNotEmpty()) {
                                        animations += file
                                        val resourceAnimation = ResourceAnimation(
                                            id = id,
                                            file = file,
                                            segment = segment,
                                            offsetX = line["Offset X"]?.toFloatOrNull() ?: 0f,
                                            offsetY = line["Offset Y"]?.toFloatOrNull() ?: 0f,
                                            scale = line["Scale"]?.toFloatOrNull() ?: 1f,
                                        )
                                        animationById[id] = resourceAnimation
                                        animationBySpecAndRegion.getOrPut(file) { mutableMapOf() }[segment] =
                                            resourceAnimation
                                    }
                                }
                            }
                        }
                    }

                    "Sprites" -> {
                        line["texture"]?.let { file ->
                            line["Sprite ID"]?.let { id ->
                                val sprite = ResourceSprite(
                                    id, file,
                                    animationFrames = line["Animation frames"]?.toIntOrNull() ?: 1,
                                    frameTime = line["Frame time, ms"]?.toFloatOrNull()
                                        ?.let { it / 1000f } ?: Float.MAX_VALUE,
                                    anchorX = line["Anchor X"]?.toFloatOrNull() ?: 0f,
                                    anchorY = line["Anchor Y"]?.toFloatOrNull() ?: 0f,
                                )
                                sprites += sprite
                                spriteById[id] = sprite
                            }
                        }
                    }

                    "Sounds" -> {
                        line["Sound file"]?.let { file ->
                            line["Sound ID"]?.let { id ->
                                val sound = ResourceSound(
                                    id = id,
                                    file = file,
                                    probability = line["Probability"]?.toFloatOrNull() ?: 1f,
                                    minRate = line["Min playback rate, %"]?.toFloatOrNull()
                                        ?.let { it / 100f } ?: 1f,
                                    maxRate = line["Max playback rate, %"]?.toFloatOrNull()
                                        ?.let { it / 100f } ?: 1f,
                                    minVolume = line["Min volume, %"]?.toFloatOrNull()
                                        ?.let { it / 100f } ?: 1f,
                                    maxVolume = line["Max volume, %"]?.toFloatOrNull()
                                        ?.let { it / 100f } ?: 1f,
                                )
                                sounds += sound
                                soundsById.getOrPut(id, { mutableListOf() }).add(sound)
                            }
                        }
                    }

                    "Music" -> {
                        line["Music file"]?.let { file ->
                            line["Music ID"]?.let { id ->
                                val music = ResourceMusic(id, file)
                                this.music += music
                                musicById[id] = music
                            }
                        }
                    }

                    "Levels" -> {
                        line["world"]?.let { file ->
                            if (file.endsWith(".tmj")) { // tiled map
                                //levels += ResourceLevel(file, Rect())
                            } else if (file.endsWith(".tsj")) { // tileset

                            } else if (file.endsWith(".world")) { // xml world
                                //worlds += file
                            } else if (file.endsWith(".png") || file.endsWith(".jpg")) {
                                tilesets += file
                            }
                        }
                    }

                    "Parameters" -> {
                        line["Key (DO NOT CHANGE)"]?.let { key ->
                            line["Value"]?.let { value ->
                                parameterOverride[key] = value
                            }
                        }
                    }

                    "Creatures" -> {
                        line["Name"]?.let { name ->
                            line["Idle animation"]?.let { idleRegion ->
                                enemies[name] = ResourceEnemy(
                                    name,
                                    hp = line["HP"]?.toFloatOrNull() ?: 1f,
                                    idleName = idleRegion,
                                    walkName = line["Walk animation"]?.takeIf(String::isNotEmpty) ?: idleRegion,
                                    jumpName = line["Jump animation"]?.takeIf(String::isNotEmpty) ?: idleRegion,
                                    fallName = line["Fall animation"]?.takeIf(String::isNotEmpty) ?: idleRegion,
                                    swimName = line["Swim animation"]?.takeIf(String::isNotEmpty) ?: idleRegion,
                                    wallSlideName = line["Slide animation"]?.takeIf(String::isNotEmpty) ?: idleRegion,
                                    size = Vec2f(
                                        line["Width"]?.toFloatOrNull() ?: 0.98f,
                                        line["Height"]?.toFloatOrNull() ?: 0.98f,
                                    ),
                                )
                            }
                        }
                    }
                }
                row++
            }
        }

        worlds.add(WORLD_NAME)

        textures = sprites.map { it.file }.toSet()
        soundFiles = sounds.map { it.file }.toSet()
        musicFiles = music.map { it.file }.toSet()
        //levelFiles = levels.map { it.file }.toSet()
        //levelByName = levels.associateBy { it.file }
    }

    companion object {
        val tables =
            listOf(
                "Sprites",
                "Sounds",
                "Music",
                "Levels",
                "Parameters",
                "Animations",
                "Creatures",
            )

        fun ranges(encode: (String) -> String) =
            tables.joinToString(separator = "") { "&range=${encode(it)}%21A1%3AZZ" }
    }
}