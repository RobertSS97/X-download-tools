package com.nodexati.xdownload.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale

/** Estado de um player: o ExoPlayer e o que a UI precisa observar. */
@Stable
class VideoPlayerState internal constructor(context: Context, private val source: String, loop: Boolean, muted: Boolean) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        volume = if (muted) 0f else 1f
    }
    var started by mutableStateOf(false); private set
    var playing by mutableStateOf(false); private set
    var buffering by mutableStateOf(false); private set
    var ended by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var muted by mutableStateOf(muted); private set
    var position by mutableLongStateOf(0L); internal set
    var duration by mutableLongStateOf(0L); private set
    /** Proporção do vídeo (largura/altura) quando já conhecida. */
    var videoAspect by mutableFloatStateOf(0f); private set

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoAspect = size.width * size.pixelWidthHeightRatio / size.height
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        })
    }

    /** Só baixa o vídeo quando a pessoa pede para tocar (economiza dados). */
    fun play() {
        if (!started) {
            player.setMediaItem(MediaItem.fromUri(source))
            player.prepare()
            started = true
        }
        failed = false
        if (ended) player.seekTo(0)
        player.play()
    }

    fun pause() = player.pause()

    fun toggle() = if (playing) pause() else play()

    fun seekTo(fraction: Float) {
        if (duration > 0) {
            val target = (duration * fraction.coerceIn(0f, 1f)).toLong()
            player.seekTo(target)
            position = target
        }
    }

    fun toggleMute() {
        muted = !muted
        player.volume = if (muted) 0f else 1f
    }

    internal fun release() = player.release()
}

@Composable
fun rememberVideoPlayerState(source: String, loop: Boolean = true, muted: Boolean = false): VideoPlayerState {
    val context = LocalContext.current
    val state = remember(source) { VideoPlayerState(context.applicationContext, source, loop, muted) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(state, lifecycle) {
        // Pausa ao sair do app; solta o player quando some da tela.
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) state.pause() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            state.release()
        }
    }
    LaunchedEffect(state, state.playing) {
        while (state.playing) {
            state.position = state.player.currentPosition
            delay(200)
        }
        state.position = state.player.currentPosition
    }
    return state
}

/**
 * Player embutido: capa com botão de play até começar, depois o vídeo com controles
 * que somem sozinhos (play/pausa, barra de progresso arrastável, som e tela cheia).
 * GIFs tocam em loop, sem som e sem controles, como no X.
 */
@Composable
fun VideoPlayer(
    source: String,
    thumbnail: String?,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    gif: Boolean = false,
    overlay: @Composable () -> Unit = {},
) {
    val state = rememberVideoPlayerState(source, loop = true, muted = gif)
    var fullscreen by remember { mutableStateOf(false) }
    LaunchedEffect(gif) { if (gif) state.play() }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(shape)
            .background(Color.Black),
    ) {
        if (!fullscreen) {
            PlayerSurface(state, Modifier.fillMaxSize())
        }
        // Capa até o primeiro quadro aparecer.
        AnimatedVisibility(visible = !state.started || (state.position == 0L && !state.playing && !state.ended), enter = fadeIn(), exit = fadeOut(tween(250))) {
            AsyncImage(model = thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (gif) {
            Badge("GIF", Modifier.align(Alignment.TopStart).padding(12.dp))
        } else {
            Controls(state, compact = true, onFullscreen = { fullscreen = true }, overlay = overlay)
        }
    }

    if (fullscreen) {
        FullscreenPlayer(state, onClose = { fullscreen = false })
    }
}

/** Superfície do vídeo (TextureView, para respeitar o recorte arredondado). */
@Composable
private fun PlayerSurface(state: VideoPlayerState, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val aspect = state.videoAspect
        val fit = if (aspect > 0f) {
            // Encaixa sem cortar: limita pela largura ou pela altura.
            if (maxWidth / maxHeight > aspect) Modifier.fillMaxHeight().aspectRatio(aspect, matchHeightConstraintsFirst = true)
            else Modifier.fillMaxWidth().aspectRatio(aspect)
        } else Modifier.fillMaxSize()
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { state.player.setVideoTextureView(it) } },
            onRelease = { view -> state.player.clearVideoTextureView(view) },
            modifier = fit,
        )
    }
}

