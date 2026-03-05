package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class TimeToLiveComponent(
    var timeToLive: Float,
): Component<TimeToLiveComponent> {
    override fun type() = TimeToLiveComponent
    companion object: ComponentType<TimeToLiveComponent>()
}