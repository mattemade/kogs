package net.mattemade.latencytest.asset

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import com.littlekt.file.vfs.readAudioClipEx
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.FMOD_FS_createPreloadedFile
import net.mattemade.fmod.FMOD_Module_Create
import net.mattemade.fmod.FMOD_Studio_System_Create
import net.mattemade.fmod.FmodBank
import net.mattemade.fmod.FmodEventDescription
import net.mattemade.fmod.FmodStudioSystem
import net.mattemade.fmod.FmodStudioSystemCore
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import kotlin.collections.component1
import kotlin.collections.component2

class Assets(
    context: Context,
    fmodFolderPrefix: String,
) : AssetPack(context) {
    private val runtimeTextureAtlasPacker =
        RuntimeTextureAtlasPacker(context, useMiMaps = false, allowFiltering = true).releasing()

    val sound by pack(order = 0) {
        Sound(context)
    }
    val fmod by pack(order = 0) {
        Fmod(context, fmodFolderPrefix)
    }
/*    val fmodAssets by pack(order = 1) {
        FmodAssets(context, fmodFolderPrefix, fmod)
    }*/
}

private val FMOD_BANKS = listOf(
    "Master.bank",
    "Master.strings.bank",
)

class Sound(context: Context): AssetPack(context) {
    val pluck by prepare { context.vfs["sound/pluck.wav"].readAudioClipEx() }
}

class Fmod(context: Context, fmodFolderPrefix: String) : AssetPack(context) {

    lateinit var studioSystem: FmodStudioSystem
    lateinit var system: FmodStudioSystemCore
    private var studioSystemReady = false

    private val module by selfPreparePlain(order = 0, tag = "module", {
        FMOD_Module_Create({
            FMOD_BANKS.forEach {
                FMOD_FS_createPreloadedFile("${fmodFolderPrefix}fmod/${it}")
            }
        }) {
            studioSystem = FMOD_Studio_System_Create()
            val core = studioSystem.coreSystem
            system = core
            // 128 is much better!
            core.setDSPBufferSize(128, 2)
            val driver = core.getDriverInfo(0)
            println(driver.systemRate)
            core.setSoftwareFormat(driver.systemRate, FMOD.SPEAKERMODE_DEFAULT, 0)

            studioSystem.coreSystem.setOutput(FMOD.OUTPUTTYPE_ASIO)
            studioSystem.initialize(
                maxChannels = 128,
                studioInitFlags = FMOD.STUDIO_INIT_NORMAL or FMOD.STUDIO_INIT_SYNCHRONOUS_UPDATE,
                initFlags = FMOD.INIT_NORMAL,
                extraDriverData = null
            )

            studioSystemReady = true
        }
    }) {
        studioSystemReady
    }


}

class FmodAssets(
    context: Context,
    fmodFolderPrefix: String,
    fmod: Fmod,
) : AssetPack(context) {

    private val studioSystem = fmod.studioSystem

    val map = ConcurrentMutableMap<String, FmodBank>()
    val eventCache = ConcurrentMutableMap<String, FmodEventDescription?>()

    fun getEvent(name: String): FmodEventDescription? = eventCache.getOrPut(name) { studioSystem.getEvent(name) }

    private var order = 0
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
            value.loadSampleData()
        }
    }) {
        map.all { (key, value) ->
            value.sampleLoadingState == FMOD.STUDIO_LOADING_STATE_LOADED
        }
    }

    val pluck by lazy { studioSystem.getEvent("event:/Pluck")!! }
    val music by lazy { studioSystem.getEvent("event:/Music")!! }
}
