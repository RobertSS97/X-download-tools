package com.nodexati.xdownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nodexati.xdownload.settings.AppSettings
import com.nodexati.xdownload.ui.AppRoot
import com.nodexati.xdownload.ui.screens.baixar.BaixarViewModel
import com.nodexati.xdownload.ui.theme.XDownloadTheme

class MainActivity : ComponentActivity() {
    private val vm: BaixarViewModel by viewModels()

    /** Conta os links recebidos por compartilhamento, para a UI voltar à aba Baixar. */
    private val sharedTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        val settings = AppSettings.get(this)
        setContent {
            val dark = settings.isDark(isSystemInDarkTheme())
            LaunchedEffect(dark) {
                val transparent = Color.Transparent.toArgb()
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            XDownloadTheme(darkTheme = dark, largeText = settings.largeText) {
                AppRoot(vm, sharedTick.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** "Compartilhar" do app do X (texto com o link) ou link aberto direto no app. */
    private fun handleIntent(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        vm.onShared(text)
        sharedTick.intValue++
    }
}
