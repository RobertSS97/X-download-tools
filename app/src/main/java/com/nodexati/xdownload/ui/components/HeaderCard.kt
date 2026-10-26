package com.nodexati.xdownload.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Número em negrito seguido do texto discreto, como "0 de 0 concluídas". */
data class HeaderStat(val value: String, val label: String)

/**
 * Cartão de vidro do topo das telas, no molde do "Minha rota" do RotaPRO: título grande,
 * subtítulo, círculo à direita, uma linha de números e, se houver, a barra de progresso.
 */
@Composable
fun HeaderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: BackdropState,
    modifier: Modifier = Modifier,
    start: HeaderStat? = null,
    end: HeaderStat? = null,
    progress: Float? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedCornerShape(30.dp), veil = 0.62f)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .then(if (onIconClick != null) Modifier.pressable(onClick = onIconClick, pressedScale = 0.88f) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
        if (start != null || end != null) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (start != null) Stat(start)
                Spacer(Modifier.weight(1f))
                if (end != null) Stat(end)
            }
        }
        if (progress != null) {
            val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(450), label = "headerProgress")
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun Stat(stat: HeaderStat) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append(stat.value) }
            append(" ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(stat.label) }
        },
        fontSize = 15.sp,
    )
}

/** "Segunda, 26 de outubro". */
fun todayLabel(): String {
    val text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
        .replace("-feira", "")
    return text.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
}

/** Início do dia de hoje, em milissegundos. */
fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis
