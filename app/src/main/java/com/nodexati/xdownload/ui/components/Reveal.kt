package com.nodexati.xdownload.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/** Entrada escalonada: cada bloco sobe e aparece um pouco depois do anterior. */
fun Modifier.reveal(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L * index)
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow))
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 18f * density
    }
}

