package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class _TemplateComponent(
    
): Component<_TemplateComponent> {
    override fun type() = _TemplateComponent
    companion object: ComponentType<_TemplateComponent>()
}