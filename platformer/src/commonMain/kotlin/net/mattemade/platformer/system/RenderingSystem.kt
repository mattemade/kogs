package net.mattemade.platformer.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.github.quillraven.fleks.collection.compareEntity
import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledLayer
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.graphics.util.BlendMode
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import com.littlekt.util.Scaler
import com.littlekt.util.fastForEach
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScalingViewport
import net.mattemade.fmod.Fmod3DAttributes
import net.mattemade.platformer.HALF_WORLD_UNIT_HEIGHT
import net.mattemade.platformer.HALF_WORLD_UNIT_WIDTH
import net.mattemade.platformer.PlatformerGameContext
import net.mattemade.platformer.UNITS_PER_PIXEL
import net.mattemade.platformer.WORLD_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_HEIGHT
import net.mattemade.platformer.WORLD_UNIT_WIDTH
import net.mattemade.platformer.WORLD_WIDTH
import net.mattemade.platformer.component.Box2DPhysicsComponent
import net.mattemade.platformer.component.ContextComponent
import net.mattemade.platformer.component.KnockbackComponent
import net.mattemade.platformer.component.MoveComponent
import net.mattemade.platformer.component.PlayerComponent
import net.mattemade.platformer.component.PositionComponent
import net.mattemade.platformer.component.RotationComponent
import net.mattemade.platformer.component.SpriteComponent
import net.mattemade.platformer.px
import net.mattemade.utils.msdf.MsdfFontRenderer
import net.mattemade.utils.render.PixelRender
import kotlin.math.abs
import kotlin.time.Duration

