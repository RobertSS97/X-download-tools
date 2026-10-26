package com.nodexati.xdownload.ui.screens.baixar

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.nodexati.xdownload.ui.components.PrimaryButton
import com.nodexati.xdownload.ui.components.glass
import com.nodexati.xdownload.ui.components.pressable
import com.nodexati.xdownload.ui.components.reveal
import com.nodexati.xdownload.ui.theme.DoneGreen
import com.nodexati.xdownload.ui.theme.FailedRed
import java.util.Locale

/**
 * Baixar: uma frase, um campo e o resultado. Sem cartões de instrução nem rótulos —
 * o vídeo encontrado ocupa a tela e as escolhas ficam numa linha só.
 */
@Composable
fun BaixarScreen(vm: BaixarViewModel, backdrop: BackdropState, bottomInset: Dp) {
    val context = LocalContext.current
    val settings = AppSettings.get(context)
    val clipboardLink = rememberClipboardLink(enabled = settings.readClipboard && vm.link.isBlank())
    val idle = vm.step is BaixarStep.Idle

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomInset + 24.dp),
    ) {
        Mark(Modifier.reveal(0))

        // Com o vídeo aberto, o título encolhe e o resultado sobe.
        val headTop by androidx.compose.animation.core.animateDpAsState(if (idle) 72.dp else 28.dp, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), label = "headTop")
        Spacer(Modifier.height(headTop))
        Text(
            text = "Cole o link.\nBaixe o vídeo.",
            fontSize = if (idle) 34.sp else 26.sp,
            lineHeight = if (idle) 38.sp else 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.reveal(1).padding(start = 2.dp).animateContentSize(),
        )
        Spacer(Modifier.height(20.dp))

        LinkBar(vm, backdrop, Modifier.reveal(2))

        AnimatedVisibility(
            visible = vm.error != null,
            enter = expandVertically(tween(220)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
        ) {
            ErrorLine(vm.error.orEmpty(), Modifier.padding(top = 12.dp, start = 6.dp))
        }

        AnimatedVisibility(
            visible = idle && vm.error == null,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(120)),
        ) {
            Column {
                if (clipboardLink != null) {
                    ClipboardHint(clipboardLink, onUse = { vm.onShared(clipboardLink) }, modifier = Modifier.padding(top = 14.dp))
                } else {
                    Text(
                        text = "Ou toque em Compartilhar no app do X e escolha XDownload.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.reveal(3).padding(start = 6.dp, top = 14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            targetState = vm.step,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(280, delayMillis = 80)) + slideInVertically(tween(360)) { it / 10 }) togetherWith fadeOut(tween(120))
            },
            label = "baixarStep",
        ) { current ->
            when (current) {
                is BaixarStep.Ready -> Result(vm, current.post, backdrop)
                BaixarStep.Loading -> Skeleton(backdrop)
                BaixarStep.Idle -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

/** Marca discreta no topo: só o quadrado azul e o nome. */
@Composable
private fun Mark(modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("XDownload", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

/**
 * Campo único em vidro: o link à esquerda e, à direita, "Colar" (vazio) ou o botão
 * redondo de buscar, que vira indicador enquanto procura.
 */
@Composable
private fun LinkBar(vm: BaixarViewModel, backdrop: BackdropState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    val ring by animateColorAsState(
        when {
            vm.error != null -> FailedRed.copy(alpha = 0.8f)
            focused -> primary.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        tween(200),
        label = "linkRing",
    )
    val loading = vm.step is BaixarStep.Loading
    fun submit() {
        focus.clearFocus()
        vm.fetch()
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .glass(backdrop, shape, veil = 0.7f)
            .ringBorder(ring, shape)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (vm.link.isEmpty()) {
                Text("x.com/…/status/…", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            BasicTextField(
                value = vm.link,
                onValueChange = vm::onLinkChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(primary),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onAny = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = when {
                loading -> 2
                vm.link.isEmpty() -> 0
                else -> 1
            },
            transitionSpec = { (fadeIn(tween(180)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.8f)) togetherWith fadeOut(tween(100)) },
            label = "linkAction",
        ) { action ->
            when (action) {
                0 -> Text(
                    "Colar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .pressable(onClick = { readClipboard(context)?.takeIf { it.isNotBlank() }?.let(vm::onShared) })
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
                1 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .pressable(onClick = { vm.onLinkChange("") }, pressedScale = 0.85f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primary)
                            .pressable(onClick = ::submit, pressedScale = 0.88f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Buscar vídeo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    }
                }
                else -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = primary)
                }
            }
        }
    }
}

/** Borda só quando há algo a sinalizar (foco ou erro). */
private fun Modifier.ringBorder(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    if (color.alpha == 0f) this else this.border(1.5.dp, color, shape)

@Composable
private fun ClipboardHint(link: String, onUse: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .pressable(onClick = onUse)
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Usar link copiado · ${link.removePrefix("https://").removePrefix("www.")}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Skeleton(backdrop: BackdropState) {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha by pulse.animateFloat(0.04f, 0.10f, infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "skeletonAlpha")
    val block = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).glass(backdrop, RoundedCornerShape(28.dp)).background(block))
        Box(Modifier.width(220.dp).height(12.dp).clip(CircleShape).background(block))
        Box(Modifier.width(160.dp).height(12.dp).clip(CircleShape).background(block))
    }
}

@Composable
private fun Result(vm: BaixarViewModel, post: PostInfo, backdrop: BackdropState) {
    val context = LocalContext.current
    val media = post.videos[vm.selectedVideo.coerceIn(0, post.videos.lastIndex)]
    val variant = media.variants[vm.selectedVariant.coerceIn(0, media.variants.lastIndex)]
    val state = vm.downloads[vm.key(vm.selectedVideo, variant)]

    Column(Modifier.animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))) {
        Poster(post, media, Modifier.reveal(0))

        if (post.text.isNotBlank()) {
            Text(
                text = post.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.reveal(1).padding(start = 6.dp, end = 6.dp, top = 14.dp),
            )
        }

        if (post.videos.size > 1) {
            Spacer(Modifier.height(18.dp))
            Pills(
                options = post.videos.mapIndexed { i, v -> if (v.kind == MediaKind.Gif) "GIF ${i + 1}" else "Vídeo ${i + 1}" },
                selected = vm.selectedVideo,
                onSelect = vm::selectVideo,
                backdrop = backdrop,
                modifier = Modifier.reveal(2),
            )
        }

        Spacer(Modifier.height(18.dp))
        Pills(
            options = media.variants.map { it.label },
            selected = vm.selectedVariant,
            onSelect = vm::selectVariant,
            backdrop = backdrop,
            modifier = Modifier.reveal(2),
        )
        Text(
            text = details(variant, media.durationMs),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.reveal(2).padding(start = 6.dp, top = 8.dp),
        )

        Spacer(Modifier.height(18.dp))
        Box(Modifier.reveal(3)) {
            DownloadAction(
                label = variant.label,
                size = estimateSize(variant.bitrate, media.durationMs),
                state = state,
                onDownload = vm::download,
                onOpen = { openVideo(context, it) },
                onShare = { shareVideo(context, it) },
            )
        }
    }
}

/** Capa do vídeo com o autor por cima, sobre um degradê escuro. */
@Composable
private fun Poster(post: PostInfo, media: VideoMedia, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        AsyncImage(model = media.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)))
        )
        Icon(
            if (media.kind == MediaKind.Gif) Icons.Rounded.Gif else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .padding(12.dp),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = post.authorAvatar,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(post.authorName.ifBlank { "Post do X" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (post.authorHandle.isNotBlank()) {
                    Text("@${post.authorHandle}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
                }
            }
            media.durationMs?.let {
                Text(
                    formatDuration(it),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Escolha em linha: pílulas de vidro, a escolhida em azul. */
@Composable
private fun Pills(options: List<String>, selected: Int, onSelect: (Int) -> Unit, backdrop: BackdropState, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val fg by animateColorAsState(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(200), label = "pillFg")
            val fill by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, tween(200), label = "pillBg")
            Box(
                Modifier
                    .height(42.dp)
                    .glass(backdrop, CircleShape, veil = 0.6f)
                    .background(fill, CircleShape)
                    .pressable(onClick = { onSelect(index) }, pressedScale = 0.92f)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = fg)
            }
        }
    }
}

/** Botão de download que vira barra de progresso e, no fim, abrir/compartilhar. */
@Composable
private fun DownloadAction(
    label: String,
    size: String?,
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
            null -> PrimaryButton(
                text = if (size != null) "Baixar $label · $size" else "Baixar $label",
                icon = Icons.Rounded.Download,
                onClick = onDownload,
            )
            is DownloadState.Running -> ProgressBar(current)
            is DownloadState.Done -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val uri = current.uri
                PrimaryButton(
                    text = "Salvo · Abrir",
                    icon = Icons.Rounded.Check,
                    enabled = uri != null,
                    color = DoneGreen,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = { uri?.let(onOpen) },
                )
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .pressable(onClick = { uri?.let(onShare) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "Compartilhar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
            }
            is DownloadState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ErrorLine(current.reason, Modifier.padding(start = 6.dp))
                PrimaryButton(text = "Tentar de novo", icon = Icons.Rounded.Refresh, onClick = onDownload)
            }
        }
    }
}

@Composable
private fun ProgressBar(state: DownloadState.Running) {
    val fraction = state.fraction
    val animated by animateFloatAsState(fraction ?: 0f, tween(350), label = "progress")
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(if (fraction == null) 0.06f else animated.coerceAtLeast(0.04f))
                .background(primary)
        )
        Text(
            if (fraction == null) "Preparando…" else "${(animated * 100).toInt()}%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (animated > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---- auxiliares ----

private fun details(variant: VideoVariant, durationMs: Long?): String = buildList {
    if (variant.width > 0 && variant.height > 0) add("${variant.width}×${variant.height}")
    if (variant.bitrate > 0) add(String.format(Locale("pt", "BR"), "%.1f Mbps", variant.bitrate / 1e6))
    durationMs?.let { add(formatDuration(it)) }
}.joinToString("  ·  ")

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
