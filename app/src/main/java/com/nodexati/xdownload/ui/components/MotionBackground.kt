package com.nodexati.xdownload.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fundo em movimento, no papel do "trânsito" do login do RotaPRO: manchas de luz azul
 * e quadros de vídeo flutuando devagar. Fica atrás dos painéis de vidro.
 */
@Composable
fun MotionBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "motion")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "motionTime",
    )
    val tiles = remember {
        val random = Random(7)
        List(14) {
            Tile(
                x = random.nextFloat(),
                y = random.nextFloat(),
                scale = 0.55f + random.nextFloat() * 0.8f,
                speed = 0.6f + random.nextFloat() * 0.9f,
                tilt = -14f + random.nextFloat() * 28f,
                phase = random.nextFloat(),
            )
        }
    }
    val primary = scheme.primary
    val tileFill = if (dark) Color.White.copy(alpha = 0.05f) else primary.copy(alpha = 0.07f)
    val tileEdge = if (dark) Color.White.copy(alpha = 0.10f) else primary.copy(alpha = 0.16f)
    val play = if (dark) primary.copy(alpha = 0.55f) else primary.copy(alpha = 0.45f)

    Canvas(modifier) {
        drawRect(scheme.background)
        val wave = (t * 2 * PI).toFloat()
        orb(Offset(size.width * (0.2f + 0.1f * sin(wave)), size.height * 0.18f), size.width * 0.75f, primary.copy(alpha = if (dark) 0.35f else 0.22f))
        orb(Offset(size.width * (0.9f - 0.08f * sin(wave + 1.3f)), size.height * (0.42f + 0.05f * sin(wave))), size.width * 0.65f, Color(0xFF8B5CF6).copy(alpha = if (dark) 0.22f else 0.12f))
        orb(Offset(size.width * 0.35f, size.height * (0.85f + 0.04f * sin(wave + 2f))), size.width * 0.7f, Color(0xFF22D3EE).copy(alpha = if (dark) 0.16f else 0.12f))

        val base = size.minDimension * 0.22f
        tiles.forEach { tile ->
            val progress = (t * tile.speed + tile.phase) % 1f
            val w = base * tile.scale
            val h = w * 0.62f
            val x = tile.x * size.width + sin((progress + tile.phase) * 2 * PI).toFloat() * w * 0.25f
            val y = (1.15f - progress * 1.4f) * size.height + tile.y * h
            rotate(tile.tilt + progress * 8f, pivot = Offset(x, y)) {
                videoTile(Offset(x - w / 2, y - h / 2), Size(w, h), tileFill, tileEdge, play)
            }
        }
    }
}

private data class Tile(val x: Float, val y: Float, val scale: Float, val speed: Float, val tilt: Float, val phase: Float)

private fun DrawScope.orb(center: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius), radius, center)
}

private fun DrawScope.videoTile(topLeft: Offset, size: Size, fill: Color, edge: Color, play: Color) {
    val corner = CornerRadius(size.height * 0.18f)
    drawRoundRect(fill, topLeft, size, corner)
    drawRoundRect(edge, topLeft, size, corner, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
    val c = Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2)
    val r = size.height * 0.2f
    val triangle = Path().apply {
        moveTo(c.x - r * 0.6f, c.y - r)
        lineTo(c.x + r, c.y)
        lineTo(c.x - r * 0.6f, c.y + r)
        close()
    }
    drawPath(triangle, play)
    // Barra de progresso do "player".
    val barY = topLeft.y + size.height * 0.84f
    drawRoundRect(edge, Offset(topLeft.x + size.width * 0.1f, barY), Size(size.width * 0.8f, 3f), CornerRadius(2f))
    drawRoundRect(play, Offset(topLeft.x + size.width * 0.1f, barY), Size(size.width * 0.35f, 3f), CornerRadius(2f))
}
