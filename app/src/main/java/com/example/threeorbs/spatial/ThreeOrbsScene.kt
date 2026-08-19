package com.example.threeorbs.spatial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.threeorbs.domain.model.TaskStatus
import com.example.threeorbs.ui.today.Panel
import com.example.threeorbs.ui.today.ThreeOrbsEvent
import com.example.threeorbs.ui.today.ThreeOrbsUiState
import com.example.threeorbs.ui.today.components.ThreeOrbsControlLayer
import com.example.threeorbs.ui.today.components.SpatialDateWallPanel
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Matrix4
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.foundation.gesture.SpatialPointerInfo
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialPointerEvent
import com.pico.spatial.ui.platform.Material
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Composable
fun ThreeOrbsScene(
    state: ThreeOrbsUiState,
    onEvent: (ThreeOrbsEvent) -> Unit,
) {
    val context = LocalContext.current
    val scene = remember { ThreeOrbsSceneController() }
    val pressStarts = remember { ConcurrentHashMap<Any, PressStart>() }
    val gestureScope = rememberCoroutineScope()

    DisposableEffect(scene) {
        onDispose { scene.destroy() }
    }
    LaunchedEffect(state.today, state.data.groupX, state.data.groupY, state.data.groupZ) {
        scene.sync(state)
    }
    LaunchedEffect(state.message) {
        if (state.message?.startsWith("星尘") == true) scene.playCompletionTransfer()
    }
    LaunchedEffect(scene) { scene.playNightSkyTwinkle() }

    Box(Modifier.fillMaxSize()) {
        SpatialView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scene) {
                    detectSpatialPointerEvent(
                        context,
                        targetedToEntity = TargetEntity.any { scene.isTaskOrb(it) },
                    ) { infos ->
                        handleOrbPointerEvents(infos, pressStarts, gestureScope, scene, onEvent)
                        true
                    }
                }
                .pointerInput(scene) {
                    detectSpatialDragGesture(
                        context,
                        targetedToEntity = TargetEntity.hit(scene.grabEntity),
                        onDragStart = { scene.beginDrag() },
                        onDragCancel = { scene.endDrag()?.let { (x, y, z) -> onEvent(ThreeOrbsEvent.MoveGroup(x, y, z)) } },
                        onDragEnd = { scene.endDrag()?.let { (x, y, z) -> onEvent(ThreeOrbsEvent.MoveGroup(x, y, z)) } },
                    ) { drag -> scene.dragGroup(drag.dragAmount.x, drag.dragAmount.y, drag.dragAmount.z) }
                },
            initial = { content, attachments ->
                scene.build()
                content.addEntity(scene.root)
                scene.bindAttachments(
                    labels = List(3) { slot -> checkNotNull(attachments.entity("task-label-$slot")) },
                    dateWallUi = checkNotNull(attachments.entity("date-wall-ui")),
                )
                scene.sync(state)
            },
            attachments = {
                state.today.slots.forEachIndexed { slot, task ->
                    AttachmentPanel("task-label-$slot") {
                        TaskOrbLabel(task.text, task.status == TaskStatus.COMPLETED)
                    }
                }
                AttachmentPanel("date-wall-ui") {
                    SpatialDateWallPanel(state) { onEvent(ThreeOrbsEvent.Open(Panel.History)) }
                }
            },
        )
        ThreeOrbsControlLayer(state, onEvent)
    }
}

