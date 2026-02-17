package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.littlekt.math.MutableVec2f
import org.jbox2d.dynamics.Body

class PhysicsComponent(
    var body: Body,
    val previousPosition: MutableVec2f = MutableVec2f(body.position.x, body.position.y),
): Component<PhysicsComponent> {

    override fun type(): ComponentType<PhysicsComponent> = PhysicsComponent

    override fun World.onRemove(entity: Entity) {
        body.userData = null
        body.destroyBody()
    }

    companion object: ComponentType<PhysicsComponent>()
}