package fyi.kiyori.modernmediaplayer.feature.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    var isPlaying by remember(controller) { mutableStateOf(controller?.isPlaying == true) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = controller?.duration?.takeIf { it > 0L } ?: 0L
                position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                seeking = false
                position = 0L
                duration = controller?.duration?.takeIf { it > 0L } ?: 0L
            }

            override fun onEvents(player: Player, events: Player.Events) {
                duration = player.duration.takeIf { it > 0L } ?: 0L
                position = player.currentPosition.coerceAtLeast(0L)
            }
        }
        controller?.addListener(listener)
        isPlaying = controller?.isPlaying == true
        duration = controller?.duration?.takeIf { it > 0L } ?: 0L
        position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(controller, isPlaying) {
        val player = controller ?: return@LaunchedEffect
        while (isPlaying) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: 0L
            delay(500)
        }
    }

    val metadata = controller?.mediaMetadata
    val title = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: fallbackTitle
    val artist = metadata?.artist?.toString()?.takeIf { it.isNotBlank() && it != "KW Player" } ?: "Локальный аудиофайл"
    val album = metadata?.albumTitle?.toString()?.takeIf { it.isNotBlank() }
    val uri = controller?.currentMediaItem?.localConfiguration?.uri
    val artworkItem = uri?.let { LocalMediaItem(it, title, "audio/unknown", 0L, 0L) }
    val bands = listOf(60, 230, 910, 3600, 8000, 14000)
        .map { PlaybackEqualizer.nearestBand(it) }.filter { it >= 0 }.distinct()
    val range = PlaybackEqualizer.levelRange()
    val queueCount = controller?.mediaItemCount ?: 0
    val currentIndex = controller?.currentMediaItemIndex ?: -1

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(surface, background, background)))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹ Назад", color = accent) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("KW PLAYER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.8.sp)
                Text("СЕЙЧАС ИГРАЕТ", color = Color(0xFF9BA4B5), fontSize = 9.sp, letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
            ) {
                Text(if (controller?.shuffleModeEnabled == true) "⇄ ✓" else "⇄", color = accent, fontSize = 18.sp)
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (artworkItem != null) {
                MediaArtwork(
                    item = artworkItem,
                    modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f),
                    accent = accent,
                    surface = background
                )
            } else {
                Box(
                    Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                        .background(surface, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♫", color = accent, fontSize = 88.sp, fontWeight = FontWeight.Light)
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color(0xFFB1BAC8), fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            album?.let { Text(it, color = Color(0xFF8994A4), fontSize = 12.sp) }
        }

        Column(Modifier.fillMaxWidth()) {
            val max = duration.coerceAtLeast(1L).toFloat()
            Slider(
                value = if (seeking) preview.coerceIn(0f, max) else position.toFloat().coerceIn(0f, max),
                onValueChange = { preview = it; seeking = true },
                onValueChangeFinished = { controller?.seekTo(preview.toLong()); seeking = false },
                valueRange = 0f..max,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(if (seeking) preview.toLong() else position), color = Color(0xFF9BA4B5), fontSize = 11.sp)
                Text("-${formatTime((duration - position).coerceAtLeast(0L))}", color = Color(0xFF9BA4B5), fontSize = 11.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { controller?.seekToPreviousMediaItem() }, modifier = Modifier.size(52.dp)) {
                Text("⏮", color = Color.White, fontSize = 23.sp)
            }
            FilledIconButton(
                onClick = { if (controller?.isPlaying == true) controller.pause() else controller?.play() },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = background)
            ) {
                Text(if (controller?.isPlaying == true) "Ⅱ" else "▶", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { controller?.seekToNextMediaItem() }, modifier = Modifier.size(52.dp)) {
                Text("⏭", color = Color.White, fontSize = 23.sp)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = surface)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ОЧЕРЕДЬ ВОСПРОИЗВЕДЕНИЯ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(if (queueCount > 0) "$queueCount треков • текущий ${(currentIndex + 1).coerceAtLeast(0)}" else "Очередь пуста",
                            color = Color(0xFF9BA4B5), fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        controller?.let { player ->
                            player.repeatMode = when (player.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    }) {
                        Text(
                            when (controller?.repeatMode) {
                                Player.REPEAT_MODE_ALL -> "Повтор: всё"
                                Player.REPEAT_MODE_ONE -> "Повтор: 1"
                                else -> "Повтор: выкл."
                            },
                            color = accent,
                            fontSize = 10.sp
                        )
                    }
                }
                if (queueCount > 0) {
                    val start = (currentIndex - 1).coerceAtLeast(0)
                    val end = (currentIndex + 4).coerceAtMost(queueCount - 1)
                    for (index in start..end) {
                        val item = controller?.getMediaItemAt(index) ?: continue
                        val itemTitle = item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Трек ${index + 1}"
                        val itemArtist = item.mediaMetadata.artist?.toString().orEmpty()
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (index == currentIndex) accent.copy(alpha = 0.13f) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    controller?.let { player ->
                                        player.seekTo(index, 0L)
                                        player.play()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (index == currentIndex) "♫" else "${index + 1}", color = accent, modifier = Modifier.width(28.dp), fontSize = 13.sp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(itemTitle, color = if (index == currentIndex) accent else Color.White, fontSize = 12.sp,
                                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (itemArtist.isNotBlank()) Text(itemArtist, color = Color(0xFF9BA4B5), fontSize = 10.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (index == currentIndex) Text("ИГРАЕТ", color = accent, fontSize = 8.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = surface)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ЭКВАЛАЙЗЕР", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (bands.isEmpty()) "Недоступен" else "Аудиоэффекты", color = Color(0xFF9BA4B5), fontSize = 10.sp)
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
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
}
