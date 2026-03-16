package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.math.Rect
import com.littlekt.util.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mattemade.platformer.component.TaggedText
import net.mattemade.platformer.ink.InkStory
import net.mattemade.platformer.service.StoryDisplayService
import net.mattemade.fmod.FMOD
import net.mattemade.fmod.Fmod3DAttributes
import net.mattemade.fmod.FmodEventDescription
import net.mattemade.fmod.FmodEventInstance
import net.mattemade.platformer.input.GameInput
import net.mattemade.platformer.input.bindInputs
import net.mattemade.platformer.scene.PlatformingScene
import net.mattemade.utils.Scheduler
import kotlin.math.absoluteValue
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
    val inkStoryFactory: net.mattemade.platformer.ink.InkStoryFactory,
) {

    var currentlyPlayingMusicDescription: FmodEventDescription? = null
    var currentlyPlayingMusic: FmodEventInstance? = null
    val assets = PlatformerAssets(context, this, getFromUrl, fmodFolderPrefix, fmodLiveUpdate, overrideResourcesFrom)
    val fmodAssets by lazy { FmodAssets(context, fmodFolderPrefix, this) }
    val storyDisplayService by lazy { StoryDisplayService(context, assets) }
    val scheduler = Scheduler()
    var canvasZoom: Float = 1f
    var canvasInverseZoom: Float = 1f
    val worldSize = Rect(
        x = 50000000000f,
        y = 50000000000f,
        width = -100000000000f,
        height = -100000000000f
    ) // to ensure the world edges will be within
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

    private var _story: InkStory? = null
    val story: InkStory
        get() {
            if (_story == null) {
                _story = inkStoryFactory.createStory(assets.storyString)
            }
            return _story!!
        }

    fun save() {
        val newState = story.getState()
        gameState.story.state = newState
        val state = json.encodeToString(gameState)
        if (previousSavedState != state) {
            log("save|${gameState.checkpoint}|${gameState.sword}|${gameState.waterPearl}|${gameState.airPearl}|${PlatformingScene.collectedPearls}")
            println("saving $state")
            context.vfs.store("save", state)
            previousSavedState = state
        }
    }

    fun load(forceRestart: Boolean = false, reset: Boolean = false) {
        _story = null
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
                val decoded: GameState = json.decodeFromString(it)
                decoded.story.state?.let { storyState ->
                    story.loadState(storyState)
                }
                decoded
            } catch (e: Exception) {
                // this looks dangerous? or?
                e.printStackTrace()
                null
            }
        } ?: GameState().also { println("No save found, creating new GameState") }
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
        var stories: MutableMap<String, Boolean> = mutableMapOf(),
        var currentRoom: String = FIRST_LEVEL_NAME,
        var checkpoint: Int = -1,
        var story: StoryState = StoryState(),
    )

    @Serializable
    data class StoryState(
        var state: String? = null,
        var history: List<TaggedText> = emptyList(),
        var options: List<TaggedText> = emptyList(),
        var goOn: Boolean = true,
    )

    @Serializable
    data class RoomState(
        var isVisited: Boolean = false,
        var gauntletCompleted: Boolean? = null,
    )


    var musicType: String? = null
        set(value) {
            if (field != value) {
                field = value; updateMusic()
            }
        }
    var swimmingMusic: Boolean = false
        set(value) {
            if (field != value) {
                field = value; updateState()
            }
        }
    var lowStamina: Boolean = false
        set(value) {
            if (field != value) {
                field = value; updateState()
            }
        }
    var lowHealth: Boolean = false
        set(value) {
            if (field != value) {
                field = value; updateState()
            }
        }
    var enemiesInTheRoom: Boolean = false
        set(value) {
            if (field != value) {
                field = value; updateState()
            }
        }
    var fightingInTheRoom: Boolean = false
        set(value) {
            if (field != value) {
                field = value; updateState()
            }
        }
    private var currentInStory = 0f
    var inStory: Float = 0f
        set(value) {
            if (field != value) {
                scheduler.forceStop(inStoryChaningTag)
                val currentInStory = currentInStory
                val diff = value - currentInStory
                val timeToSwitch = 1f * diff.absoluteValue // up to 1 seconds
                scheduler.schedule(inStoryChaningTag).then(timeToSwitch) {
                    this.currentInStory = currentInStory + diff * it
                    assets.fmod.studioSystem.setParameterByID(fmodAssets.inStoryParameter, currentInStory + diff * it, 0)
                }
                field = value
            }
        }


    fun updateMusic() {
        println("music; $musicType")
        when (musicType) {
            "sky" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.skyMusic)
            "caves" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.cavesMusic)
            "strings" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.ambienceMusic)
            "temple" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.templeMusic)
            "gauntlet" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.gauntletMusic)
            "boss" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.bossBattle)
            "title" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.titleMusic)
            "credits" -> whenCurrentDoesNotMatchStopItAndStartAnother(fmodAssets.creditsMusic)
            "silence" -> stopCurrentMusic()
            else -> { /* no-op */ }
        }
    }

    fun updateState() {
        val state = if (lowHealth) {
            if (swimmingMusic) {
                if (fightingInTheRoom) {
                    "Swimming, fighting, low health"
                } else if (enemiesInTheRoom) {
                    "Swimming, enemy nearby, low health"
                } else {
                    "Swimming, low health"
                }
            } else {
                if (fightingInTheRoom) {
                    "Walking, fighting, low health"
                } else if (enemiesInTheRoom) {
                    "Walking, enemy nearby, low health"
                } else {
                    "Walking, low health"
                }
            }
        } else {
            if (swimmingMusic) {
                if (fightingInTheRoom) {
                    "Swimming, fighting"
                } else if (lowStamina) {
                    "Swimming, low stamina"
                } else if (enemiesInTheRoom) {
                    "Swimming, enemy nearby"
                } else {
                    "Swimming"
                }
            } else {
                if (fightingInTheRoom) {
                    "Walking, fighting"
                } else if (enemiesInTheRoom) {
                    "Walking, enemy nearby"
                } else {
                    "Walking"
                }
            }
        }

        println("state: $state")
        assets.fmod.studioSystem.setParameterByIDWithLabel(fmodAssets.playerStateParameter, state, 0)
    }

    fun stopCurrentMusic() {
        currentlyPlayingMusic?.stop(FMOD.FMOD_STUDIO_STOP_ALLOWFADEOUT)
        currentlyPlayingMusic?.release()
        currentlyPlayingMusic = null
        currentlyPlayingMusicDescription = null
    }

    fun whenCurrentDoesNotMatchStopItAndStartAnother(description: FmodEventDescription) {
        if (description == currentlyPlayingMusicDescription) {
            return
        }

        stopCurrentMusic()

        currentlyPlayingMusicDescription = description
        currentlyPlayingMusic = description.createInstance()
        currentlyPlayingMusic?.start()
    }

    companion object {
        private val LOG_TAG = "mgmt"
        private val inStoryChaningTag = "instorychanging"

        val sharedAttributes: Fmod3DAttributes by lazy {
            Fmod3DAttributes().apply {
                forward.apply { z = 1f }
                up.apply { y = 1f }
            }
        }

    }
}