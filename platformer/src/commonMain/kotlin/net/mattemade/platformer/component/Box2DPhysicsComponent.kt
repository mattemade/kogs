package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.littlekt.math.MutableVec2f
import net.mattemade.fmod.Fmod3DAttributes
import net.mattemade.fmod.FmodEventDescription
import net.mattemade.fmod.FmodEventInstance
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.Fixture

class Box2DPhysicsComponent(
    var body: Body,
    val previousPosition: MutableVec2f = MutableVec2f(body.position.x, body.position.y),
    val previousVelocity: MutableVec2f = MutableVec2f(0f, 0f),
    val attachedSounds: MutableList<Pair<FmodEventInstance, Fmod3DAttributes>> = mutableListOf(),
) : Component<Box2DPhysicsComponent> {

    lateinit var landBodyFixture: Fixture
    lateinit var waterBodyFixture: Fixture

    fun playSoundAttached(event: FmodEventDescription): FmodEventInstance {
        val instance = event.createInstance()
        val attributes = Fmod3DAttributes().apply {
            forward.apply { z = 1f }
            up.apply { y = 1f }
            position.apply { x = body.position.x; y = body.position.y; }
            velocity.apply { x = body.linearVelocityX; y = body.linearVelocityY; }
        }
        attachedSounds += Pair(instance, attributes)
        instance.set3DAttributes(attributes)
        instance.start()
        return instance
    }

    fun playSound(event: FmodEventDescription): FmodEventInstance {
        val instance = event.createInstance()
        val attributes = Fmod3DAttributes().apply {
            forward.apply { z = 1f }
            up.apply { y = 1f }
            position.apply { x = body.position.x; y = body.position.y; }
            velocity.apply { x = body.linearVelocityX; y = body.linearVelocityY; }
        }
        attachedSounds += Pair(instance, attributes)
        instance.set3DAttributes(attributes)
        instance.start()
        return instance
    }

    override fun type(): ComponentType<Box2DPhysicsComponent> = Box2DPhysicsComponent

    override fun World.onRemove(entity: Entity) {
        body.userData = null
        body.destroyBody()
    }

    companion object : ComponentType<Box2DPhysicsComponent>()
}