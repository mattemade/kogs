package net.mattemade.utils.asset

import com.littlekt.AssetProvider
import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.Releasable
import com.littlekt.SelfPreparingGameAsset
import korlibs.datastructure.FastIntMap
import korlibs.datastructure.fastValueForEach
import korlibs.datastructure.get
import korlibs.datastructure.getOrPut
import net.mattemade.utils.animation.SignallingAnimationPlayer
import net.mattemade.utils.animation.readAnimationPlayer
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self

open class AssetPack(protected val context: Context, private val defaultAnimationCallback: ((String) -> Unit)? = null) :
    Releasing by Self() {

    private var currentOrder = 0
    private var maxOrder = 0
    private val orderedProviders = FastIntMap<MutableList<AssetProvider>>()

    // create a new provider each time, so each asset could be loaded independently
    private fun createProvider(order: Int = 0, tag: String? = null): AssetProvider =
        AssetProvider(context, tag).also {
            orderedProviders.getOrPut(order) { mutableListOf() } += it
            maxOrder = maxOf(maxOrder, order)
        }

    private var providerWasFullyLoaded = false
    val isLoaded: Boolean
        get() =
            if (providerWasFullyLoaded) {
                true
            } else {
                var result = true
                var stop = false
                while (result && !stop) {
                    val assetProviders = orderedProviders[currentOrder]
                    val currentProvidersCount = assetProviders?.size ?: 0
                    if (currentProvidersCount == 0) {
                        if (currentOrder >= maxOrder) {
                            providerWasFullyLoaded = true
                            stop = true
                        } else {
                            currentOrder++
                        }
                    } else {
                        for (i in 0 until currentProvidersCount) {
                            assetProviders?.get(i)?.update()
                        }
                        assetProviders?.forEach {
                            if (!it.fullyLoaded) {
                                it.update()
                                result = false
                            }
                        }
                        if (result) {
                            currentOrder++
                        } else {
                            stop = true
                        }
                    }
                }
                result
            }

    fun <T : Any> preparePlain(tag: String? = null, order: Int = 0, action: suspend () -> T): PreparableGameAsset<T> =
        createProvider(order, tag).prepare { action() }

    fun <T : Releasable> prepare(order: Int = 0, tag: String? = null, action: suspend () -> T): PreparableGameAsset<T> =
        createProvider(order, tag).prepare { action().releasing() }

    fun <T : Any> selfPreparePlain(order: Int = 0, tag: String? = null, action: () -> T, prepared: (T) -> Boolean): SelfPreparingGameAsset<T> =
        createProvider(order, tag).selfPrepare({ action() }, { prepared(it) } )

    protected fun String.prepareAnimationPlayer(
        runtimeTextureAtlasPacker: RuntimeTextureAtlasPacker,
        order: Int = 0,
        callback: ((String) -> Unit)? = defaultAnimationCallback
    ): PreparableGameAsset<SignallingAnimationPlayer> =
        createProvider(order).prepare { this.readAnimationPlayer(runtimeTextureAtlasPacker, callback) }

    protected suspend fun String.readAnimationPlayer(
        runtimeTextureAtlasPacker: RuntimeTextureAtlasPacker,
        callback: ((String) -> Unit)? = defaultAnimationCallback
    ): SignallingAnimationPlayer =
        context.resourcesVfs[this].readAnimationPlayer(runtimeTextureAtlasPacker, callback)

    fun <T : AssetPack> T.packed(order: Int = 0): T {
        this@AssetPack.orderedProviders.getOrPut(order) { mutableListOf() }.apply {
            orderedProviders.fastValueForEach { addAll(it) }
        }
        return this
    }

    fun <T : AssetPack> pack(order: Int = 0, action: suspend () -> T): PreparableGameAsset<T> =
        createProvider(order).prepare {
            val assetPack = action()
            assetPack.orderedProviders.fastValueForEach { it.forEach { it.update() } }
            assetPack.packed(order).releasing()
        }
}
