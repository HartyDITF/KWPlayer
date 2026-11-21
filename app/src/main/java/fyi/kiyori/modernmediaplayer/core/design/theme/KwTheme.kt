package fyi.kiyori.modernmediaplayer.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KwTheme(
    accent: Color = KwColors.DefaultAccent,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = KwColors.TextOnAccent,
        secondary = KwColors.Lcd,
        onSecondary = KwColors.LcdBackground,
        background = KwColors.Background,
        onBackground = KwColors.Text,
        surface = KwColors.Panel,
        onSurface = KwColors.Text,
        surfaceVariant = KwColors.Border,
        onSurfaceVariant = KwColors.SecondaryText,
        outline = KwColors.Border
    )

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
