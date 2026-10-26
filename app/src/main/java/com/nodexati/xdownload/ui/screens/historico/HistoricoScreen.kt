package com.nodexati.xdownload.ui.screens.historico

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nodexati.xdownload.data.Downloader
import com.nodexati.xdownload.data.HistoryEntry
import com.nodexati.xdownload.data.HistoryStore
import com.nodexati.xdownload.ui.components.BackdropState
import com.nodexati.xdownload.ui.components.RoundIconButton
import com.nodexati.xdownload.ui.components.ScreenHeader
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.reveal
import com.nodexati.xdownload.ui.screens.baixar.formatDuration
import com.nodexati.xdownload.ui.screens.baixar.openVideo
import com.nodexati.xdownload.ui.screens.baixar.shareVideo
import com.nodexati.xdownload.ui.theme.FailedRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoricoScreen(backdrop: BackdropState, bottomInset: Dp, onGoDownload: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HistoryStore.get(context) }
    val downloader = remember { Downloader(context) }
    val items = store.items
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 24.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(Modifier.reveal(0).padding(start = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader(
                    title = "Histórico",
                    subtitle = if (items.isEmpty()) "Nada baixado ainda" else "${items.size} ${if (items.size == 1) "vídeo baixado" else "vídeos baixados"}",
                    modifier = Modifier.weight(1f),
                )
                if (items.isNotEmpty()) {
                    Box {
                        RoundIconButton(Icons.Rounded.MoreHoriz, "Mais opções", onClick = { confirmClear = true })
                        DropdownMenu(expanded = confirmClear, onDismissRequest = { confirmClear = false }) {
                            DropdownMenuItem(
                                text = { Text("Limpar histórico") },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    confirmClear = false
                                    store.clear()
                                },
                            )
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item(key = "empty") { EmptyState(backdrop, onGoDownload, Modifier.reveal(1)) }
        } else {
            itemsIndexed(items, key = { _, e -> e.downloadId }) { index, entry ->
                HistoryCard(
                    entry = entry,
                    backdrop = backdrop,
                    modifier = Modifier.animateItem().reveal((index + 1).coerceAtMost(6)),
                    uri = { downloader.uriFor(entry.downloadId) },
                    onOpenPost = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.postUrl))) }
                    },
                    onRemove = { store.remove(entry) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(backdrop: BackdropState, onGoDownload: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(28.dp), veil = 0.6f)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Seus vídeos aparecem aqui", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tudo o que você baixar fica salvo na galeria e listado aqui para abrir ou compartilhar.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        com.nodexati.xdownload.ui.components.PrimaryButton(text = "Baixar um vídeo", icon = Icons.Rounded.Download, onClick = onGoDownload)
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    backdrop: BackdropState,
    uri: () -> Uri?,
    onOpenPost: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var missing by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(24.dp))
            .pressable(onClick = {
                val file = uri()
                if (file != null) openVideo(context, file) else missing = true
            }, pressedScale = 0.98f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 104.dp, height = 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            AsyncImage(model = entry.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(4.dp),
            )
            entry.durationMs?.let {
                Text(
                    formatDuration(it),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.authorName.ifBlank { "@${entry.authorHandle}" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (missing) "Arquivo não encontrado na galeria" else entry.text.ifBlank { "Vídeo do X" },
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = if (missing) FailedRed else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${entry.quality} · ${relativeDate(entry.createdAt)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .pressable(onClick = { menu = true }, pressedScale = 0.85f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "Opções", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Compartilhar") },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = {
                        menu = false
                        val file = uri()
                        if (file != null) shareVideo(context, file) else missing = true
                    },
                )
                DropdownMenuItem(
                    text = { Text("Abrir post no X") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = {
                        menu = false
                        onOpenPost()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remover do histórico") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menu = false
                        onRemove()
                    },
                )
            }
        }
    }
}

private fun relativeDate(millis: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    val time = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(millis))
    val sameYear = then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "hoje, $time"
        sameYear && dayDiff == 1 -> "ontem, $time"
        else -> SimpleDateFormat("d 'de' MMM", Locale("pt", "BR")).format(Date(millis))
    }
}
