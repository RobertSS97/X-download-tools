package com.nodexati.xdownload.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nodexati.xdownload.ui.components.BackdropState
import com.nodexati.xdownload.ui.components.MotionBackground
import com.nodexati.xdownload.ui.components.backdropSource
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.rememberBackdropState
import com.nodexati.xdownload.ui.screens.ajustes.AjustesScreen
import com.nodexati.xdownload.ui.screens.baixar.BaixarScreen
import com.nodexati.xdownload.ui.screens.baixar.BaixarViewModel
import com.nodexati.xdownload.ui.screens.historico.HistoricoScreen

/** Abas com ícone contornado e a versão cheia para a aba ativa, como no RotaPRO. */
private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Baixar("Baixar", Icons.Outlined.FileDownload, Icons.Filled.FileDownload),
    Historico("Histórico", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    Ajustes("Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Estrutura do app: fundo em movimento, telas por cima e a barra de navegação de vidro. */
@Composable
fun AppRoot(vm: BaixarViewModel, sharedTick: Int) {
    val backdrop = rememberBackdropState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Link compartilhado do X sempre leva para a aba Baixar.
    androidx.compose.runtime.LaunchedEffect(sharedTick) { if (sharedTick > 0) tab = Tab.Baixar.ordinal }
    val bottomInset = 96.dp

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MotionBackground(Modifier.fillMaxSize().backdropSource(backdrop))

        AnimatedContent(
            targetState = Tab.entries[tab],
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 60)) + scaleIn(tween(300), initialScale = 0.98f)) togetherWith fadeOut(tween(120))
            },
            label = "tab",
        ) { current ->
            when (current) {
                Tab.Baixar -> BaixarScreen(vm, backdrop, bottomInset)
                Tab.Historico -> HistoricoScreen(backdrop, bottomInset, onGoDownload = { tab = Tab.Baixar.ordinal })
                Tab.Ajustes -> AjustesScreen(backdrop, bottomInset)
            }
        }

        NavBar(
            selected = tab,
            onSelect = { tab = it },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )
    }
}

/**
 * Barra de vidro "empurra empurra": a lente azul segue o dedo quando você arrasta pela
 * barra, estica na direção do movimento e empurra os ícones vizinhos para o lado. Ao
 * soltar, encaixa na aba mais próxima com uma mola. Tocar numa aba faz a lente deslizar
 * até ela com o mesmo efeito.
 */
@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit, backdrop: BackdropState, modifier: Modifier = Modifier) {
    val count = Tab.entries.size
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Posição da lente em "abas" (0f = primeira, 1f = segunda…), contínua durante o arrasto.
    val position = remember { Animatable(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    // Velocidade recente da lente, usada para esticar e achatar.
    var velocity by remember { mutableFloatStateOf(0f) }
    var lastHover by remember { mutableIntStateOf(selected) }

    LaunchedEffect(selected) {
        if (!dragging && position.targetValue != selected.toFloat()) {
            position.animateTo(selected.toFloat(), spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) {
                velocity = this.velocity
            }
            velocity = 0f
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .glass(backdrop, RoundedCornerShape(34.dp), veil = 0.6f)
            .padding(6.dp),
    ) {
        val itemWidth = maxWidth / count
        val itemPx = with(LocalDensity.current) { itemWidth.toPx() }

        fun settle(target: Int) {
            scope.launch {
                position.animateTo(target.toFloat(), spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) {
                    velocity = this.velocity
                }
                velocity = 0f
            }
        }

        val dragModifier = Modifier.pointerInput(count, itemPx) {
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    dragging = true
                    // Começa a arrastar de onde o dedo tocou, como se pegasse a lente.
                    scope.launch { position.animateTo((offset.x / itemPx - 0.5f).coerceIn(0f, count - 1f), spring(stiffness = Spring.StiffnessHigh)) }
                },
                onDragEnd = {
                    dragging = false
                    val target = position.value.roundToInt().coerceIn(0, count - 1)
                    if (target != selected) onSelect(target)
                    settle(target)
                },
                onDragCancel = {
                    dragging = false
                    settle(selected)
                },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    val raw = position.value + amount / itemPx
                    // Resistência elástica ao passar das pontas.
                    val next = when {
                        raw < 0f -> raw / (1f + abs(raw) * 4f)
                        raw > count - 1f -> (count - 1f) + (raw - (count - 1f)) / (1f + abs(raw - (count - 1f)) * 4f)
                        else -> raw
                    }
                    velocity = amount / itemPx * 60f
                    scope.launch { position.snapTo(next) }
                    val hover = next.roundToInt().coerceIn(0, count - 1)
                    if (hover != lastHover) {
                        lastHover = hover
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
            )
        }

        // Lente: estica com a velocidade e achata um pouco na vertical, como gota.
        val speed = abs(velocity).coerceAtMost(12f) / 12f
        val stretch by animateFloatAsState(1f + speed * 0.35f + if (dragging) 0.08f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "lensStretch")
        val squash by animateFloatAsState(1f - speed * 0.14f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "lensSquash")
        Box(
            Modifier
                .graphicsLayer {
                    translationX = position.value * itemPx
                    scaleX = stretch
                    scaleY = if (dragging) squash * 1.04f else squash
                }
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (dragging) 0.20f else 0.14f))
        )

        Row(Modifier.fillMaxSize().then(dragModifier)) {
            Tab.entries.forEachIndexed { index, item ->
                // Distância da lente até esta aba, em abas: 0 = embaixo da lente.
                val distance = index - position.value
                val near = (1f - abs(distance)).coerceIn(0f, 1f)
                // Os vizinhos são empurrados para longe da lente enquanto ela passa.
                val push = if (abs(distance) in 0.15f..1.4f) sign(distance) * (1.4f - abs(distance)).coerceAtMost(0.7f) * 10f else 0f
                val active = index == selected
                val primary = MaterialTheme.colorScheme.primary
                val idle = MaterialTheme.colorScheme.onSurfaceVariant
                val color = lerp(idle, primary, near)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = push * density
                            val s = 1f + near * 0.12f
                            scaleX = s
                            scaleY = s
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .pressable(onClick = { onSelect(index) }, pressedScale = 0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(if (near > 0.5f) item.selectedIcon else item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                    )
                }
            }
        }
    }
}
