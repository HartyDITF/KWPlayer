package fyi.kiyori.modernmediaplayer.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KwTheme(
    accent: Color = KwColors.DefaultAccent,
    light: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (light) {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = Color(0xFF287C70),
            background = Color(0xFFF2F4F7),
            onBackground = Color(0xFF171A20),
            surface = Color.White,
            onSurface = Color(0xFF171A20),
            surfaceVariant = Color(0xFFE5E8ED),
            onSurfaceVariant = Color(0xFF5D6674),
            outline = Color(0xFFD0D5DD)
        )
    } else {
        darkColorScheme(
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
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
