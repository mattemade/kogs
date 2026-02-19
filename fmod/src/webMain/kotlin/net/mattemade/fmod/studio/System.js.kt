package net.mattemade.fmod.studio

actual class System {
    actual fun setAdvancedSettings(settings: StudioAdvancedSettings) {
    }

    actual fun setAdvancedSettings(setup: StudioAdvancedSettingsDsl.() -> Unit) {
    }
}