class RenderingSystem(
    private val context: Context = inject(),
    private val gameContext: PlatformerGameContext = inject(),
    val map: TiledMap = inject(),
) : IteratingSystem(
    family = family { all(PositionComponent, RotationComponent, SpriteComponent) },
    comparator = compareEntity { left, right -> left[SpriteComponent].priority.compareTo(right[SpriteComponent].priority) }) {

    private val viewport = ScalingViewport(
        scaler = Scaler.Stretch(),
        width = WORLD_WIDTH,
        height = WORLD_HEIGHT,
        virtualWidth = WORLD_UNIT_WIDTH,
        virtualHeight = WORLD_UNIT_HEIGHT
    )
    private val camera = viewport.camera
    private val playerLightPosition = MutableVec2f()
    private val batch = SpriteBatch(context)
    private val shapeRenderer = ShapeRenderer(batch, slice = gameContext.assets.textureFiles.whitePixel)
    private val mapScale = 1f / map.tileWidth
    private val playerLayerIndex = map.layers.indexOfFirst { it.name == "player-spawn" }
    private val mapLayers = map.layers.size

    private val mapFillsWidth = map.width >= WORLD_UNIT_WIDTH
    private val mapFillsHeight = map.height >= WORLD_UNIT_HEIGHT
    private val minCameraPosition = Vec2f(
        if (mapFillsWidth) HALF_WORLD_UNIT_WIDTH else map.width * 0.5f,
        if (mapFillsHeight) HALF_WORLD_UNIT_HEIGHT else map.height * 0.5f,
    )
    private val maxCameraPosition = Vec2f(
        if (mapFillsWidth) map.width - HALF_WORLD_UNIT_WIDTH else minCameraPosition.x,
        if (mapFillsHeight) map.height - HALF_WORLD_UNIT_HEIGHT else minCameraPosition.y,
    )
    private val fontRenderer = MsdfFontRenderer(gameContext.assets.font.fredokaMsdf)

    private val fmodListenerAttributes = Fmod3DAttributes().apply {
        forward.apply { x = 0f; y = 0f; z = 1f; }
        up.apply { x = 0f; y = 1f; z = 0f; }
    }

    init {
        if (sharedLightRenderer == null) {
            sharedLightRenderer = PixelRender(
                gameContext.context,
                targetWidth = WORLD_WIDTH,
                targetHeight = WORLD_HEIGHT,
                virtualWidth = WORLD_UNIT_WIDTH,
                virtualHeight = WORLD_UNIT_HEIGHT,
                preRenderCall = { dt, camera -> },
                renderCall = { dt, camera, batch, shapeRenderer -> }
            )
        }
    }

    override fun onTick() {
        context.gl.clearColor(Color.BLACK)
        context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        val playerPosition = family.first { it.getOrNull(PlayerComponent) != null }[PositionComponent].position
        playerLightPosition.set(playerPosition)
        val cameraX = playerPosition.x.clamp(minCameraPosition.x, maxCameraPosition.x)
        val cameraY = playerPosition.y.clamp(minCameraPosition.y, maxCameraPosition.y)
        camera.position.set(
            cameraX.px,
            cameraY.px,
            0f
        )
        fmodListenerAttributes.position.apply {
            x = cameraX
            y = cameraY
        }
        gameContext.assets.fmod.studioSystem.setListenerAttributes(0, fmodListenerAttributes)

        var hasLight = false
        (map.layers.firstOrNull { it.name == light } as? TiledObjectLayer)?.let {
            hasLight = true
            sharedLightRenderer?.render(it, 0f.seconds, ::prerenderLight, ::renderLight)
        }

        //camera.position.set(WORLD_UNIT_WIDTH * 0.5f, WORLD_UNIT_HEIGHT * 0.5f, 0f)
        viewport.apply(context)
        batch.begin(camera.viewProjection)
        renderLevel(from = 0, to = playerLayerIndex)
        super.onTick() // tick to render entities
        renderLevel(from = playerLayerIndex + 1, to = mapLayers)
        renderSideBars() // to cover any sprite that goes out-of-bounds

        if (hasLight) {
            sharedLightRenderer?.texture?.let {
                batch.setBlendFunction(BlendMode.Multiply)
                batch.draw(
                    it,
                    x = camera.position.x - HALF_WORLD_UNIT_WIDTH,
                    y = camera.position.y - HALF_WORLD_UNIT_HEIGHT,
                    width = WORLD_UNIT_WIDTH,
                    height = WORLD_UNIT_HEIGHT,
                    flipY = true,
                )
                batch.setToPreviousBlendFunction()
            }
        }

        batch.end()
    }

    private fun renderLevel(from: Int, to: Int) {
        for (i in from until to) {
            renderLevelLayer(i)
        }
    }

    private fun renderLevelLayer(i: Int) {
        val layer = map.layers[i]
        if (layer.name == fakeWalls || layer.name == light) {
            return
        }
        renderLayer(layer, batch)
    }

    private fun renderLayer(layer: TiledLayer, batch: Batch) {
        val xOffset = (1f - layer.parallaxFactor.x) * (camera.position.x - minCameraPosition.x)
        val yOffset = (1f - layer.parallaxFactor.y) * (camera.position.y - minCameraPosition.y)
        layer.render(batch, camera = camera, x = xOffset.px, y = yOffset.px, scale = mapScale, displayObjects = true)
    }

    private fun renderSideBars() {
        if (!mapFillsWidth) { // fill black to left and right of the map
            shapeRenderer.filledRectangle(
                x = minCameraPosition.x - map.width * 0.5f,
                y = camera.position.y - HALF_WORLD_UNIT_HEIGHT,
                width = -WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
            shapeRenderer.filledRectangle(
                x = minCameraPosition.x + map.width * 0.5f,
                y = camera.position.y - HALF_WORLD_UNIT_HEIGHT,
                width = WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
        }
        if (!mapFillsHeight) { // fill black above and below of the map
            shapeRenderer.filledRectangle(
                x = camera.position.x - HALF_WORLD_UNIT_WIDTH,
                y = minCameraPosition.y - map.height * 0.5f,
                width = WORLD_UNIT_WIDTH,
                height = -WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
            shapeRenderer.filledRectangle(
                x = camera.position.x - HALF_WORLD_UNIT_WIDTH,
                y = minCameraPosition.y + map.height * 0.5f,
                width = WORLD_UNIT_WIDTH,
                height = WORLD_UNIT_HEIGHT,
                color = sideBarColor,
            )
        }
    }

    private val animationEvents = mutableListOf<String>()

    override fun onTickEntity(entity: Entity) {
        val spriteComponent = entity[SpriteComponent]
        val bounds = spriteComponent.bounds
        val (position) = entity[PositionComponent]
        val (rotation) = entity[RotationComponent]

        val angle = rotation.radians
/*        val tint = spriteComponent.tint
        tempVec2f.set(bounds.x, bounds.y).rotate(angle)
        shapeRenderer.filledRectangle(
            x = (position.x + bounds.x).px,
            y = (position.y + bounds.y).px,
            width = bounds.width,
            height = bounds.height,
            color = bottomColor,
            color2 = bottomColor,
            color3 = tint,
            color4 = tint,
            rotation = angle
        )*/

        var animationTimeMultiplier = 1f
        entity.getOrNull(ContextComponent)?.let { context ->
            entity.getOrNull(Box2DPhysicsComponent)?.let { physicsComponent ->
                val body = physicsComponent.body
                spriteComponent.currentAnimation = if (entity.getOrNull(KnockbackComponent) != null) {
                    spriteComponent.hurtAnimation
                } else if (context.swimming) {
                    entity.getOrNull(MoveComponent)?.let { move ->
                        //animationTimeMultiplier = abs(move.moveDirection.x / move.maxMoveSpeed)//.clamp(0.5f, 2f)
                        if (context.dashing) {
                            spriteComponent.swimDashAnimation
                        } else if (move.moveDirection.x != 0f || move.moveDirection.y != 0f) {
                            spriteComponent.swimAnimation
                        } else {
                            spriteComponent.swimIdleAnimation
                        }
                    } ?: spriteComponent.swimAnimation
                } else if (context.standing && body.linearVelocityY == 0f) { // grounded
                    if (body.linearVelocityX == 0f) {
                        spriteComponent.idleAnimation
                    } else if (context.dashing) {
                        spriteComponent.fallAnimation
                    } else {
                        entity.getOrNull(MoveComponent)?.let { move ->
                            animationTimeMultiplier = abs(move.moveDirection.x / move.maxMoveSpeed)//.clamp(0.5f, 2f)
                        }
                        spriteComponent.walkAnimation
                    }
                } else if (body.linearVelocityY < 0f) {
                    spriteComponent.jumpAnimation
                } else if (body.linearVelocityY > 0f) {
                    if (gameContext.gameState.airPearl && context.wallSlide) {
                        spriteComponent.wallSlideAnimation
                    } else {
                        spriteComponent.fallAnimation
                    }
                } else if (context.dashing) {
                    if (context.swimming) {
                        spriteComponent.swimDashAnimation
                    } else if (body.linearVelocityX != 0f) {
                        spriteComponent.airDashAnimation
                    } else {
                        // we are dashing with no speed!! probably it's a remaining state of dash-to-wall-slide
                        spriteComponent.currentAnimation
                    }
                } else {
                    spriteComponent.currentAnimation
                }
            }


            val (currentAnimation, offset, scale) = spriteComponent.currentAnimation

            currentAnimation.update((deltaTime * animationTimeMultiplier).seconds, animationEvents::add)
            entity.getOrNull(Box2DPhysicsComponent)?.let { physicsComponent ->
                animationEvents.fastForEach {
                    spriteComponent.animationEventCallback.invoke(it, physicsComponent)
                }
            }
            animationEvents.clear()

            if (spriteComponent.visible) {
                tempVec2f.set(
                    -offset.x * UNITS_PER_PIXEL,
                    -offset.y * UNITS_PER_PIXEL + bounds.height * 0.5f, // to put the sprite on the ground
                ).rotate(angle)
                currentAnimation.currentKeyFrame?.let {
                    batch.draw(
                        slice = it,
                        x = (position.x + tempVec2f.x).px,
                        y = (position.y + tempVec2f.y).px,
                        width = it.width * UNITS_PER_PIXEL * scale,
                        height = it.height * UNITS_PER_PIXEL * scale,
                        rotation = angle,
                        flipX = !context.facingRight,
                    )
                }
            }
        }
    }

    private fun prerenderLight(level: TiledObjectLayer, dt: Duration, camera: Camera) {
        camera.position.set(this.camera.position)
    }

    private fun renderLight(
        layer: TiledObjectLayer,
        dt: Duration,
        camera: Camera,
        batch: Batch,
        shapeRenderer: ShapeRenderer
    ) {
        gameContext.context.gl.clearColor(Color.BLACK)
        gameContext.context.gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        val ambientColor = (layer.properties[abmient] as? TiledMap.Property.ColorProp)?.value ?: Color.BLACK
        shapeRenderer.filledRectangle(x = playerLightPosition.x - HALF_WORLD_UNIT_WIDTH, y = playerLightPosition.y - HALF_WORLD_UNIT_HEIGHT, width = WORLD_UNIT_WIDTH, height = WORLD_UNIT_HEIGHT, color = ambientColor.toFloatBits())
        //batch.setBlendFunction(BlendMode.Add)
        renderLayer(layer, batch)
        shapeRenderer.filledEllipse(x = playerLightPosition.x, y = playerLightPosition.y, rx = 8f, ry = 8f, innerColor = innerPlayerLight, outerColor = outerPlayerLight)
       // batch.setToPreviousBlendFunction()
    }

    companion object {
        private val tempVec2f = MutableVec2f()
        private val sideBarColor = Color.BLACK.toFloatBits()
        private val topColor = Color.RED.toFloatBits()
        private val bottomColor = Color.WHITE.toMutableColor().apply { a = 0.2f }.toFloatBits()
        private val fakeWalls = "FAKE WALLS"
        private val light = "light"
        private val abmient = "ambient"
        private val innerPlayerLight = Color.WHITE.toFloatBits()
        private val outerPlayerLight = Color.WHITE.toMutableColor().apply { a = 0f }.toFloatBits()

        private var sharedLightRenderer: PixelRender? = null
    }

}