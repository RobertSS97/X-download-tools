package com.nodexati.xdownload.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Vidro: o fundo animado é gravado numa camada (backdropSource) e cada painel de vidro
 * redesenha o pedaço que fica atrás dele, desfocado (backdropBlur). No Android 11 e
 * anteriores não há blur em tempo real, então fica só o véu translúcido.
 */
class BackdropState internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var recorded by mutableStateOf(false)
}

@Composable
fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

/** Marca o conteúdo que aparece desfocado atrás dos painéis de vidro. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInRoot() }
    .drawWithContent {
        state.layer.record { this@drawWithContent.drawContent() }
        if (!state.recorded) state.recorded = true
        drawLayer(state.layer)
    }

/** Painel de vidro: base, fundo desfocado, véu e o recorte no formato pedido. */
fun Modifier.backdropBlur(
    state: BackdropState,
    shape: Shape,
    base: Color,
    blurRadius: Dp = 20.dp,
    tint: Color,
): Modifier = composed {
    var position by remember { mutableStateOf(Offset.Zero) }
    val blurLayer = rememberGraphicsLayer()
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    this
        .onGloballyPositioned { position = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            drawRect(base)
            if (canBlur && state.recorded) {
                val radius = blurRadius.toPx()
                blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                val dx = state.origin.x - position.x
                val dy = state.origin.y - position.y
                blurLayer.record {
                    translate(dx, dy) { drawLayer(state.layer) }
                }
                drawLayer(blurLayer)
                drawRect(tint)
            } else {
                // Sem blur o véu precisa ser mais denso para o texto continuar legível.
                drawRect(tint.copy(alpha = (tint.alpha + 0.3f).coerceAtMost(0.96f)))
            }
        }
}

/** Atalho com o visual padrão dos cartões de vidro: véu do tema e contorno fino. */
fun Modifier.glass(state: BackdropState, shape: Shape, veil: Float = 0.55f): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val tint = if (dark) Color(0xFF0A0F1A).copy(alpha = veil) else Color.White.copy(alpha = veil)
    val edge = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    this
        .backdropBlur(state, shape, base = scheme.background, blurRadius = 24.dp, tint = tint)
        .border(1.dp, edge, shape)
}
