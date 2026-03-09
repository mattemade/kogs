package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.math.Rect
import com.littlekt.util.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mattemade.fmod.FmodEventInstance
import net.mattemade.platformer.input.GameInput
import net.mattemade.platformer.input.bindInputs
import net.mattemade.utils.Scheduler
import kotlin.random.Random
import kotlin.time.Duration

class PlatformerGameContext(
    val context: Context,
    private val sendLog: (String) -> Unit,
    val encodeUrlComponent: (String) -> String,
    val getFromUrl: (String) -> List<String>?,
    val overrideResourcesFrom: String?,
    val fmodFolderPrefix: String,
    val fmodLiveUpdate: Boolean,
    val restartScene: () -> Unit,
    val startGame: () -> Unit,
) {

    var currentlyPlayingMusic: FmodEventInstance? = null
    val assets = PlatformerAssets(context, this, getFromUrl, fmodFolderPrefix, fmodLiveUpdate, overrideResourcesFrom)
    val fmodAssets by lazy { FmodAssets(context, fmodFolderPrefix, this) }
    val scheduler = Scheduler()
    var canvasZoom: Float = 1f
    var canvasInverseZoom: Float = 1f
    val worldSize = Rect(x = 50000000000f, y = 50000000000f, width = -100000000000f, height = -100000000000f) // to ensure the world edges will be within
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private var previousSavedState: String? = null
    lateinit var gameState: GameState
    var gameInput = GameInput(context, context.bindInputs())
    var controlsActive = true
    var paused = false

    private var tag =
        context.vfs.loadString("tag") ?: Random.nextInt().toString().also {
            context.vfs.store("tag", it)
        }
    private var run = Random.nextInt().toString()

    fun log(log: String) {
        sendLog("$LOG_TAG|$tag|$run|$log")
    }

    fun update(dt: Duration) {
        scheduler.update(dt.seconds)
    }

    // TODO: oooh, it shouldn't be like that, but well
    // make inputs push-based rather than pull-based, otherwise we lose input fidelity
    // calling it frequent enough would work, though, but it really should be fixed!!!
    fun updateInputs() {
        gameInput.update(controlsActive)
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
        var sword: Boolean = false,
        var pearls: MutableList<Boolean> = mutableListOf(),
        var tutorials: MutableMap<String, Boolean> = mutableMapOf(),
        var currentRoom: String = FIRST_LEVEL_NAME,
        var checkpoint: Int = -1,
    )

    @Serializable
    data class RoomState(
        var isVisited: Boolean = false,
    )


    fun switchMusicState(state: String) {
        println("setting state to $state")
        currentlyPlayingMusic?.setParameterByIDWithLabel(fmodAssets.musicStateParameter, state, 0)
    }

    companion object {
        private val LOG_TAG = "mgmt"
        val stateWalking = "Walking"
        val stateWalkingWithEnemies = "Walking, enemies"
        val stateLowHealth = "Walking, low health"
        val stateSwimming = "Swimming"
        val stateSwimmingLowHealth = "Swimming, low health"


    }
}