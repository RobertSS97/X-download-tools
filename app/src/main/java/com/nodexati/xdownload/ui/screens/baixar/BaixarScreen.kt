package com.nodexati.xdownload.ui.screens.baixar

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nodexati.xdownload.data.DownloadState
import com.nodexati.xdownload.data.MediaKind
import com.nodexati.xdownload.data.PostInfo
import com.nodexati.xdownload.data.VideoMedia
import com.nodexati.xdownload.data.VideoVariant
import com.nodexati.xdownload.data.XLink
import com.nodexati.xdownload.settings.AppSettings
import com.nodexati.xdownload.ui.components.BackdropState
import com.nodexati.xdownload.ui.components.ErrorLine
import com.nodexati.xdownload.ui.components.Field
import com.nodexati.xdownload.ui.components.PrimaryButton
import com.nodexati.xdownload.ui.components.SecondaryButton
import com.nodexati.xdownload.ui.components.SectionLabel
import com.nodexati.xdownload.ui.components.StatusBanner
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.reveal
import com.nodexati.xdownload.ui.theme.DoneGreen
import com.nodexati.xdownload.ui.theme.FailedRed
import java.util.Locale

@Composable
fun BaixarScreen(vm: BaixarViewModel, backdrop: BackdropState, bottomInset: Dp) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val settings = AppSettings.get(context)
    val clipboardLink = rememberClipboardLink(enabled = settings.readClipboard && vm.link.isBlank())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Brand(Modifier.padding(start = 6.dp).reveal(0))

        // Painel do link
        Column(
            Modifier
                .reveal(1)
                .fillMaxWidth()
                .glass(backdrop, RoundedCornerShape(28.dp), veil = 0.6f)
                .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                .padding(20.dp),
        ) {
            Text(text = "Baixar vídeo", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Cole o link do post do X ou compartilhe direto do app do X.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Field(
                value = vm.link,
                onValueChange = vm::onLinkChange,
                label = "Link do post",
                icon = Icons.Rounded.Link,
                onIme = { focus.clearFocus(); vm.fetch() },
                error = vm.error != null,
                trailing = if (vm.link.isEmpty()) {
                    {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .pressable(onClick = {
                                    val text = readClipboard(context)
                                    if (!text.isNullOrBlank()) vm.onShared(text)
                                })
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Colar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else null,
            )
            AnimatedVisibility(
                visible = vm.error != null,
                enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
            ) {
                ErrorLine(vm.error.orEmpty(), Modifier.padding(top = 10.dp))
            }
            AnimatedVisibility(visible = clipboardLink != null && vm.step is BaixarStep.Idle) {
                ClipboardSuggestion(clipboardLink.orEmpty(), onUse = { vm.onShared(clipboardLink.orEmpty()) })
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Buscar vídeo",
                icon = Icons.Rounded.Search,
                loading = vm.step is BaixarStep.Loading,
                enabled = vm.link.isNotBlank(),
                onClick = { focus.clearFocus(); vm.fetch() },
            )
        }

        AnimatedContent(
            targetState = vm.step,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 60)) + androidx.compose.animation.slideInVertically(tween(320)) { it / 8 }) togetherWith
                    fadeOut(tween(120))
            },
            label = "baixarStep",
        ) { current ->
            when (current) {
                is BaixarStep.Ready -> ResultCard(vm, current.post, backdrop)
                BaixarStep.Loading -> LoadingCard(backdrop)
                BaixarStep.Idle -> HowTo(backdrop)
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = "XDownload", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "Vídeos do X direto na sua galeria", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClipboardSuggestion(link: String, onUse: () -> Unit) {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .pressable(onClick = onUse)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Link copiado", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(link, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text("Usar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HowTo(backdrop: BackdropState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Como usar", Modifier.reveal(2))
        Column(
            Modifier
                .reveal(3)
                .fillMaxWidth()
                .glass(backdrop, RoundedCornerShape(24.dp))
                .padding(vertical = 6.dp),
        ) {
            Step(1, Icons.Rounded.Share, "No app do X, toque em Compartilhar no post", "Escolha XDownload na lista, ou copie o link.")
            Step(2, Icons.Rounded.TouchApp, "Escolha a qualidade", "Do 1080p ao arquivo mais leve, com o tamanho estimado.")
            Step(3, Icons.Rounded.Download, "Pronto, está na galeria", "Os vídeos ficam em Filmes/XDownload.")
        }
    }
}

@Composable
private fun Step(index: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("$index. $title", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(body, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingCard(backdrop: BackdropState) {
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val block = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(28.dp), veil = 0.6f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)).background(block))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(block))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(140.dp).height(12.dp).clip(CircleShape).background(block))
                Box(Modifier.width(90.dp).height(10.dp).clip(CircleShape).background(block))
            }
        }
        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(block))
    }
}

