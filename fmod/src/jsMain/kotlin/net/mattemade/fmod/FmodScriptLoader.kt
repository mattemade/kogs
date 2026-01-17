package net.mattemade.fmod

import kotlinx.browser.document

class FmodScriptLoader(val onReadyCallback: () -> Unit) {

    var isLoaded = false
        private set

    init {
        val iframe = document.createElement("iframe")
        val script = iframe.asDynamic().contentWindow.document.createElement("script")

    }
}