@Composable
private fun Controls(state: VideoPlayerState, compact: Boolean, onFullscreen: (() -> Unit)?, onClose: (() -> Unit)? = null, overlay: @Composable () -> Unit = {}) {
    var visible by remember { mutableStateOf(true) }
    var interaction by remember { mutableStateOf(0) }
    // Some 2,5 s depois do último toque enquanto o vídeo toca.
    LaunchedEffect(state.playing, interaction) {
        if (state.playing) {
            delay(2500)
            visible = false
        } else {
            visible = true
        }
    }
    val show = visible || !state.playing

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(state) {
                detectTapGestures(
                    onTap = {
                        if (!state.started) state.play() else {
                            visible = !visible
                            interaction++
                        }
                    },
                    onDoubleTap = { state.toggle(); interaction++ },
                )
            },
    ) {
        AnimatedVisibility(show, enter = fadeIn(tween(180)), exit = fadeOut(tween(250))) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.35f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)))
                )
                if (!state.started) overlay()
                if (onClose != null) {
                    RoundButton(Icons.Rounded.Close, "Fechar", Modifier.align(Alignment.TopStart).padding(16.dp), size = 44) { onClose() }
                }
                if (!state.buffering || !state.started) {
                    val icon = when {
                        state.ended -> Icons.Rounded.Replay
                        state.playing -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
                    }
                    RoundButton(icon, if (state.playing) "Pausar" else "Tocar", Modifier.align(Alignment.Center), size = if (compact) 56 else 68) {
                        state.toggle()
                        interaction++
                    }
                }
                if (state.started) {
                    BottomBar(state, compact, onFullscreen, onInteract = { interaction++ }, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        if (state.buffering && state.started) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = Color.White, strokeWidth = 3.dp)
        }
        if (state.failed) {
            Text(
                "Não foi possível tocar o vídeo.",
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(state: VideoPlayerState, compact: Boolean, onFullscreen: (() -> Unit)?, onInteract: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 6.dp, bottom = if (compact) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${formatTime(state.position)} / ${formatTime(state.duration)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.width(12.dp))
        SeekBar(state, onInteract, Modifier.weight(1f))
        Spacer(Modifier.width(4.dp))
        IconTap(if (state.muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (state.muted) "Ativar som" else "Sem som") {
            state.toggleMute()
            onInteract()
        }
        if (onFullscreen != null) {
            IconTap(Icons.Rounded.Fullscreen, "Tela cheia") { onFullscreen() }
        }
    }
}

/** Barra de progresso arrastável; engrossa enquanto o dedo está nela. */
@Composable
private fun SeekBar(state: VideoPlayerState, onInteract: () -> Unit, modifier: Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val fraction = if (dragging) dragFraction else if (state.duration > 0) state.position.toFloat() / state.duration else 0f
    BoxWithConstraints(
        modifier
            .height(32.dp)
            .pointerInput(state) {
                detectTapGestures { offset ->
                    state.seekTo(offset.x / size.width)
                    onInteract()
                }
            }
            .pointerInput(state) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        state.seekTo(dragFraction)
                        dragging = false
                        onInteract()
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragFraction = (dragFraction + amount / size.width).coerceIn(0f, 1f)
                        onInteract()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val track = if (dragging) 6.dp else 4.dp
        Box(Modifier.fillMaxWidth().height(track).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(track).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        val thumb = if (dragging) 16.dp else 12.dp
        Box(
            Modifier
                .offset(x = (maxWidth - thumb) * fraction.coerceIn(0f, 1f))
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Tela cheia: mesmo player, fundo preto, gira para paisagem se o vídeo for deitado. */
@Composable
private fun FullscreenPlayer(state: VideoPlayerState, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        val activity = LocalContext.current.findActivity()
        DisposableEffect(Unit) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            val previous = activity?.requestedOrientation
            if (state.videoAspect > 1.05f) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                if (previous != null) activity.requestedOrientation = previous
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(state, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                Controls(state, compact = false, onFullscreen = null, onClose = onClose)
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, modifier: Modifier, size: Int, onClick: () -> Unit) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .pressable(onClick = onClick, pressedScale = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
private fun IconTap(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
