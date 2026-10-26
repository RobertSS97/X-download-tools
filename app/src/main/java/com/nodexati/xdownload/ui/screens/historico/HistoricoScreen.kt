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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
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
import com.nodexati.xdownload.ui.components.HeaderCard
import com.nodexati.xdownload.ui.components.HeaderStat
import com.nodexati.xdownload.ui.components.LocalSheet
import com.nodexati.xdownload.ui.components.startOfToday
import com.nodexati.xdownload.ui.components.todayLabel
import com.nodexati.xdownload.ui.components.PrimaryButton
import com.nodexati.xdownload.ui.components.StatusBanner
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
    val sheet = LocalSheet.current

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            val today = startOfToday()
            val todayCount = items.count { it.createdAt >= today }
            HeaderCard(
                title = "Histórico",
                subtitle = if (items.isEmpty()) "Nada baixado ainda" else todayLabel(),
                icon = if (items.isEmpty()) Icons.Rounded.VideoLibrary else Icons.Rounded.DeleteOutline,
                backdrop = backdrop,
                start = HeaderStat("$todayCount", "hoje"),
                end = HeaderStat("${items.size}", if (items.size == 1) "vídeo" else "vídeos"),
                onIconClick = if (items.isEmpty()) null else {
                    {
                        sheet.show {
                            ConfirmClear(
                                count = items.size,
                                onConfirm = {
                                    store.clear()
                                    sheet.dismiss()
                                },
                                onCancel = sheet::dismiss,
                            )
                        }
                    }
                },
                modifier = Modifier.reveal(0).padding(bottom = 4.dp),
            )
        }

        if (items.isEmpty()) {
            item(key = "empty") { EmptyState(backdrop, onGoDownload, Modifier.reveal(1)) }
        } else {
            itemsIndexed(items, key = { _, e -> e.downloadId }) { index, entry ->
                HistoryCard(
                    entry = entry,
                    backdrop = backdrop,
                    modifier = Modifier.animateItem().reveal((index + 1).coerceAtMost(6)),
                    onClick = {
                        sheet.show {
                            EntrySheet(
                                entry = entry,
                                uri = downloader.uriFor(entry.downloadId),
                                onOpenPost = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.postUrl))) }
                                },
                                onRemove = {
                                    store.remove(entry)
                                    sheet.dismiss()
                                },
                            )
                        }
                    },
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
        PrimaryButton(text = "Baixar um vídeo", icon = Icons.Rounded.Download, onClick = onGoDownload)
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    backdrop: BackdropState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(24.dp))
            .pressable(onClick = onClick, pressedScale = 0.98f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(entry, Modifier.size(width = 96.dp, height = 64.dp), RoundedCornerShape(16.dp), small = true)
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
                entry.text.ifBlank { "Vídeo do X" },
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${entry.quality} · ${relativeDate(entry.createdAt)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}

@Composable
private fun Cover(entry: HistoryEntry, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape, small: Boolean) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        AsyncImage(model = entry.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (small) 26.dp else 52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(if (small) 4.dp else 12.dp),
        )
        entry.durationMs?.let {
            Text(
                formatDuration(it),
                fontSize = if (small) 10.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (small) 5.dp else 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = if (small) 5.dp else 9.dp, vertical = if (small) 1.dp else 4.dp),
            )
        }
    }
}

/** Sheet de um vídeo baixado: capa, autor, texto e as ações. */
@Composable
private fun EntrySheet(entry: HistoryEntry, uri: Uri?, onOpenPost: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    Cover(entry, Modifier.fillMaxWidth().aspectRatio(16f / 10f), RoundedCornerShape(24.dp), small = false)
    Spacer(Modifier.height(16.dp))
    Text(
        entry.authorName.ifBlank { "Post do X" },
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Text(
        listOfNotNull(entry.authorHandle.takeIf { it.isNotBlank() }?.let { "@$it" }, entry.quality, relativeDate(entry.createdAt)).joinToString(" · "),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    if (entry.text.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            entry.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    Spacer(Modifier.height(18.dp))
    if (uri == null) {
        StatusBanner(Icons.Rounded.ErrorOutline, "O arquivo não está mais na galeria.", FailedRed)
        Spacer(Modifier.height(12.dp))
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(text = "Abrir vídeo", icon = Icons.Rounded.PlayArrow, modifier = Modifier.weight(1f), onClick = { openVideo(context, uri) })
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .pressable(onClick = { shareVideo(context, uri) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Compartilhar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    SheetAction(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir post no X", MaterialTheme.colorScheme.onSurface, onOpenPost)
    SheetAction(Icons.Rounded.DeleteOutline, "Remover do histórico", FailedRed, onRemove)
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pressable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Confirmação para limpar o histórico (os arquivos continuam na galeria). */
@Composable
private fun ConfirmClear(count: Int, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .background(FailedRed.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = FailedRed, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Limpar histórico?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sai da lista ${if (count == 1) "1 vídeo" else "$count vídeos"}. Os arquivos continuam na galeria.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Limpar", icon = Icons.Rounded.DeleteOutline, color = FailedRed, contentColor = Color.White, onClick = onConfirm)
        Spacer(Modifier.height(8.dp))
        Text(
            "Cancelar",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .pressable(onClick = onCancel)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
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
