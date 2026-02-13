package net.mattemade.bigmode.resources

import com.littlekt.graphics.Color
import kotlin.random.Random

class TemplateResourceSheet(data: List<String>) {

    val sprites: List<ResourceSprite>
    val sounds: List<ResourceSound>
    val music: List<ResourceMusic>
    val levels: List<Level>
    val powerUps: List<PowerUpSpec>
    val hazards: List<Hazard>
    val parameters: List<Parameter>

    val textures: Set<String>
    val soundFiles: Set<String>
    val musicFiles: Set<String>

    val spriteById: Map<String, ResourceSprite>
    val soundsById: Map<String, List<ResourceSound>>
    val musicById: Map<String, ResourceMusic>
    val levelById: Map<Int, Level>
    val powerUpById: Map<Int, PowerUpSpec>

    //val hazardById: Map<String, Hazard>
    val weightedHazards: List<Pair<Float, Hazard>>
    val parametersByName: Map<String, Parameter>

    init {
        data.forEach {
            //println(it)
        }
        val spritesResult = mutableListOf<ResourceSprite>()
        val soundsResult = mutableListOf<ResourceSound>()
        val musicResult = mutableListOf<ResourceMusic>()
        val levelsResult = mutableListOf<Level>()
        val powerUpsResult = mutableListOf<PowerUpSpec>()
        val hazardsResult = mutableListOf<Hazard>()
        val parametersResult = mutableListOf<Parameter>()

        val spriteByIdResult = mutableMapOf<String, ResourceSprite>()
        val soundsByIdResult = mutableMapOf<String, MutableList<ResourceSound>>()
        val musicByIdResult = mutableMapOf<String, ResourceMusic>()
        val levelByIdResult = mutableMapOf<Int, Level>()
        val powerUpByIdResult = mutableMapOf<Int, PowerUpSpec>()
        val hazardByIdResult = mutableMapOf<String, Hazard>()
        val parameterByNameResult = mutableMapOf<String, Parameter>()

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
                                val music = ResourceMusic(id, file, line["BPM"]?.toFloatOrNull() ?: 200f)
                                musicResult += music
                                musicByIdResult[id] = music
                            }
                        }
                    }

                    "Levels" -> {
                        line["Level"]?.toIntOrNull()?.let { level ->
                            line["Orders to deliver"]?.toIntOrNull()?.let { orders ->
                                line["Table layout"]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                                    ?.let { layout ->
                                        line["Food station"]?.lowercase()?.toBooleanStrictOrNull()?.let { food ->
                                            line["Drink station"]?.lowercase()?.toBooleanStrictOrNull()?.let { drinks ->
                                                line["Horizontal table layout bias"]?.toFloatOrNull()?.let { bias ->
                                                    line["Impatience time"]?.toFloatOrNull()?.let { impatience ->
                                                        line["Time to throw"]?.toFloatOrNull()?.let { tro ->
                                                            line["Min time to order"]?.toFloatOrNull()?.let { minToOrder ->
                                                                line["Max time to order"]?.toFloatOrNull()?.let { maxToOrder ->
                                                                    line["Min time between orders"]?.toFloatOrNull()?.let { betweenOrders ->
                                                                        val level = Level(
                                                                            level,
                                                                            orders,
                                                                            layout,
                                                                            food,
                                                                            drinks,
                                                                            bias,
                                                                            impatience,
                                                                            impatience + tro,
                                                                            minToOrder,
                                                                            maxToOrder,
                                                                            betweenOrders,
                                                                        )
                                                                        levelsResult += level
                                                                        levelByIdResult[level.level] = level
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    "PowerUps" -> {
                        line["Level"]?.toIntOrNull()?.let { level ->
                            line["Power up"]?.let { power ->
                                line["Chance"]?.toFloatOrNull()?.let { chance ->
                                    line["Active time"]?.toFloatOrNull()?.let { activeTimer ->
                                        line["Additve tint color, RGB(255,255,255)"]?.split(",")
                                            ?.mapNotNull { it.trim().toFloatOrNull() }?.takeIf { it.size == 3 }
                                            ?.let { Color(it[0] / 255f, it[1] / 255f, it[2] / 255f) }?.let { tint ->
                                                line["Speed boost"]?.lowercase()?.toBooleanStrictOrNull()
                                                    ?.let { speed ->
                                                        line["Sticky fingers: plates never fall"]?.lowercase()
                                                            ?.toBooleanStrictOrNull()?.let { sticky ->
                                                                line["Calm down: temporarily stop impatience timers"]?.lowercase()
                                                                    ?.toBooleanStrictOrNull()?.let { calm ->
                                                                        line["Stack stabilizer"]?.lowercase()
                                                                            ?.toBooleanStrictOrNull()
                                                                            ?.let { stack ->
                                                                                line["Cleanup Hazards: remove all"]?.lowercase()
                                                                                    ?.toBooleanStrictOrNull()
                                                                                    ?.let { cleanup ->
                                                                                        val powerup = PowerUpSpec(
                                                                                            level,
                                                                                            power,
                                                                                            tint,
                                                                                            activeTimer,
                                                                                            speed,
                                                                                            sticky,
                                                                                            calm,
                                                                                            stack,
                                                                                            cleanup,
                                                                                        )
                                                                                        powerUpsResult += powerup
                                                                                        powerUpByIdResult[powerup.availableFrom] =
                                                                                            powerup
                                                                                    }
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                                }
                            }
                        }
                    }

                    "Parameters" -> {
                        line["Name"]?.let { name ->
                            line["Value"]?.toFloatOrNull()?.let { value ->
                                val parameter = Parameter(name, value)
                                parametersResult += parameter
                                parameterByNameResult[name] = parameter
                            }
                        }
                    }

                    "Hazards" -> {
                        line["Type"]?.let { type ->
                            line["Chance factor"]?.toFloatOrNull()?.let { chanceFactor ->
                                val hazard = Hazard(type, chanceFactor)
                                hazardsResult += hazard
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
        levels = levelsResult
        powerUps = powerUpsResult
        hazards = hazardsResult
        parameters = parametersResult

        textures = sprites.map { it.file }.toSet()
        soundFiles = sounds.map { it.file }.toSet()
        musicFiles = music.map { it.file }.toSet()


        spriteById = spriteByIdResult
        soundsById = soundsByIdResult
        musicById = musicByIdResult
        levelById = levelByIdResult
        powerUpById = powerUpByIdResult
        //hazardById = hazardByIdResult
        parametersByName = parameterByNameResult

        val totalHazardChance = hazardsResult.sumOf { it.chanceFactor }
        weightedHazards = hazardsResult.map { it.chanceFactor / totalHazardChance to it }
    }


    fun getRandomHazard(): String {
        var random = Random.nextFloat()

        for ((chance, hazard) in weightedHazards) {
            random -= chance
            if (random < 0f) {
                return hazard.type
            }
        }

        return weightedHazards.last().second.type
    }


    companion object {
        val tables =
            listOf(
                "Sprites",
                "Sounds",
                "Music",
                "Levels",
                "PowerUps",
                "Hazards",
                "Parameters",
            )

        fun ranges(encode: (String) -> String) =
            tables.joinToString(separator = "") { "&range=${encode(it)}%21A1%3AZZ" }
    }
}


inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
    var sum: Float = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}