package net.mattemade.platformer

import com.littlekt.Context
import net.mattemade.fmod.FMOD
import net.mattemade.utils.asset.AssetPack

class FmodAssets(
    context: Context,
    fmodFolderPrefix: String,
    private val gameContext: PlatformerGameContext,
) : AssetPack(context) {
    val studioSystem = gameContext.assets.fmod.studioSystem
    val bank by selfPreparePlain(order = 0, action = {
        println("bank started")
        studioSystem.loadBankFile("${fmodFolderPrefix}fmod/Master.bank", FMOD.STUDIO_LOAD_BANK_NONBLOCKING)
    }) {
        val loadingState = it.loadingState
        println("bank checking: $loadingState")
        (loadingState == FMOD.STUDIO_LOADING_STATE_LOADED)
    }
    val bankStrings by selfPreparePlain(order = 0, action = {
        println("strings bank started")
        studioSystem.loadBankFile("${fmodFolderPrefix}fmod/Master.strings.bank", FMOD.STUDIO_LOAD_BANK_NONBLOCKING)
    }) {
        val loadingState = it.loadingState
        println("strings bank checking: $loadingState")
        (loadingState == FMOD.STUDIO_LOADING_STATE_LOADED)
    }
    val sampleData by selfPreparePlain(order = 1, action = {
        println("sample started")
        bank.loadSampleData()
    }) {
        val sampleLoadingState = bank.sampleLoadingState
        println("sample checking: $sampleLoadingState")
        sampleLoadingState == FMOD.STUDIO_LOADING_STATE_LOADED
    }
    val eventDescription by preparePlain(order = 2) {
        studioSystem.getEvent("event:/drum")
    }
    val musicEventDescription by preparePlain(order = 2) {
        studioSystem.getEvent("event:/Music/test")
    }
}