@Composable
private fun TaskOrbLabel(text: String, completed: Boolean) {
    Box(
        Modifier.width(170.dp).clip(RoundedCornerShape(18.dp))
            .backgroundMaterial(true, Material.Thin).padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = if (completed) "✓ $text" else text.ifBlank { "待填写" },
            modifier = Modifier.width(142.dp),
            color = if (completed) PicoTheme.colorScheme.labelSecondary else PicoTheme.colorScheme.labelPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private data class PressStart(
    val entity: Entity,
    val startedAt: Long,
    var triggered: Boolean = false,
    var thresholdJob: Job? = null,
)

private fun handleOrbPointerEvents(
    infos: List<SpatialPointerInfo>,
    pressStarts: ConcurrentHashMap<Any, PressStart>,
    gestureScope: CoroutineScope,
    scene: ThreeOrbsSceneController,
    onEvent: (ThreeOrbsEvent) -> Unit,
) {
    infos.forEach { info ->
        val pointerId = info.pointerId
        if (info.isDownEvent()) {
            val target = info.targetedEntity ?: return@forEach
            val press = PressStart(target, info.uptimeMillis)
            pressStarts[pointerId] = press
            scene.setPressed(target, true)
            press.thresholdJob = gestureScope.launch {
                delay(LONG_PINCH_MILLIS)
                if (pressStarts[pointerId] === press) {
                    val slot = scene.slotFor(press.entity) ?: return@launch
                    if (scene.canComplete(slot)) {
                        press.triggered = true
                        scene.setPressed(press.entity, false)
                        onEvent(ThreeOrbsEvent.Complete(slot))
                    }
                }
            }
        } else if (info.isUpEvent()) {
            val press = pressStarts.remove(pointerId) ?: return@forEach
            press.thresholdJob?.cancel()
            scene.setPressed(press.entity, false)
            val slot = scene.slotFor(press.entity) ?: return@forEach
            val heldMillis = info.uptimeMillis - press.startedAt
            if (!press.triggered && heldMillis >= LONG_PINCH_MILLIS && scene.canComplete(slot)) {
                onEvent(ThreeOrbsEvent.Complete(slot))
            } else if (!press.triggered && heldMillis < LONG_PINCH_MILLIS) {
                onEvent(ThreeOrbsEvent.Open(Panel.Edit(slot)))
            }
        }
    }
}

private class ThreeOrbsSceneController {
    val root = Entity().apply { setName("ThreeOrbsSpatialRoot") }
    private val group = Entity().apply { setName("TaskOrbGroup") }
    val grabEntity = Entity().apply { setName("TaskOrbGroupGrabHandle") }
    private val dateWall = Entity().apply { setName("DateWall3D") }
    private val constellation = Entity().apply { setName("DateConstellation") }
    private val nightSky = Entity().apply { setName("NightSkyStars") }
    private val taskEntities = mutableListOf<Entity>()
    private val taskMaterials = mutableListOf<PhysicallyBasedMaterial>()
    private val taskGlows = mutableListOf<ModelEntity>()
    private val taskLabels = mutableListOf<Entity>()
    private var dateWallUi: Entity? = null
    private val taskPositions = listOf(
        Vector3(0f, 0.20f, 0.04f),
        Vector3(-0.21f, -0.13f, 0.02f),
        Vector3(0.21f, -0.13f, 0.02f),
    )
    private val resources = mutableListOf<AutoCloseable>()
    private val starEntities = mutableListOf<Entity>()
    private val transferParticles = mutableListOf<Entity>()
    private val nightStars = mutableListOf<Entity>()
    private val slotByEntityId = mutableMapOf<Long, Int>()
    private var latestState: ThreeOrbsUiState? = null
    private var built = false
    private var dragStart = Vector3.ZERO

    private val orbColors = listOf(
        Color4(1.00f, 0.28f, 0.24f, 1f),
        Color4(0.12f, 0.80f, 0.72f, 1f),
        Color4(0.50f, 0.32f, 1.00f, 1f),
    )

    fun build() {
        if (root.hasChild()) return
        root.addChild(group)
        root.addChild(dateWall)
        root.addChild(constellation)
        root.addChild(nightSky)
        group.components[TransformComponent::class.java]?.setPosition(DEFAULT_GROUP_POSITION)
        createTaskOrbs()
        createGrabHandle()
        createDateWall()
        createConstellation()
        createTransferParticles()
        createNightSky()
        built = true
    }

    private fun createTaskOrbs() {
        val sphereMesh = MeshResource.createSphere(ORB_RADIUS).rememberResource()
        val collider = ShapeResource.createSphere(ORB_RADIUS * 1.18f).rememberResource()
        taskPositions.forEachIndexed { slot, position ->
            val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
                setBaseColor(orbColors[slot])
                setEmissiveColor(orbColors[slot] * 0.72f)
                setMetallic(0.10f)
                setRoughness(0.24f)
            }.rememberResource()
            val orb = Entity().apply {
                setName("TaskOrb_$slot")
                components[TransformComponent::class.java]?.setPosition(position)
                components.set(ModelComponent(sphereMesh, material))
                components.set(InteractableComponent())
                components.set(CollisionComponent(listOf(collider), PhysicsMaterialResource()))
                components.set(HoverEffectComponent())
            }
            val glowMaterial = PhysicallyBasedMaterial.create(BlendingMode.TRANSPARENT).apply {
                setBaseColor(orbColors[slot])
                setEmissiveColor(orbColors[slot])
                setOpacity(0.16f)
                setDepthWrite(false)
            }.rememberResource()
            val glow = ModelEntity(sphereMesh, glowMaterial).apply {
                setName("TaskOrbGlow_$slot")
                components[TransformComponent::class.java]?.setScaleVector(Vector3(1.18f))
            }
            orb.addChild(glow)
            group.addChild(orb)
            taskEntities += orb
            taskMaterials += material
            taskGlows += glow
            slotByEntityId[orb.id] = slot
        }
    }

    fun bindAttachments(labels: List<Entity>, dateWallUi: Entity) {
        if (taskLabels.isNotEmpty()) return
        labels.forEachIndexed { slot, label ->
            label.setName("TaskOrbLabel_$slot")
            label.components[TransformComponent::class.java]?.apply {
                setPosition(taskPositions[slot] + Vector3(0f, -0.155f, 0.13f))
                setScaleVector(Vector3(0.58f))
            }
            group.addChild(label)
            taskLabels += label
        }
        dateWallUi.setName("DateWallUI")
        dateWallUi.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(0f, 0f, 0.045f))
            setScaleVector(Vector3(0.53f))
        }
        dateWall.addChild(dateWallUi)
        this.dateWallUi = dateWallUi
    }

    private fun createGrabHandle() {
        val mesh = MeshResource.createTorus(0.15f, 0.022f).rememberResource()
        val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.18f, 0.22f, 0.30f, 1f))
            setEmissiveColor(Color4(0.08f, 0.12f, 0.22f, 1f))
            setMetallic(0.65f)
            setRoughness(0.30f)
        }.rememberResource()
        grabEntity.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(0f, -0.36f, 0.04f))
            setScaleVector(Vector3(1.25f, 0.55f, 1.25f))
        }
        grabEntity.components.set(ModelComponent(mesh, material))
        grabEntity.components.set(InteractableComponent())
        grabEntity.components.set(CollisionComponent(listOf(ShapeResource.createBox(Vector3(0.23f, 0.075f, 0.075f)).rememberResource()), PhysicsMaterialResource()))
        grabEntity.components.set(HoverEffectComponent())
        group.addChild(grabEntity)
    }

    private fun createDateWall() {
        dateWall.components[TransformComponent::class.java]?.setPosition(DEFAULT_DATE_WALL_POSITION)
        val panelMesh = MeshResource.createBox(Vector3(0.17f, 0.28f, 0.018f), 0.025f).rememberResource()
        val panelMaterial = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.055f, 0.075f, 0.13f, 1f))
            setEmissiveColor(Color4(0.015f, 0.025f, 0.07f, 1f))
            setMetallic(0.12f)
            setRoughness(0.48f)
        }.rememberResource()
        dateWall.addChild(ModelEntity(panelMesh, panelMaterial))
        val dotMesh = MeshResource.createSphere(0.018f).rememberResource()
        repeat(3) { index ->
            val dotMaterial = UnlitMaterial.create(BlendingMode.OPAQUE).apply { setBaseColor(Color4(0.23f, 0.30f, 0.42f, 1f)) }.rememberResource()
            dateWall.addChild(ModelEntity(dotMesh, dotMaterial).apply {
                setName("ProgressDot_$index")
                components[TransformComponent::class.java]?.setPosition(Vector3(-0.055f + index * 0.055f, 0.13f, 0.04f))
            })
        }
    }

    private fun createConstellation() {
        constellation.components[TransformComponent::class.java]?.setPosition(Vector3(0.43f, -0.055f, -0.02f))
        val starMesh = MeshResource.createSphere(0.014f).rememberResource()
        val starMaterial = UnlitMaterial.create(BlendingMode.OPAQUE).apply { setBaseColor(Color4(0.55f, 0.86f, 1f, 1f)) }.rememberResource()
        val starPositions = listOf(
            Vector3(-0.09f, 0.05f, 0.035f), Vector3(-0.035f, 0.105f, 0.035f),
            Vector3(0.02f, 0.045f, 0.035f), Vector3(0.085f, 0.095f, 0.035f),
            Vector3(0.055f, -0.005f, 0.035f), Vector3(-0.055f, -0.04f, 0.035f),
        )
        starPositions.forEachIndexed { index, position ->
            val star = ModelEntity(starMesh, starMaterial).apply {
                setName("ConstellationStar_$index")
                components[TransformComponent::class.java]?.setPosition(position)
            }
            constellation.addChild(star)
            starEntities += star
        }
        listOf(0 to 1, 1 to 2, 2 to 3, 2 to 5, 5 to 4, 4 to 3).forEachIndexed { index, (from, to) ->
            val a = starPositions[from]
            val b = starPositions[to]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            val linkMesh = MeshResource.createBox(Vector3(length * 0.5f, 0.0025f, 0.0025f), 0.001f).rememberResource()
            constellation.addChild(ModelEntity(linkMesh, starMaterial).apply {
                setName("ConstellationLink_$index")
                components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, 0.034f))
                    setQuaternion(Matrix4.rotateZByDegrees(Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()).rotation)
                }
            })
        }
        constellation.enabled = false
    }

    private fun createTransferParticles() {
        val mesh = MeshResource.createSphere(0.009f).rememberResource()
        val material = UnlitMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.58f, 0.90f, 1f, 1f))
        }.rememberResource()
        repeat(9) { index ->
            val particle = ModelEntity(mesh, material).apply {
                setName("Stardust_$index")
                enabled = false
            }
            root.addChild(particle)
            transferParticles += particle
        }
    }

    /** A non-interactive volumetric ambience layer: real emissive meshes, no flat bitmap. */
    private fun createNightSky() {
        val mesh = MeshResource.createSphere(0.0065f).rememberResource()
        val material = UnlitMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.66f, 0.84f, 1f, 1f))
        }.rememberResource()
        val positions = listOf(
            Vector3(-0.72f, 0.42f, -0.16f), Vector3(-0.58f, 0.25f, -0.13f),
            Vector3(-0.43f, 0.48f, -0.18f), Vector3(-0.28f, 0.35f, -0.14f),
            Vector3(-0.10f, 0.52f, -0.19f), Vector3(0.08f, 0.38f, -0.15f),
            Vector3(0.27f, 0.50f, -0.17f), Vector3(0.45f, 0.34f, -0.14f),
            Vector3(0.67f, 0.45f, -0.18f), Vector3(-0.67f, 0.06f, -0.15f),
            Vector3(-0.50f, -0.29f, -0.17f), Vector3(-0.31f, -0.43f, -0.14f),
            Vector3(-0.08f, -0.36f, -0.18f), Vector3(0.17f, -0.45f, -0.14f),
            Vector3(0.38f, -0.31f, -0.17f), Vector3(0.62f, -0.12f, -0.15f),
        )
        positions.forEachIndexed { index, position ->
            val star = ModelEntity(mesh, material).apply {
                setName("NightStar_$index")
                components[TransformComponent::class.java]?.apply {
                    setPosition(position)
                    setScaleVector(Vector3(0.70f + (index % 4) * 0.13f))
                }
            }
            nightSky.addChild(star)
            nightStars += star
        }
    }

    suspend fun playNightSkyTwinkle() {
        var current = 0
        while (currentCoroutineContext().isActive) {
            if (nightStars.isEmpty()) return
            val previous = (current - 1 + nightStars.size) % nightStars.size
            nightStars[previous].components[TransformComponent::class.java]
                ?.setScaleVector(Vector3(0.72f + (previous % 4) * 0.11f))
            nightStars[current].components[TransformComponent::class.java]
                ?.setScaleVector(Vector3(1.42f))
            current = (current + 1) % nightStars.size
            delay(280)
        }
    }

    fun sync(state: ThreeOrbsUiState) {
        latestState = state
        if (!built || taskEntities.size != 3) return
        val safeX = state.data.groupX.takeIf { it.isFinite() } ?: 0f
        val safeY = state.data.groupY.takeIf { it.isFinite() } ?: 0f
        val safeZ = state.data.groupZ.takeIf { it.isFinite() } ?: 0f
        val persisted = Vector3(
            safeX / GROUP_POSITION_SCALE,
            safeY / GROUP_POSITION_SCALE,
            safeZ / GROUP_POSITION_SCALE,
        )
        group.components[TransformComponent::class.java]?.setPosition(DEFAULT_GROUP_POSITION + persisted)
        state.today.slots.forEachIndexed { index, slot ->
            val completed = slot.status == TaskStatus.COMPLETED
            val empty = slot.status == TaskStatus.EMPTY || slot.status == TaskStatus.ARCHIVED
            taskEntities[index].enabled = true
            taskLabels.getOrNull(index)?.enabled = true
            taskMaterials[index].apply {
                setBaseColor(
                    when {
                        completed -> orbColors[index] * 0.30f
                        empty -> orbColors[index] * 0.18f
                        else -> orbColors[index]
                    },
                )
                setEmissiveColor(
                    when {
                        completed -> orbColors[index] * 0.08f
                        empty -> orbColors[index] * 0.025f
                        else -> orbColors[index] * 0.72f
                    },
                )
                setOpacity(if (completed || empty) 0.46f else 1f)
            }
            taskGlows[index].enabled = !completed && !empty
        }
        constellation.enabled = state.today.completedCount == 3
        dateWall.getChildren().filter { it.getName().startsWith("ProgressDot_") }.forEachIndexed { index, dot ->
            dot.components[TransformComponent::class.java]?.setScaleVector(Vector3(if (index < state.today.completedCount) 1.55f else 1f))
        }
    }

    fun isTaskOrb(entity: Entity): Boolean = slotFor(entity) != null

    fun slotFor(entity: Entity): Int? {
        var current: Entity? = entity
        while (current != null) {
            slotByEntityId[current.id]?.let { return it }
            current = current.getParent()
        }
        return null
    }

    fun canComplete(slot: Int): Boolean = latestState?.today?.slots?.getOrNull(slot)?.status == TaskStatus.ACTIVE

    fun setPressed(entity: Entity, pressed: Boolean) {
        slotFor(entity)?.let { slot ->
            taskEntities[slot].components[TransformComponent::class.java]?.setScaleVector(Vector3(if (pressed) 1.06f else 1f))
        }
    }

    fun beginDrag() {
        dragStart = group.components[TransformComponent::class.java]?.position ?: Vector3.ZERO
    }

    fun dragGroup(x: Float, y: Float, z: Float) {
        val transform = group.components[TransformComponent::class.java] ?: return
        val delta = Vector3(x / DRAG_PIXELS_PER_METER, -y / DRAG_PIXELS_PER_METER, z / DRAG_PIXELS_PER_METER)
        val next = transform.position + delta
        transform.setPosition(Vector3(next.x.coerceIn(-0.43f, 0.22f), next.y.coerceIn(-0.27f, 0.28f), next.z.coerceIn(-0.20f, 0.32f)))
    }

    fun endDrag(): Triple<Float, Float, Float>? {
        val position = group.components[TransformComponent::class.java]?.position ?: return null
        return Triple(
            (position.x - DEFAULT_GROUP_POSITION.x) * GROUP_POSITION_SCALE,
            (position.y - DEFAULT_GROUP_POSITION.y) * GROUP_POSITION_SCALE,
            (position.z - DEFAULT_GROUP_POSITION.z) * GROUP_POSITION_SCALE,
        )
    }

    suspend fun playCompletionTransfer() {
        val completedSlot = latestState?.today?.slots?.filter { it.status == TaskStatus.COMPLETED }
            ?.maxByOrNull { it.completedAt ?: 0L }?.index ?: 0
        val groupPosition = group.components[TransformComponent::class.java]?.position ?: Vector3.ZERO
        val start = groupPosition + taskPositions[completedSlot]
        val end = dateWall.components[TransformComponent::class.java]?.position ?: DEFAULT_DATE_WALL_POSITION
        transferParticles.forEach { it.enabled = true }
        repeat(18) { frame ->
            transferParticles.forEachIndexed { index, particle ->
                val t = ((frame - index * 0.55f) / 13f).coerceIn(0f, 1f)
                val base = Vector3.lerp(start, end, t)
                val arc = kotlin.math.sin(t * Math.PI).toFloat() * (0.06f + index % 3 * 0.012f)
                particle.components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(base.x, base.y + arc, base.z + (index % 2) * 0.012f))
                    setScaleVector(Vector3(0.70f + (1f - t) * 0.65f))
                }
            }
            delay(45)
        }
        transferParticles.forEach { it.enabled = false }
    }

    fun destroy() {
        root.destroy()
        resources.asReversed().forEach { runCatching { it.close() } }
        resources.clear()
    }

    private fun <T : AutoCloseable> T.rememberResource(): T = also { resources += it }
}

private const val ORB_RADIUS = 0.115f
private const val LONG_PINCH_MILLIS = 800L
private const val DRAG_PIXELS_PER_METER = 1150f
private const val GROUP_POSITION_SCALE = 410f
private val DEFAULT_GROUP_POSITION = Vector3(-0.10f, 0.02f, 0.08f)
private val DEFAULT_DATE_WALL_POSITION = Vector3(0.43f, 0.02f, -0.07f)
