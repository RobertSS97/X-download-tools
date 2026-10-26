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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
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

private enum class Tab(val label: String, val icon: ImageVector) {
    Baixar("Baixar", Icons.Rounded.Download),
    Historico("Histórico", Icons.Rounded.History),
    Ajustes("Ajustes", Icons.Rounded.Tune),
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

/** Barra flutuante de vidro com a pílula da aba ativa deslizando em mola. */
@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit, backdrop: BackdropState, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .glass(backdrop, RoundedCornerShape(28.dp), veil = 0.6f)
            .padding(6.dp),
    ) {
        val itemWidth = maxWidth / Tab.entries.size
        val offset by animateDpAsState(itemWidth * selected, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "navPill")
        Box(
            Modifier
                .offset(x = offset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        )
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEachIndexed { index, item ->
                val active = index == selected
                val color by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(200),
                    label = "navColor",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .pressable(onClick = { onSelect(index) }, pressedScale = 0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        item.label,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                    )
                }
            }
        }
    }
}
