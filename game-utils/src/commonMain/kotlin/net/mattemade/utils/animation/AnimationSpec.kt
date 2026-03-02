package net.mattemade.utils.animation

import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.g2d.Animation
import com.littlekt.graphics.g2d.AnimationPlayer
import com.littlekt.graphics.g2d.TextureSlice
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class AnimationSpec(
    val path: String,
    private val formatPrefix: String,
    private val formatSuffix: String?,
    private val formatNumberDigits: Int,
    val framesCount: Int,
    val frameSpecs: List<AnimationFrameSpec>,
    val signals: Map<Int, String>
) {
    fun getFramePath(frame: Int): String {
        if (formatNumberDigits == 0) {
            return "$path/$formatPrefix"
        }
        val frameString = frame.toString()
        val frameLength = frameString.length
        if (frameLength > formatNumberDigits) {
            return "$path/$formatPrefix$frameString$formatSuffix"
        } else {
            var trailingZeros = ""
            for (i in 1..(formatNumberDigits - frameLength)) {
                trailingZeros += "0"
            }
            return "$path/$formatPrefix$trailingZeros$frameString$formatSuffix"
        }
    }
}

data class AnimationFrameSpec(
    val frameIndicies: List<Int>,
    val frameDuration: List<Duration>,
    val repeatLogic: String,
)

data class AnimationPlayerSpec(
    val player: AnimationPlayer<TextureSlice>,
    val animation: Animation<TextureSlice>,
    val limitRepeats: Int = 0,
    val duration: Duration,
)

private data class TextureUsageCounter(
    var slice: TextureSlice,
    var counter: Int,
)

suspend fun VfsFile.readAnimationMultiPlayer(
    runtimeTextureAtlasPacker: RuntimeTextureAtlasPacker,
    signalCallback: ((String) -> Unit)? = null,
): Map<String, SignallingAnimationPlayer> =
    readAnimationMultiSpec().mapValues { (name, spec) ->
        createAnimationPlayer(spec, runtimeTextureAtlasPacker, signalCallback)
    }

suspend fun VfsFile.readAnimationPlayer(
    runtimeTextureAtlasPacker: RuntimeTextureAtlasPacker,
    signalCallback: ((String) -> Unit)? = null,
): SignallingAnimationPlayer =
    readAnimationSpec().let { spec ->
        createAnimationPlayer(spec, runtimeTextureAtlasPacker, signalCallback)
    }

private suspend fun createAnimationPlayer(
    spec: AnimationSpec,
    runtimeTextureAtlasPacker: RuntimeTextureAtlasPacker,
    signalCallback: ((String) -> Unit)?
): SignallingAnimationPlayer {
    val textureCache = mutableMapOf<String, TextureUsageCounter>()
    (0..<spec.framesCount).forEach {
        val textureUsage = textureCache.getOrPut(spec.getFramePath(it)) {
            TextureUsageCounter(
                runtimeTextureAtlasPacker.pack(spec.getFramePath(it)).await(),
                0
            )
        }
        textureUsage.counter++
    }
    val sliceCache = mutableMapOf<Int, TextureSlice>()
    fun getSlice(index: Int): TextureSlice =
        sliceCache.getOrPut(index) {
            var remainder = index
            for (i in 0..<spec.framesCount) {
                val textureUsage = textureCache[spec.getFramePath(i)]!!
                if (remainder >= textureUsage.counter) {
                    remainder -= textureUsage.counter
                } else {
                    val sliceWidth = textureUsage.slice.width / textureUsage.counter
                    return textureUsage.slice.slice(
                        x = remainder * sliceWidth,
                        y = 0,
                        width = sliceWidth,
                        height = textureUsage.slice.height
                    )
                }
            }
            error("Out of bounds")
        }

    val framesPerPlayer = mutableListOf<Int>()
    val players = spec.frameSpecs.map { frameSpec ->
        framesPerPlayer += frameSpec.frameIndicies.size
        val limitRepeats =
            if (frameSpec.repeatLogic.isBlank()) 0 else frameSpec.repeatLogic.toInt()
        val totalFrameDurations =
            frameSpec.frameDuration.reduce { acc, duration -> acc + duration }
        AnimationPlayerSpec(
            player = AnimationPlayer(),
            animation = Animation(
                frames = (0..<frameSpec.frameIndicies.size).map { getSlice(frameSpec.frameIndicies[it]) },
                frameIndices = frameSpec.frameIndicies.indices.toList(),
                frameTimes = frameSpec.frameDuration,
            ),
            limitRepeats = limitRepeats,
            duration = totalFrameDurations * limitRepeats,
        )

    }

    return SignallingAnimationPlayer(players, framesPerPlayer, spec.signals, signalCallback)
}

