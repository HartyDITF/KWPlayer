package fyi.kiyori.modernmediaplayer.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * KW Player design tokens.
 *
 * Runtime accent selection is supplied by SettingsRepository; DefaultAccent is only
 * the fallback used before the saved setting has been loaded.
 */
object KwColors {
    val Background = Color(0xFF101318)
    val Panel = Color(0xFF1A1E26)
    val Border = Color(0xFF2A2F3A)
    val Navigation = Color(0xFF0D1014)

    val Text = Color(0xFFE9ECF2)
    val SecondaryText = Color(0xFF9BA4B5)

    val DefaultAccent = Color(0xFFFF8F1F)
    val TextOnAccent = Color(0xFF101318)

    val Lcd = Color(0xFF5CF2CF)
    val LcdBackground = Color(0xFF0B0D11)
}
