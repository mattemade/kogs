package net.mattemade.platformer.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.littlekt.math.PI2_F
import net.mattemade.fmod.FmodEventInstance
import kotlin.random.Random

class PickupComponent(
    var floatingTime: Float = PI2_F * Random.nextFloat(),
): Component<PickupComponent> {
    override fun type() = PickupComponent
    companion object: ComponentType<PickupComponent>()
}