suspend fun VfsFile.readAnimationMultiSpec(): Map<String, AnimationSpec> {
    val result = mutableMapOf<String, AnimationSpec>()
    val specText = readLines().map { it.trim() }

    var from: Int = -1
    var currentName: String = ""
    for (i in specText.indices) {
        val line = specText[i].trim()
        if (line.startsWith("name ")) {
            if (from >= 0) {
                parseAnimationSpec(parent.path, specText, from, i)?.let {
                    result[currentName] = it
                }
            }
            from = i + 1
            currentName = line.substringAfter("name ").trim()
        }
    }

    if (from >= 0) {
        parseAnimationSpec(parent.path, specText, from, specText.size)?.let {
            result[currentName] = it
        }
    }

    return result
}

suspend fun VfsFile.readAnimationSpec(): AnimationSpec {
    val specText = get("spec.txt").readLines().map { it.trim() }

    return parseAnimationSpec(path, specText, 0, specText.size)!!
}

fun parseAnimationSpec(path: String, lines: List<String>, from: Int, to: Int): AnimationSpec? {
    var frameIndices = mutableListOf<Int>()
    var frameTimes = mutableListOf<Duration>()
    val signals = mutableMapOf<Int, String>()
    var repeatLogic = "1"
    var framesCount = 0
    var format = ""
    val frameSpecs = mutableListOf<AnimationFrameSpec>()
    var framesPerAllPreviousAnimations = 0
    var parsedLines = 0
    for (i in from..<to) {
        parsedLines++
        val line = lines[i]
        if (!line.startsWith("#")) {
            val formatSplit = line.split(" frames like ")
            val sequenceSplit = line.split(" for ")
            if (formatSplit.size == 2) {
                framesCount = formatSplit[0].toInt()
                format = formatSplit[1]
            } else if (sequenceSplit.size == 2) {
                val rangeSplit = sequenceSplit[0].split("..")
                val frameTime = sequenceSplit[1].toInt().milliseconds
                if (rangeSplit.size == 2) {
                    val start = rangeSplit[0].toInt()
                    val end = rangeSplit[1].toInt()
                    val range = if (start < end) start..end else start downTo end
                    for (frame in range) {
                        frameIndices += frame - 1
                        frameTimes += frameTime
                    }
                } else {
                    frameIndices += sequenceSplit[0].toInt() - 1
                    frameTimes += frameTime
                }
            } else if (line.startsWith("repeat")) {
                repeatLogic = if (line.length > 7) line.substring(7) else ""
                frameSpecs += AnimationFrameSpec(
                    frameIndices,
                    frameTimes,
                    repeatLogic,
                )
                framesPerAllPreviousAnimations += frameIndices.size
                frameIndices = mutableListOf<Int>()
                frameTimes = mutableListOf<Duration>()
                repeatLogic = ""
            } else if (line.startsWith("signal")) {
                val signal = if (line.length > 7) line.substring(7) else ""
                signals[framesPerAllPreviousAnimations + frameIndices.size] = signal
            } else {
                parsedLines--
            }
        } else {
            parsedLines--
        }
    }
    val formatSplit = format.split(Regex("%+"))

    return if (parsedLines > 0) {
        AnimationSpec(
            path,
            formatPrefix = formatSplit[0],
            formatSuffix = formatSplit.getOrNull(1),
            format.count { it == '%' },
            framesCount,
            frameSpecs,
            signals
        )
    } else {
        null
    }
}

/*
7 frames like walk.%%%%.png
1..7 for 16
repeat from start
*/
