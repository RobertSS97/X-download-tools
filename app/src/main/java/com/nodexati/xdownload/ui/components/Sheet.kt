package com.nodexati.xdownload.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Bottom sheets de vidro do app, no molde do sheet de login do RotaPRO: entra em mola,
 * acompanha o dedo com resistência elástica e fecha ao arrastar para baixo.
 *
 * Um único host fica na raiz (acima da barra de navegação); as telas pedem um sheet
 * por [LocalSheet] e recebem o aviso quando ele fecha.
 */
@Stable
class SheetController {
    internal var content by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
    internal var onDismiss: () -> Unit = {}
    /** Muda a cada [show]; o host usa para animar a entrada de novo. */
    internal var generation by mutableIntStateOf(0)
    internal var closing by mutableStateOf(false)

    val isOpen: Boolean get() = content != null && !closing

    fun show(onDismiss: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
        val replacing = this.content != null && !closing
        this.onDismiss = onDismiss
        this.content = content
        closing = false
        if (!replacing) generation++
    }

    /** Fecha com animação; o onDismiss roda quando o sheet sai da tela. */
    fun dismiss() {
        if (content != null) closing = true
    }
}

val LocalSheet = staticCompositionLocalOf<SheetController> { error("SheetHost ausente") }

@Composable
fun rememberSheetController(): SheetController = remember { SheetController() }

@Composable
fun SheetHost(controller: SheetController, backdrop: BackdropState) {
    val content = controller.content ?: return
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val progress = remember(controller.generation) { Animatable(0f) }
    var drag by remember(controller.generation) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dismissPx = with(density) { 110.dp.toPx() }

    LaunchedEffect(controller.generation, controller.closing) {
        if (controller.closing) {
            focus.clearFocus()
            progress.animateTo(0f, tween(240))
            val done = controller.onDismiss
            controller.content = null
            controller.closing = false
            drag = 0f
            done()
        } else {
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
        }
    }
    BackHandler(enabled = !controller.closing) { controller.dismiss() }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(Modifier.fillMaxSize()) {
        // Véu escuro atrás do sheet; tocar fora fecha.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color.Black.copy(alpha = if (dark) 0.5f else 0.32f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { controller.dismiss() }
        )
        BoxWithConstraints(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            val maxSheet = maxHeight * 0.92f
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheet)
                    .graphicsLayer { translationY = (1f - progress.value) * size.height + drag }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            // Para baixo segue o dedo; para cima resiste cada vez mais.
                            drag = if (drag + delta < 0f) (drag + delta / (1f + abs(drag) / 40f)).coerceAtLeast(-60f) else drag + delta
                        },
                        onDragStopped = { velocity ->
                            if (drag > dismissPx || velocity > 1400f) {
                                controller.dismiss()
                            } else {
                                scope.launch {
                                    animate(drag, 0f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { value, _ -> drag = value }
                                }
                            }
                        },
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .backdropBlur(
                        state = backdrop,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        base = MaterialTheme.colorScheme.background,
                        blurRadius = 24.dp,
                        tint = if (dark) Color(0xFF0A0F1A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
            ) {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        .pressable(onClick = { controller.dismiss() })
                )
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    content = content,
                )
            }
        }
    }
}
