package net.mattemade.gametemplate.resources

import com.littlekt.audio.AudioClipEx
import net.mattemade.gametemplate.TemplateGameContext
import net.mattemade.utils.math.sumOf
import kotlin.random.Random

class Sound(private val id: String, private val entries: List<ResourceSound>, private val gameContext: TemplateGameContext) {

    private val pool: List<RoundRobinEntry>
    private var currentIndex = 0

    init {
        val poolResult = mutableListOf<RoundRobinEntry>()
        val randomPool = mutableListOf<Pair<ResourceSound, AudioClipEx>>()
        entries.forEach {
            if (it.probability > 0f) {
                gameContext.assets.soundFiles.map[it.file]?.let { file ->
                    randomPool += it to file
                }
            } else if (it.probability < 0f) {
                gameContext.assets.soundFiles.map[it.file]?.let { file ->
                    poolResult += RoundRobinEntry(listOf(it to file))
                }
            }
        }
        poolResult += RoundRobinEntry(randomPool)
        pool = poolResult
    }

    fun play() {
        val entry = pool[currentIndex].entry
        currentIndex = (currentIndex + 1) % pool.size

        val playbackRate = entry.first.minRate + (entry.first.maxRate - entry.first.minRate) * Random.nextFloat()
        val volume = entry.first.minVolume + (entry.first.maxVolume - entry.first.minVolume) * Random.nextFloat()

        entry.second.apply {
            val id = play(volume, onEnded = null)
            setPlaybackRate(id, playbackRate)
        }
    }

    private class RoundRobinEntry(val entries: List<Pair<ResourceSound, AudioClipEx>>) {
        private val totalProbability = entries.sumOf { it.first.probability }
        private val maxIndex = entries.size-1

        val entry: Pair<ResourceSound, AudioClipEx>
            get() {
                var random = Random.nextFloat() * totalProbability
                var index = 0
                while (random > entries[index].first.probability && index < maxIndex) {
                    random -= entries[index].first.probability
                    index++
                }
                return entries[index]
            }
    }
}