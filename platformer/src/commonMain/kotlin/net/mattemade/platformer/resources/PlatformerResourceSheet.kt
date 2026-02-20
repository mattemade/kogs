package net.mattemade.platformer.resources

import com.littlekt.math.Rect

class PlatformerResourceSheet(data: List<String>) {

    val sprites = mutableListOf<ResourceSprite>()
    val sounds = mutableListOf<ResourceSound>()
    val music = mutableListOf<ResourceMusic>()
    val levels = mutableListOf<ResourceLevel>()
    val worlds = mutableListOf<String>()

    val tilesets = mutableSetOf<String>()
    val textures: Set<String>
    val soundFiles: Set<String>
    val musicFiles: Set<String>
    val levelFiles: Set<String>

    val spriteById = mutableMapOf<String, ResourceSprite>()
    val soundsById = mutableMapOf<String, MutableList<ResourceSound>>()
    val musicById = mutableMapOf<String, ResourceMusic>()
    val levelByName: Map<String, ResourceLevel>


    init {
        data.forEach {
            println(it)
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
                                levels += ResourceLevel(file, Rect())
                            } else if (file.endsWith(".tsj")) { // tileset

                            } else if (file.endsWith(".world")) { // xml world
                                worlds += file
                            } else if (file.endsWith(".png") || file.endsWith(".jpg")) {
                                tilesets += file
                            }
                        }
                    }
                }
                row++
            }

        }

        textures = sprites.map { it.file }.toSet()
        soundFiles = sounds.map { it.file }.toSet()
        musicFiles = music.map { it.file }.toSet()
        levelFiles = levels.map { it.file }.toSet()
        levelByName = levels.associateBy { it.file }
    }

    companion object {
        val tables =
            listOf(
                "Sprites",
                "Sounds",
                "Music",
                "Levels",
            )

        fun ranges(encode: (String) -> String) =
            tables.joinToString(separator = "") { "&range=${encode(it)}%21A1%3AZZ" }
    }
}