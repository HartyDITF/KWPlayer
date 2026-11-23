package fyi.kiyori.modernmediaplayer.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.luminance

@Composable
fun KwTheme(
    accent: Color = KwColors.DefaultAccent,
    light: Boolean = false,
    skinId: String = "standard",
    content: @Composable () -> Unit
) {
    val skin = kwSkin(skinId, light)
    val onAccent = if (accent.luminance() > 0.32f) Color(0xFF111111) else Color.White
    val scheme = if (!skin.isDark) {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = skin.accent,
            background = skin.bg,
            onBackground = skin.text,
            surface = skin.surface,
            onSurface = skin.text,
            surfaceVariant = skin.raised,
            onSurfaceVariant = skin.muted,
            outline = skin.line
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = skin.accent,
            onSecondary = skin.onAccent,
            background = skin.bg,
            onBackground = skin.text,
            surface = skin.surface,
            onSurface = skin.text,
            surfaceVariant = skin.raised,
            onSurfaceVariant = skin.muted,
            outline = skin.line
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
