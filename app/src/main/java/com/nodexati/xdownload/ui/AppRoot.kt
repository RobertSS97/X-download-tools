package com.nodexati.xdownload.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Download
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
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
import com.nodexati.xdownload.ui.components.LocalSheet
import com.nodexati.xdownload.ui.components.SheetHost
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.rememberSheetController
import androidx.compose.runtime.CompositionLocalProvider
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.rememberBackdropState
import com.nodexati.xdownload.ui.screens.ajustes.AjustesScreen
import com.nodexati.xdownload.ui.screens.baixar.BaixarScreen
import com.nodexati.xdownload.ui.screens.baixar.BaixarViewModel
import com.nodexati.xdownload.ui.screens.historico.HistoricoScreen

/** Abas com ícone contornado e a versão cheia para a aba ativa, como no RotaPRO. */
private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Baixar("Baixar", Icons.Rounded.Download, Icons.Rounded.Download),
    Historico("Histórico", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    Ajustes("Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Estrutura do app: fundo em movimento, telas por cima e a barra de navegação de vidro. */
@Composable
fun AppRoot(vm: BaixarViewModel, sharedTick: Int) {
    val backdrop = rememberBackdropState()
    val sheet = rememberSheetController()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Link compartilhado do X sempre leva para a aba Baixar.
    androidx.compose.runtime.LaunchedEffect(sharedTick) { if (sharedTick > 0) tab = Tab.Baixar.ordinal }
    val bottomInset = 100.dp

    CompositionLocalProvider(LocalSheet provides sheet) {
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
            onSelect = {
                if (sheet.isOpen) sheet.dismiss()
                tab = it
            },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        )

        // Sheets por cima de tudo, inclusive da barra de navegação.
        SheetHost(sheet, backdrop)
    }
    }
}

/**
 * Barra de navegação do RotaPRO: cápsula de vidro compacta e centralizada. A aba ativa
 * abre uma pílula azul com ícone cheio e o nome; ao trocar, a nova pílula se abre e
 * empurra as outras abas para o lado, enquanto a anterior se fecha só no ícone.
 */
@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit, backdrop: BackdropState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .glass(backdrop, CircleShape, veil = 0.6f)
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEachIndexed { index, item ->
                NavItem(item, active = index == selected, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
private fun NavItem(item: Tab, active: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val pill by animateColorAsState(if (active) primary.copy(alpha = 0.16f) else primary.copy(alpha = 0f), tween(220), label = "navPill")
    val tint by animateColorAsState(if (active) primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(220), label = "navTint")
    Row(
        Modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(pill)
            .pressable(onClick = onClick, pressedScale = 0.9f)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = if (active) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        AnimatedVisibility(
            visible = active,
            enter = expandHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220, delayMillis = 60)),
            exit = shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(100)),
        ) {
            Text(
                item.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
