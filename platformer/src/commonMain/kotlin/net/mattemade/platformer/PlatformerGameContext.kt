package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.math.Rect
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mattemade.utils.Scheduler
import kotlin.random.Random

class PlatformerGameContext(
    val context: Context,
    private val sendLog: (String) -> Unit,
    val encodeUrlComponent: (String) -> String,
    val getFromUrl: (String) -> List<String>?,
    val overrideResourcesFrom: String?,
    val fmodFolderPrefix: String,
    val fmodLiveUpdate: Boolean,
    val restartScene: () -> Unit,
) {

    val assets = PlatformerAssets(context, this, getFromUrl, fmodFolderPrefix, fmodLiveUpdate, overrideResourcesFrom)
    val fmodAssets by lazy { FmodAssets(context, fmodFolderPrefix, this) }
    val scheduler = Scheduler()
    var canvasZoom: Float = 1f
    val worldSize = Rect()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private var previousSavedState: String? = null
    lateinit var gameState: GameState


    private var tag =
        context.vfs.loadString("tag") ?: Random.nextInt().toString().also {
            context.vfs.store("tag", it)
        }
    private var run = Random.nextInt().toString()

    fun log(log: String) {
        sendLog("$LOG_TAG|$tag|$run|$log")
    }

    fun save() {
        val state = json.encodeToString(gameState)
        if (previousSavedState != state) {
            println("saving $state")
            context.vfs.store("save", state)
            previousSavedState = state
        }
    }

    fun load(forceRestart: Boolean = false, reset: Boolean = false) {
        if (reset) {
            previousSavedState = null
            gameState = GameState()
            save()
            restartScene()
            return
        }

        gameState = context.vfs.loadString("save")?.let {
            try {
                previousSavedState = it
                json.decodeFromString(it)
            } catch (_: Exception) {
                null
            }
        } ?: GameState()
        if (forceRestart) {
            restartScene()
        }
    }

    @Serializable
    data class GameState(
        var roomStates: MutableMap<String, RoomState> = mutableMapOf(),
        var waterPearl: Boolean = false,
        var airPearl: Boolean = false,
        var currentRoom: String = FIRST_LEVEL_NAME,
    )

    @Serializable
    data class RoomState(
        var isVisited: Boolean = false,
    )


    companion object {
        private val LOG_TAG = "mgmt"
    }
}