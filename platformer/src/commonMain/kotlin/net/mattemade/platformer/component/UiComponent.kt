package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class UiComponent(
    var showMap: Boolean = false,
): Component<UiComponent> {
    override fun type() = UiComponent
    companion object: ComponentType<UiComponent>()
}