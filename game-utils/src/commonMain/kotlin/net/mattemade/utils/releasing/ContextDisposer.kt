package net.mattemade.utils.releasing

fun interface ContextReleaser {
    fun release(context: Any?)
}
