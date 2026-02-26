package net.mattemade.platformer

import com.littlekt.Context
import com.littlekt.math.Rect
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
) {

    val assets = PlatformerAssets(context, this, getFromUrl, fmodFolderPrefix, fmodLiveUpdate, overrideResourcesFrom)
    val fmodAssets by lazy { FmodAssets(context, fmodFolderPrefix, this) }
    val scheduler = Scheduler()
    var canvasZoom: Float = 1f
    val worldSize = Rect()

    private var tag =
        context.vfs.loadString("tag") ?: Random.nextInt().toString().also {
            context.vfs.store("tag", it)
        }
    private var run = Random.nextInt().toString()

    fun log(log: String) {
        sendLog("$LOG_TAG|$tag|$run|$log")
    }

    companion object {
        private val LOG_TAG = "mgmt"
    }
}