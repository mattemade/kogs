package net.mattemade.platformer

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.FmodBank
import net.mattemade.fmod.FmodEventDescription
import net.mattemade.utils.asset.AssetPack

class FmodAssets(
    context: Context,
    fmodFolderPrefix: String,
    private val gameContext: PlatformerGameContext,
) : AssetPack(context) {

    private val studioSystem = gameContext.assets.fmod.studioSystem

    val map = ConcurrentMutableMap<String, FmodBank>()
    val eventCache = ConcurrentMutableMap<String, FmodEventDescription?>()

    fun getEvent(name: String): FmodEventDescription? = eventCache.getOrPut(name) { studioSystem.getEvent(name) }

    val preparation by selfPreparePlain(order = 0, action = {
        FMOD_BANKS.forEach { bankName ->
            val bank by selfPreparePlain(order = 0, action = {
                val bank =
                    studioSystem.loadBankFile("${fmodFolderPrefix}fmod/${bankName}", FMOD.STUDIO_LOAD_BANK_NONBLOCKING)
                bank
            }) {
                val result = it.loadingState == FMOD.STUDIO_LOADING_STATE_LOADED
                if (result) {
                    map[bankName] = it
                }
                result
            }
        }
    }) {
        map.size == FMOD_BANKS.size
    }

    val sampleDataPreparation by selfPreparePlain(order = 1, action = {
        map.forEach { (key, value) ->
            if (key.contains("Game")) {
                value.loadSampleData()
            }
        }
    }) {
        map.all { (key, value) ->
            !key.contains("Game") || value.sampleLoadingState == FMOD.STUDIO_LOADING_STATE_LOADED
        }
    }

    val jump by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val jumpDownThroughPlatform by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump down through platform")!! }
    val land by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Land")!! }
    val step by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Footsteps")!! }
    val doubleJump by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Double jump")!! }
    val wallSlideLoop by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Wall slide (loop)")!! }
    val damaged by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Damaged")!! }

    val pickUpgrade by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Upgrade")!! }
    val pickCollectiblePearl by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Pick collectible pearl")!! }

    val firstAttack by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/First attack")!! }
    val secondAttack by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Second attack")!! }
    val thirdAttack by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Third attack")!! }

    val airDash by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Air dash (start + loop)")!! }
    val waterDash by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Water dash (start + loop)")!! }

    val swim by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Swim (loop)")!! }
    val getInWater by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Start swimming")!! }
    val getOutOfWater by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Stop swimming")!! }

    val hitEnemy by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Hit enemy")!! }
    val enemyDefeated by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Enemy is defeated")!! }
    val crabMoves by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val jellyfishMoves by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val batWingFlap by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val batShriek by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val catJump by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }
    val catScratch by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump")!! }

    val checkPointActivated by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Checkpoint activates (health restored, game saved)")!! }
    val windBlowing by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Wind blows (loop)")!! }

    val skyMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Sky")!! }
    val cavesMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Caves")!! }
    val ambienceMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Ambience Strings")!! }
    val gauntletMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Normal Fight")!! }
    val bossBattle by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/BossBattle")!! }
    val templeMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Temple")!! }
    val titleMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Title screen")!! }
    val creditsMusic by preparePlain(order = 2) { studioSystem.getEvent("event:/Music/Credits")!! }
    val playerStateParameter by lazy { studioSystem.getParameterDescriptionByName("Player state").id }
    val inStoryParameter by lazy { studioSystem.getParameterDescriptionByName("InStory").id }
}
