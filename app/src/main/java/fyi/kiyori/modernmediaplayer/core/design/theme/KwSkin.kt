package fyi.kiyori.modernmediaplayer.core.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Единственный источник визуальных токенов KW Player.
 * Компоненты интерфейса должны получать цвета и формы из активного KwSkin.
 */
data class KwSkin(
    val id: String,
    val name: String,
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val cornerCard: androidx.compose.ui.unit.Dp,
    val cornerSmall: androidx.compose.ui.unit.Dp,
    val cornerButton: androidx.compose.ui.unit.Dp,
    val font: FontFamily,
    val isDark: Boolean,
    val navOnTop: Boolean,
    val bevel: Boolean,
    val pillButtons: Boolean = false
)

private fun color(value: Long) = Color(value)

fun kwSkin(id: String, light: Boolean = false): KwSkin {
    val base = when (id) {
        "winamp" -> KwSkin("winamp", "Winamp Modern",
            color(0xFF0A1019), color(0xFF16273A), color(0xFF243D58),
            color(0xFFE8F1FB), color(0xFFA3B5CA), color(0xFF0A121C),
            color(0xFF62C4FF), color(0xFF111111), color(0xFF62C4FF),
            3.dp, 2.dp, 3.dp, FontFamily.SansSerif, true, true, true)
        "spotify" -> KwSkin("spotify", "Spotify Style",
            color(0xFF121212), color(0xFF181818), color(0xFF282828),
            Color.White, color(0xFFB3B3B3), color(0xFF2A2A2A),
            color(0xFF1ED760), color(0xFF111111), color(0xFF1ED760),
            8.dp, 5.dp, 999.dp, FontFamily.Default, true, false, false, true)
        "oneui" -> if (light) KwSkin("oneui", "One UI Style",
            color(0xFFF2F2F7), Color.White, color(0xFFE6E6EC),
            color(0xFF111111), color(0xFF5C5C61), color(0xFFDEDEE3),
            color(0xFF1F6FE0), Color.White, color(0xFF1F6FE0),
            26.dp, 16.dp, 18.dp, FontFamily.Default, false, false, false)
        else KwSkin("oneui", "One UI Style",
            Color.Black, color(0xFF1C1C1E), color(0xFF2C2C2E),
            color(0xFFF5F5F7), color(0xFFA8A8AD), color(0xFF2C2C2E),
            color(0xFF3E91FF), Color.White, color(0xFF3E91FF),
            26.dp, 16.dp, 18.dp, FontFamily.Default, true, false, false)
        "amoled" -> KwSkin("amoled", "AMOLED Neon",
            Color.Black, color(0xFF0A0A0A), color(0xFF161616),
            Color.White, color(0xFFA6A6A6), color(0xFF1F3A36),
            color(0xFF5CF2CF), color(0xFF111111), color(0xFF5CF2CF),
            2.dp, 2.dp, 2.dp, FontFamily.Default, true, false, false)
        "material_you" -> KwSkin("material_you", "Material You",
            color(0xFF171A1C), color(0xFF222629), color(0xFF303638),
            color(0xFFF0F3F4), color(0xFFB8C2C5), color(0xFF3B4549),
            color(0xFF9BCBCE), color(0xFF111111), color(0xFF9BCBCE),
            20.dp, 12.dp, 999.dp, FontFamily.Default, true, false, false, true)
        "classic_vlc" -> KwSkin("classic_vlc", "Classic VLC",
            color(0xFFF2F2F2), Color.White, color(0xFFE4E4E4),
            color(0xFF1B1B1B), color(0xFF555555), color(0xFFCFCFCF),
            color(0xFFE87500), Color.White, color(0xFFE87500),
            6.dp, 4.dp, 4.dp, FontFamily.Default, false, false, false)
        else -> if (light) KwSkin("standard", "KW Modern",
            Color.White, color(0xFFF2F2F2), color(0xFFE4E4E4),
            color(0xFF0F0F0F), color(0xFF5C5C5C), color(0xFFDADADA),
            color(0xFFE87500), Color.White, color(0xFFE87500),
            14.dp, 8.dp, 10.dp, FontFamily.Default, false, false, false)
        else KwSkin("standard", "KW Modern",
            color(0xFF0F0F0F), color(0xFF1C1C1C), color(0xFF2C2C2C),
            color(0xFFF1F1F1), color(0xFFAAAAAA), color(0xFF303030),
            color(0xFFFF8F1F), color(0xFF111111), color(0xFFFF8F1F),
            14.dp, 8.dp, 10.dp, FontFamily.Default, true, false, false)
    }
    return base
}
