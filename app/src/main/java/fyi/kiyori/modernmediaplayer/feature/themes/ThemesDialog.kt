package fyi.kiyori.modernmediaplayer.feature.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemesDialog(
    selected: String,
    selectedAccent: Color,
    onSelect: (String) -> Unit,
    onAccent: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        Triple("standard", "KW Modern", Color(0xFF0F0F0F)) to "Главная как YouTube, управление как VLC",
        Triple("winamp", "Winamp Modern", Color(0xFF0A1019)) to "Стальные панели, LCD и эквалайзер",
        Triple("spotify", "Spotify Style", Color(0xFF121212)) to "Тёмные карточки и зелёный акцент",
        Triple("oneui", "One UI Style", Color(0xFF000000)) to "Крупные мягкие формы",
        Triple("amoled", "AMOLED Neon", Color.Black) to "Чистый чёрный и неон",
        Triple("material_you", "Material You", Color(0xFF20282B)) to "Цвета обоев и темы системы",
        Triple("classic_vlc", "Classic VLC", Color(0xFFF2F2F2)) to "Светлый, плотный, оранжевый"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оформление KW Player") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Семь вариантов оформления", color = Color(0xFF9BA4B5), fontSize = 12.sp)
                themes.forEach { (entry, description) ->
                    val (id, name, bg) = entry
                    val accent = when (id) {
                        "winamp" -> Color(0xFF62C4FF)
                        "spotify" -> Color(0xFF1ED760)
                        "oneui" -> Color(0xFF3E91FF)
                        "amoled" -> Color(0xFF5CF2CF)
                        "material_you" -> Color(0xFF9BCBCE)
                        "classic_vlc" -> Color(0xFFE87500)
                        else -> Color(0xFFFF8F1F)
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .border(if (selected == id) 2.dp else 1.dp,
                                if (selected == id) selectedAccent else Color(0xFF343B47),
                                RoundedCornerShape(12.dp))
                            .background(bg, RoundedCornerShape(12.dp))
                            .clickable { onSelect(id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(width = 30.dp, height = 5.dp).background(accent, RoundedCornerShape(4.dp)))
                                Box(Modifier.size(width = 19.dp, height = 5.dp).background(accent.copy(alpha = 0.72f), RoundedCornerShape(4.dp)))
                                Box(Modifier.size(width = 19.dp, height = 5.dp).background(accent.copy(alpha = 0.48f), RoundedCornerShape(4.dp)))
                            }
                            Text(name, color = if (id == "classic_vlc") Color(0xFF1B1B1B) else Color.White, fontSize = 14.sp)
                            Text(description, color = if (id == "classic_vlc") Color(0xFF555555) else Color(0xFFB3B3B3), fontSize = 10.sp)
                        }
                        Text(if (selected == id) "✓" else "○",
                            color = if (selected == id) selectedAccent else Color(0xFF9BA4B5), fontSize = 20.sp)
                    }
                }
                Text("Акцентный цвет", color = Color(0xFF9BA4B5), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Color(0xFFFF8F1F), Color(0xFFFF4E45), Color(0xFF5CF2CF),
                        Color(0xFF62C4FF), Color(0xFF1ED760), Color(0xFF8AB4F8), Color(0xFFFF62C7)
                    ).forEach { color ->
                        Box(
                            Modifier.weight(1f).height(34.dp)
                                .background(color, RoundedCornerShape(8.dp))
                                .border(if (selectedAccent == color) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                .clickable { onAccent(color) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}
