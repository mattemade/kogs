package net.mattemade.bigmode.stuff

import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.PI_F
import com.littlekt.math.Rect
import korlibs.memory.toIntFloor
import net.mattemade.bigmode.BAND_SCALE
import net.mattemade.bigmode.BigmodeGameContext
import net.mattemade.bigmode.MUSIC_VOLUME
import net.mattemade.bigmode.RESTAURANT_WIDTH
import net.mattemade.bigmode.STAGE_TEXTURE_SCALE
import net.mattemade.bigmode.TEXTURE_SCALE
import net.mattemade.bigmode.resources.Level
import net.mattemade.bigmode.resources.Music
import kotlin.math.sin
import kotlin.random.Random

class Stage(private val gameContext: BigmodeGameContext, private val level: Level, private val spawnPowerup: (member: Int, x: Float) -> Unit) {
    private val initialMember: Int = level.level
    private val topOffset = 60f
    private val memberTopOffset = topOffset + 5f

    val sprite = Sprite(x = 0f, y = 0f, gameContext, "Stage", STAGE_TEXTURE_SCALE)
    val area = Rect()

    val bandMembers = listOf(
        Sprite(x = 0f, y = memberTopOffset, gameContext, "Neil", BAND_SCALE), // piano
        Sprite(x = 0f, y = memberTopOffset, gameContext, "Marlene", BAND_SCALE), // bass
        Sprite(x = 0f, y = memberTopOffset, gameContext, "Bernard", BAND_SCALE), // drums
        Sprite(x = 0f, y = memberTopOffset, gameContext, "Audrey", BAND_SCALE), // guitar
        Sprite(x = 0f, y = memberTopOffset, gameContext, "Samuel", BAND_SCALE), // sax
    )
    //val bandMembersBpmScaling = listOf(3 / 4f, 1f, 1f, 1 / 2f, 1f)
    val bandMembersBpmScaling = listOf(1f, 1f, 1f, 1f, 1f)
    var goingForward = true
    var activeMembers = minOf(5, initialMember)

    var musicStartsAt: Double = 0.0
    var timeToSpawnPowerup = MAX_TIME_TO_SPAWN_POWERUP


    init {
        BEATS_TO_POWERUP = MAX_BEATS_TO_POWERUP / level.level
        onRestaurantWidthChanged()

        if (musics == null) {
            musics = listOf(
                "Level 1",
                "Level 2",
                "Level 3",
                "Level 4",
                "Level 5",
            ).map {
                gameContext.assets.music(it)!!
            }
        }
        musicStartsAt = gameContext.context.audio.currentTime()
        musics?.forEachIndexed { index, music ->
            music.play(volume = if (index == activeMembers-1) MUSIC_VOLUME else 0f)
            if (index == activeMembers-1) {
                SPB = music.resourceMusic.spb
            }
        }
    }

    fun onRestaurantWidthChanged() {
        sprite.position.x = (RESTAURANT_WIDTH - sprite.width) * 0.5f
        sprite.position.y = topOffset
        sprite.updateVisiblePosition()

        val activeStageArea = sprite.width * 0.6f
        val stageOffset = (sprite.width - activeStageArea) * 0.5f
        val memberWidth = activeStageArea / (activeMembers+1) // / (bandMembers.size + 1)
        bandMembers.forEachIndexed { index, member ->
            member.position.x = sprite.position.x + stageOffset + (index + 1) * memberWidth
            member.updateVisiblePosition()
        }

        area.set(
            newX = (RESTAURANT_WIDTH - sprite.width) * 0.5f,
            newY = sprite.position.y,
            newWidth = sprite.width,
            newHeight = sprite.height,
        )
    }

    private var beats = 0
    private var beatsToPowerup = MAX_BEATS_TO_POWERUP / level.level
    fun update(dt: Float) {
/*        timeToSpawnPowerup -= dt
        if (timeToSpawnPowerup <= 0) {
            spawnPowerup(Random.nextInt(activeMembers) + 1)
            timeToSpawnPowerup += MAX_TIME_TO_SPAWN_POWERUP
        }*/

        val audioTime = gameContext.context.audio.currentTime()

        sprite.update(dt)

        val previousBeats = beats
        beats = ((audioTime - musicStartsAt) / SPB).toIntFloor()
        beatsToPowerup += (previousBeats - beats)
        while (beatsToPowerup <= 0) {
            BEATS_TO_POWERUP = maxOf(MIN_BEATS_TO_POWERUP,BEATS_TO_POWERUP - 1)
            beatsToPowerup += BEATS_TO_POWERUP
            val member = Random.nextInt(activeMembers) + 1
            spawnPowerup(member, bandMembers[member-1].position.x)
        }


        for (i in 0..< minOf(activeMembers, bandMembers.size)) {
            bandMembers[i].let {
                val currentBeatPosition = (((audioTime - musicStartsAt) * bandMembersBpmScaling[i]) % SPB) / SPB
                it.bpmScaling = 1f + sin(currentBeatPosition.toFloat() * PI_F) * 0.1f
                it.update(dt)
            }
        }
    }

    fun render(batch: Batch) {
        sprite.render(batch)

        for (i in 0..< minOf(activeMembers, bandMembers.size)) {
            bandMembers[i].render(batch)
        }
    }

    fun stopAllMusic() {
        val currentMember =  activeMembers-1
        gameContext.scheduler.schedule().then(1f) { ratio ->
            musics?.getOrNull(currentMember)?.clip?.setVolumeAll(MUSIC_VOLUME * (1f - ratio))
        }.then {
            musics?.forEach {
                it.clip.stopAll()
            }
        }
    }

    fun addMember() {
        val previousMember = activeMembers-1
        if (goingForward) {
            activeMembers++
        } else {
            activeMembers--
        }
        if (activeMembers == bandMembers.size + 1) {
            activeMembers -= 2
            goingForward = false
        } else if (activeMembers == 0) {
            activeMembers += 2
            goingForward = true
        }
        val currentMember = activeMembers-1
        updateCurrentMusic(previousMember, currentMember)
    }

    private fun updateCurrentMusic(previousMember: Int, currentMember: Int) {
        if (previousMember != currentMember) {
            gameContext.scheduler.schedule().then(0.5f) {
                musics?.getOrNull(currentMember)?.clip?.setVolumeAll(MUSIC_VOLUME * it)
                musics?.getOrNull(previousMember)?.clip?.setVolumeAll(MUSIC_VOLUME * (1f - it))
            }.then {
                SPB = musics?.getOrNull(currentMember)?.resourceMusic?.spb ?: SPB
            }
        }
    }

    companion object {
        private var musics: List<Music>? = null
        private var SPB = 1f
        private var MAX_TIME_TO_SPAWN_POWERUP = 10f
        private const val MIN_BEATS_TO_POWERUP = 16
        private var MAX_BEATS_TO_POWERUP = MIN_BEATS_TO_POWERUP * 10
        private var BEATS_TO_POWERUP = MIN_BEATS_TO_POWERUP * 10
    }
}