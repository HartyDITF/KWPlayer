package fyi.kiyori.modernmediaplayer.feature.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import fyi.kiyori.modernmediaplayer.LocalMediaItem
import fyi.kiyori.modernmediaplayer.MediaArtwork
import fyi.kiyori.modernmediaplayer.core.playback.PlaybackEqualizer
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun NowPlayingScreen(
    controller: MediaController?,
    fallbackTitle: String,
    accent: Color,
    background: Color = Color(0xFF101318),
    surface: Color = Color(0xFF1A1E26),
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var preview by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(controller) {
        while (true) {
            position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            duration = controller?.duration?.takeIf { it > 0L } ?: 0L
            delay(750)
        }
    }

    val metadata = controller?.mediaMetadata
    val title = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: fallbackTitle
    val artist = metadata?.artist?.toString()?.takeIf { it.isNotBlank() && it != "KW Player" } ?: "Локальный аудиофайл"
    val uri = controller?.currentMediaItem?.localConfiguration?.uri
    val artworkItem = uri?.let { LocalMediaItem(it, title, "audio/unknown", 0L, 0L) }
    val bands = listOf(60, 230, 910, 3600, 8000, 14000)
        .map { PlaybackEqualizer.nearestBand(it) }.filter { it >= 0 }.distinct()
    val range = PlaybackEqualizer.levelRange()

    Column(
        Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Brush.verticalGradient(listOf(surface, background, background)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Назад", color = accent) }
            Spacer(Modifier.weight(1f))
            Text("СЕЙЧАС ИГРАЕТ", color = Color(0xFF9BA4B5), fontSize = 10.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) {
                Text(if (controller?.shuffleModeEnabled == true) "SHUFFLE ✓" else "SHUFFLE", color = accent, fontSize = 10.sp)
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (artworkItem != null) {
                MediaArtwork(
                    item = artworkItem,
                    modifier = Modifier.size(230.dp),
                    accent = accent,
                    surface = background
                )
            } else {
                Box(
                    Modifier.size(230.dp).background(surface, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("KW", color = accent, fontSize = 54.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color(0xFF9BA4B5), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            metadata?.albumTitle?.toString()?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color(0xFF9BA4B5), fontSize = 11.sp)
            }
        }

        Column(Modifier.fillMaxWidth()) {
            val max = duration.coerceAtLeast(1L).toFloat()
            Slider(
                value = if (seeking) preview.coerceIn(0f, max) else position.toFloat().coerceIn(0f, max),
                onValueChange = { preview = it; seeking = true },
                onValueChangeFinished = { controller?.seekTo(preview.toLong()); seeking = false },
                valueRange = 0f..max
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(if (seeking) preview.toLong() else position), color = Color(0xFF9BA4B5), fontSize = 11.sp)
                Text("-${formatTime((duration - position).coerceAtLeast(0L))}", color = Color(0xFF9BA4B5), fontSize = 11.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { controller?.seekToPreviousMediaItem() }) { Text("PREV", color = Color.LightGray) }
            Button(
                onClick = { if (controller?.isPlaying == true) controller.pause() else controller?.play() },
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = background)
            ) { Text(if (controller?.isPlaying == true) "Ⅱ" else "▶", fontSize = 25.sp) }
            TextButton(onClick = { controller?.seekToNextMediaItem() }) { Text("NEXT", color = Color.LightGray) }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = surface)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ЭКВАЛАЙЗЕР", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (bands.isEmpty()) "Недоступен" else "Аудиовыход", color = Color(0xFF9BA4B5), fontSize = 10.sp)
                }
                if (bands.isEmpty()) {
                    Text("Эквалайзер не поддерживается этим аудиовыходом.", color = Color(0xFF9BA4B5), fontSize = 11.sp)
                } else {
                    bands.take(6).forEach { band ->
                        key(band, refresh) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${PlaybackEqualizer.centerFrequency(band) / 1000}k",
                                    color = Color(0xFF9BA4B5), modifier = Modifier.width(38.dp), fontSize = 10.sp)
                                Slider(
                                    value = PlaybackEqualizer.level(band).toFloat(),
                                    onValueChange = { PlaybackEqualizer.setLevel(band, it.toInt()); refresh++ },
                                    valueRange = range.first.toFloat()..range.last.coerceAtLeast(range.first + 1).toFloat(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            when (controller?.repeatMode) {
                Player.REPEAT_MODE_ALL -> "Повтор: весь плейлист"
                Player.REPEAT_MODE_ONE -> "Повтор: текущий трек"
                else -> "Повтор выключен"
            },
            color = Color(0xFF9BA4B5),
            fontSize = 10.sp
        )
        TextButton(onClick = {
            controller?.let { player ->
                player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }) { Text("Изменить режим повтора", color = accent) }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
}
