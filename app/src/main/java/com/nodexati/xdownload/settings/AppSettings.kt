package com.nodexati.xdownload.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) { System("Sistema"), Light("Claro"), Dark("Escuro") }

/** Qualidade escolhida por padrão quando o vídeo tem mais de uma versão. */
enum class DefaultQuality(val label: String) { Best("Melhor"), Medium("Média"), Smallest("Menor arquivo") }

/** Preferências do app, observáveis pelo Compose (mesma ideia do AppSettings do RotaPRO). */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xdownload", Context.MODE_PRIVATE)

    private val theme = mutableStateOf(ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.System })
    private val large = mutableStateOf(prefs.getBoolean(KEY_LARGE_TEXT, false))
    private val quality = mutableStateOf(DefaultQuality.entries.getOrElse(prefs.getInt(KEY_QUALITY, 0)) { DefaultQuality.Best })
    private val clipboard = mutableStateOf(prefs.getBoolean(KEY_CLIPBOARD, true))

    var themeMode: ThemeMode
        get() = theme.value
        set(value) {
            theme.value = value
            prefs.edit().putInt(KEY_THEME, value.ordinal).apply()
        }

    var largeText: Boolean
        get() = large.value
        set(value) {
            large.value = value
            prefs.edit().putBoolean(KEY_LARGE_TEXT, value).apply()
        }

    var defaultQuality: DefaultQuality
        get() = quality.value
        set(value) {
            quality.value = value
            prefs.edit().putInt(KEY_QUALITY, value.ordinal).apply()
        }

    /** Ao abrir o app, sugere o link do X que estiver na área de transferência. */
    var readClipboard: Boolean
        get() = clipboard.value
        set(value) {
            clipboard.value = value
            prefs.edit().putBoolean(KEY_CLIPBOARD, value).apply()
        }

    fun isDark(systemDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> systemDark
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_LARGE_TEXT = "large_text"
        private const val KEY_QUALITY = "default_quality"
        private const val KEY_CLIPBOARD = "read_clipboard"

        @Volatile private var instance: AppSettings? = null
        fun get(context: Context): AppSettings =
            instance ?: synchronized(this) { instance ?: AppSettings(context.applicationContext).also { instance = it } }
    }
}
