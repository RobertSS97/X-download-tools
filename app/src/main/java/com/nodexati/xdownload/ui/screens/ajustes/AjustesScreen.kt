package com.nodexati.xdownload.ui.screens.ajustes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nodexati.xdownload.BuildConfig
import com.nodexati.xdownload.settings.AppSettings
import com.nodexati.xdownload.settings.DefaultQuality
import com.nodexati.xdownload.settings.ThemeMode
import com.nodexati.xdownload.ui.components.BackdropState
import com.nodexati.xdownload.ui.components.HeaderCard
import com.nodexati.xdownload.ui.components.HeaderStat
import com.nodexati.xdownload.ui.components.SectionLabel
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.reveal

@Composable
fun AjustesScreen(backdrop: BackdropState, bottomInset: Dp) {
    val settings = AppSettings.get(LocalContext.current)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HeaderCard(
            title = "Ajustes",
            subtitle = "XDownload ${BuildConfig.VERSION_NAME}",
            icon = Icons.Rounded.Tune,
            backdrop = backdrop,
            start = HeaderStat(settings.themeMode.label, "tema"),
            end = HeaderStat(settings.defaultQuality.label, "qualidade"),
            modifier = Modifier.reveal(0).padding(bottom = 6.dp),
        )

        SectionLabel("Aparência", Modifier.reveal(1))
        Group(backdrop, Modifier.reveal(1)) {
            RowItem(Icons.Rounded.DarkMode, "Tema", "Claro, escuro ou igual ao sistema") {}
            Segmented(
                options = ThemeMode.entries.map { it.label },
                selected = settings.themeMode.ordinal,
                onSelect = { settings.themeMode = ThemeMode.entries[it] },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
            Divider()
            RowItem(Icons.Rounded.TextFields, "Texto grande", "Aumenta as letras do app") {
                Switch(settings.largeText) { settings.largeText = it }
            }
        }

        Spacer(Modifier.height(6.dp))
        SectionLabel("Downloads", Modifier.reveal(2))
        Group(backdrop, Modifier.reveal(2)) {
            RowItem(Icons.Rounded.HighQuality, "Qualidade sugerida", "Já vem marcada ao abrir um vídeo") {}
            Segmented(
                options = DefaultQuality.entries.map { it.label },
                selected = settings.defaultQuality.ordinal,
                onSelect = { settings.defaultQuality = DefaultQuality.entries[it] },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
            Divider()
            RowItem(Icons.Rounded.ContentPaste, "Sugerir link copiado", "Mostra o link do X da área de transferência") {
                Switch(settings.readClipboard) { settings.readClipboard = it }
            }
            Divider()
            RowItem(Icons.Rounded.FolderOpen, "Pasta", "Filmes/XDownload") {}
        }

        Spacer(Modifier.height(6.dp))
        SectionLabel("Sobre", Modifier.reveal(3))
        Group(backdrop, Modifier.reveal(3)) {
            RowItem(Icons.Rounded.Info, "XDownload ${BuildConfig.VERSION_NAME}", "Baixe apenas conteúdo que você tem direito de usar.") {}
        }
    }
}

@Composable
private fun Group(backdrop: BackdropState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(start = 68.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

@Composable
private fun RowItem(icon: ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** Seletor segmentado com a "pílula" deslizando até a opção escolhida. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val width = maxWidth / options.size
            val offset by animateDpAsState(width * selected, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "segment")
            Box(
                Modifier
                    .offset(x = offset)
                    .width(width)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pressable(onClick = { onSelect(index) }, pressedScale = 0.94f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (index == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Interruptor próprio (trilho + bolinha em mola), sem o visual padrão do Material. */
@Composable
private fun Switch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val track by animateColorAsState(if (checked) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), tween(200), label = "track")
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "knob")
    Box(
        Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(track)
            .pressable(onClick = { onChange(!checked) }, pressedScale = 0.92f)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = knob)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
