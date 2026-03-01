package net.mattemade.platformer

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.FmodBank
import net.mattemade.utils.asset.AssetPack

class FmodAssets(
    context: Context,
    fmodFolderPrefix: String,
    private val gameContext: PlatformerGameContext,
) : AssetPack(context) {

    private val studioSystem = gameContext.assets.fmod.studioSystem

    val map = ConcurrentMutableMap<String, FmodBank>()

    val preparation by selfPreparePlain(order = 0, action = {
        FMOD_BANKS.forEach { bankName ->
        val bank by selfPreparePlain(order = 0, action = {
            val bank = studioSystem.loadBankFile("${fmodFolderPrefix}fmod/${bankName}", FMOD.STUDIO_LOAD_BANK_NONBLOCKING)
            bank
        }) {
            val result = it.loadingState == FMOD.STUDIO_LOADING_STATE_LOADED
            if (result) {
                map[bankName] = it
            }
            result
        }
    }}) {
        map.size == FMOD_BANKS.size
    }

    val sampleDataPreparation by selfPreparePlain(order = 1, action = {
        map.values.forEach { it.loadSampleData() }
    }) {
        map.values.all { it.sampleLoadingState == FMOD.STUDIO_LOADING_STATE_LOADED }
    }

    val jump by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Jump") }
    val land by preparePlain(order = 2) { studioSystem.getEvent("event:/Main character SFX/Land") }
    val musicEventDescription by preparePlain(order = 2) {
        studioSystem.getEvent("event:/Music/Ocean and caves")
    }
}