@Composable
private fun ResultCard(vm: BaixarViewModel, post: PostInfo, backdrop: BackdropState) {
    val context = LocalContext.current
    val media = post.videos[vm.selectedVideo.coerceIn(0, post.videos.lastIndex)]
    val variant = media.variants[vm.selectedVariant.coerceIn(0, media.variants.lastIndex)]
    val state = vm.downloads[vm.key(vm.selectedVideo, variant)]

    Column(
        Modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(28.dp), veil = 0.6f)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(16.dp),
    ) {
        Thumbnail(media, Modifier.reveal(0))
        Spacer(Modifier.height(14.dp))
        Author(post, Modifier.reveal(1).padding(horizontal = 4.dp))
        if (post.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.reveal(1).padding(horizontal = 4.dp),
            )
        }

        if (post.videos.size > 1) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Vídeos do post", Modifier.reveal(2))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.reveal(2).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.videos.forEachIndexed { index, item ->
                    Chip(
                        text = if (item.kind == MediaKind.Gif) "GIF ${index + 1}" else "Vídeo ${index + 1}",
                        selected = index == vm.selectedVideo,
                        onClick = { vm.selectVideo(index) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Qualidade", Modifier.reveal(2))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.reveal(3), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            media.variants.forEachIndexed { index, item ->
                QualityRow(
                    variant = item,
                    durationMs = media.durationMs,
                    best = index == 0 && media.variants.size > 1,
                    selected = index == vm.selectedVariant,
                    onClick = { vm.selectVariant(index) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.reveal(4)) {
            DownloadAction(
                label = variant.label,
                state = state,
                onDownload = vm::download,
                onOpen = { uri -> openVideo(context, uri) },
                onShare = { uri -> shareVideo(context, uri) },
            )
        }
    }
}

@Composable
private fun Thumbnail(media: VideoMedia, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        AsyncImage(
            model = media.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
        Box(
            Modifier
                .align(Alignment.Center)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (media.kind == MediaKind.Gif) Icons.Rounded.Gif else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        media.durationMs?.let {
            Text(
                text = formatDuration(it),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun Author(post: PostInfo, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = post.authorAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(post.authorName.ifBlank { "Post do X" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (post.authorHandle.isNotBlank()) {
                Text("@${post.authorHandle}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(if (selected) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), tween(200), label = "chipBg")
    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(200), label = "chipFg")
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

/** Opção de qualidade: resolução, tamanho estimado e marcador redondo à direita. */
@Composable
private fun QualityRow(variant: VideoVariant, durationMs: Long?, best: Boolean, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    val border by animateColorAsState(
        if (selected) primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        tween(200),
        label = "qualityBorder",
    )
    val fill by animateColorAsState(
        if (selected) primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tween(200),
        label = "qualityFill",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.5.dp, border, shape)
            .pressable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(variant.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (best) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "MELHOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = DoneGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DoneGreen.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            val details = buildList {
                if (variant.width > 0 && variant.height > 0) add("${variant.width}×${variant.height}")
                estimateSize(variant.bitrate, durationMs)?.let { add("≈ $it") }
            }
            if (details.isNotEmpty()) {
                Text(details.joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val ring by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f), label = "ring")
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size((12 * ring).dp).clip(CircleShape).background(primary))
        }
    }
}

/** Botão de download que vira barra de progresso e depois oferece abrir/compartilhar. */
@Composable
private fun DownloadAction(
    label: String,
    state: DownloadState?,
    onDownload: () -> Unit,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it?.let { s -> s::class } },
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
        label = "downloadAction",
    ) { current ->
        when (current) {
            null -> PrimaryButton(text = "Baixar em $label", icon = Icons.Rounded.Download, onClick = onDownload)
            is DownloadState.Running -> ProgressButton(current)
            is DownloadState.Done -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusBanner(Icons.Rounded.CheckCircle, "Salvo na galeria em Filmes/XDownload.", DoneGreen)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val uri = current.uri
                    PrimaryButton(
                        text = "Abrir",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        enabled = uri != null,
                        modifier = Modifier.weight(1f),
                        onClick = { uri?.let(onOpen) },
                    )
                    SecondaryButton(
                        text = "Compartilhar",
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f).height(56.dp),
                        onClick = { uri?.let(onShare) },
                    )
                }
            }
            is DownloadState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusBanner(Icons.Rounded.ErrorOutline, current.reason, FailedRed)
                PrimaryButton(text = "Tentar de novo", icon = Icons.Rounded.Refresh, onClick = onDownload)
            }
        }
    }
}

@Composable
private fun ProgressButton(state: DownloadState.Running) {
    val fraction = state.fraction
    val animated by animateFloatAsState(fraction ?: 0f, tween(350), label = "progress")
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(if (fraction == null) 0.08f else animated.coerceAtLeast(0.04f))
                .background(primary)
        )
        val text = if (fraction == null) "Iniciando download…" else "Baixando ${(animated * 100).toInt()}% · ${formatBytes(state.downloaded)}"
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---- auxiliares ----

/** Link do X na área de transferência, lido quando o app ganha foco. */
@Composable
private fun rememberClipboardLink(enabled: Boolean): String? {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var link by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focused, enabled) {
        link = if (enabled && focused) readClipboard(context)?.takeIf { XLink.looksLikeX(it) }?.let { XLink.firstUrl(it) ?: it } else null
    }
    return link
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

internal fun openVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "video/mp4")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
}

internal fun shareVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("video/mp4")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, "Compartilhar vídeo")) }
}

internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale("pt", "BR"), "%.1f GB", bytes / 1e9)
    bytes >= 1_000_000 -> String.format(Locale("pt", "BR"), "%.1f MB", bytes / 1e6)
    bytes >= 1_000 -> String.format(Locale("pt", "BR"), "%.0f KB", bytes / 1e3)
    else -> "$bytes B"
}

private fun estimateSize(bitrate: Long, durationMs: Long?): String? {
    if (bitrate <= 0 || durationMs == null) return null
    return formatBytes(bitrate / 8 * durationMs / 1000)
}

