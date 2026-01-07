package net.mattemade.gametemplate.resources

class TemplateResourceSheet(data: List<String>) {

    val sprites: List<ResourceSprite>
    val sounds: List<ResourceSound>
    val music: List<ResourceMusic>

    val textures: Set<String>
    val soundFiles: Set<String>
    val musicFiles: Set<String>

    val spriteById: Map<String, ResourceSprite>
    val soundsById: Map<String, List<ResourceSound>>
    val musicById: Map<String, ResourceMusic>


    init {
        data.forEach {
            println(it)
        }
        val spritesResult = mutableListOf<ResourceSprite>()
        val soundsResult = mutableListOf<ResourceSound>()
        val musicResult = mutableListOf<ResourceMusic>()

        val spriteByIdResult = mutableMapOf<String, ResourceSprite>()
        val soundsByIdResult = mutableMapOf<String, MutableList<ResourceSound>>()
        val musicByIdResult = mutableMapOf<String, ResourceMusic>()

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
                                spritesResult += sprite
                                spriteByIdResult[id] = sprite
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
                                soundsResult += sound
                                soundsByIdResult.getOrPut(id, { mutableListOf() }).add(sound)
                            }
                        }
                    }

                    "Music" -> {
                        line["Music file"]?.let { file ->
                            line["Music ID"]?.let { id ->
                                val music = ResourceMusic(id, file)
                                musicResult += music
                                musicByIdResult[id] = music
                            }
                        }
                    }
                }
                row++
            }

        }

        sprites = spritesResult
        sounds = soundsResult
        music = musicResult

        textures = sprites.map { it.file }.toSet()
        soundFiles = sounds.map { it.file }.toSet()
        musicFiles = music.map { it.file }.toSet()

        spriteById = spriteByIdResult
        soundsById = soundsByIdResult
        musicById = musicByIdResult
    }

    companion object {
        val tables =
            listOf(
                "Sprites",
                "Sounds",
                "Music",
            )

        fun ranges(encode: (String) -> String) =
            tables.joinToString(separator = "") { "&range=${encode(it)}%21A1%3AZZ" }
    }
}