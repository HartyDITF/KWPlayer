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
        Triple("standard", "KW Modern", Color(0xFF101419)),
        Triple("winamp", "Winamp Modern", Color(0xFF090E18)),
        Triple("spotify", "Spotify Style", Color(0xFF101010)),
        Triple("oneui", "One UI", Color(0xFF111318)),
        Triple("amoled", "AMOLED Neon", Color.Black)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оформление KW Player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Пять вариантов оформления", color = Color(0xFF9BA4B5), fontSize = 12.sp)
                themes.forEach { (id, name, bg) ->
                    val accent = when (id) {
                        "winamp" -> Color(0xFF62C4FF)
                        "spotify" -> Color(0xFF1ED760)
                        "oneui" -> Color(0xFF8AB4F8)
                        "amoled" -> Color(0xFF5CF2CF)
                        else -> Color(0xFF78B9F2)
                    }
                    Row(Modifier.fillMaxWidth().border(if (selected == id) 2.dp else 1.dp, if (selected == id) selectedAccent else Color(0xFF343B47), RoundedCornerShape(16.dp)).background(bg, RoundedCornerShape(16.dp)).clickable { onSelect(id) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { index -> Box(Modifier.width(if (index == 0) 30.dp else 19.dp).height(5.dp).background(accent.copy(alpha = 1f - index * 0.18f), RoundedCornerShape(4.dp))) }
                            }
                            Text(name, color = Color.White, fontSize = 14.sp)
                            Text(when (id) {
                                "winamp" -> "Тёмные панели и синие акценты"
                                "spotify" -> "Глубокие поверхности и зелёный акцент"
                                "oneui" -> "Мягкие формы и спокойная палитра"
                                "amoled" -> "Абсолютно чёрный фон и неон"
                                else -> "Сбалансированный современный интерфейс"
                            }, color = Color(0xFF9BA4B5), fontSize = 10.sp)
                        }
                        Text(if (selected == id) "✓" else "○", color = selectedAccent, fontSize = 20.sp)
                    }
                }
                Text("Акцентный цвет", color = Color(0xFF9BA4B5), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Color(0xFFFF8F1F), Color(0xFF5CF2CF), Color(0xFF62C4FF), Color(0xFF1ED760), Color(0xFF8AB4F8), Color(0xFFFF62C7)).forEach { color ->
                        Box(Modifier.weight(1f).height(30.dp).background(color, RoundedCornerShape(9.dp)).clickable { onAccent(color) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}
