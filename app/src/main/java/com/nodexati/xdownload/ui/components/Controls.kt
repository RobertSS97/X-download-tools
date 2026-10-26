package com.nodexati.xdownload.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nodexati.xdownload.ui.theme.FailedRed

/** Botão principal com troca suave para o indicador de carregamento. */
@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector?,
    loading: Boolean = false,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (enabled || loading) 1f else 0.45f, tween(200), label = "buttonAlpha")
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .then(if (enabled && !loading) Modifier.pressable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "primaryButton",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = contentColor)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                }
            }
        }
    }
}

/** Botão secundário: fundo neutro translúcido, mesmo formato do principal. */
@Composable
fun SecondaryButton(text: String, icon: ImageVector?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Botão redondo só com ícone (voltar, colar, limpar). */
@Composable
fun RoundIconButton(icon: ImageVector, contentDescription: String, tint: Color = MaterialTheme.colorScheme.onSurface, size: Int = 38, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .pressable(onClick = onClick, pressedScale = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}

/** Aviso colorido (verde concluído, âmbar atenção, vermelho erro). */
@Composable
fun StatusBanner(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp),
    )
}

/** Campo no estilo do login do RotaPRO: ícone, rótulo que sobe ao focar e borda que acende. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Uri,
    imeAction: ImeAction = ImeAction.Go,
    onIme: () -> Unit = {},
    error: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val primary = MaterialTheme.colorScheme.primary
    val border by animateColorAsState(
        targetValue = when {
            error -> FailedRed
            focused -> primary.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        animationSpec = tween(200),
        label = "fieldBorder",
    )
    val raised = focused || value.isNotEmpty()
    val labelProgress = remember { Animatable(if (raised) 1f else 0f) }
    LaunchedEffect(raised) { labelProgress.animateTo(if (raised) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.5.dp, border, shape)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (focused) primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = (15 - 3 * labelProgress.value).sp,
                color = if (focused) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { translationY = -labelProgress.value * 11.dp.toPx() },
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(primary),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onAny = { onIme() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
        when {
            trailing != null -> trailing()
            value.isNotEmpty() -> Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .pressable(onClick = { onValueChange("") }, pressedScale = 0.85f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Linha de erro curta abaixo de um campo. */
@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = FailedRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, color = FailedRed, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** Cabeçalho das telas: título grande e subtítulo discreto. */
@Composable
fun ScreenHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
