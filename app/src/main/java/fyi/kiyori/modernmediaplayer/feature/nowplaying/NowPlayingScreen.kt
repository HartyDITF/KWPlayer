package fyi.kiyori.modernmediaplayer.feature.nowplaying
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import fyi.kiyori.modernmediaplayer.core.playback.PlaybackEqualizer
import kotlinx.coroutines.delay
import java.util.Locale
@Composable fun NowPlayingScreen(controller: MediaController?, fallbackTitle: String, accent: Color, onClose: () -> Unit) {
 BackHandler(onBack = onClose)
 var position by remember { mutableLongStateOf(0L) }; var duration by remember { mutableLongStateOf(0L) }
 var preview by remember { mutableFloatStateOf(0f) }; var seeking by remember { mutableStateOf(false) }
 var refresh by remember { mutableIntStateOf(0) }
 LaunchedEffect(controller) { while (true) { position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L; duration = controller?.duration?.takeIf { it > 0 } ?: 0L; delay(750) } }
 val title = controller?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: fallbackTitle
 val artist = controller?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Локальный файл"
 val bands = listOf(60,230,910,3600,8000,14000).map { PlaybackEqualizer.nearestBand(it) }.filter { it >= 0 }.distinct()
 val range = PlaybackEqualizer.levelRange()
 Column(Modifier.fillMaxSize().background(Color(0xFF101318)).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick=onClose) { Text("Свернуть", color=accent) }; Spacer(Modifier.weight(1f)); Text("СЕЙЧАС ИГРАЕТ", color=Color(0xFF9BA4B5), fontSize=10.sp); Spacer(Modifier.weight(1f)); TextButton(onClick={ controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) { Text("SHUFFLE", color=accent) } }
  Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color(0xFF0B0D11))) { Column(Modifier.padding(18.dp)) { Text(formatTime(position), color=Color(0xFF5CF2CF), fontSize=44.sp, fontWeight=FontWeight.Bold); Text(title.uppercase(Locale.ROOT), color=Color(0xFF5CF2CF), maxLines=1, overflow=TextOverflow.Ellipsis); Text("STEREO · локальный файл", color=Color(0xFF9BA4B5), fontSize=10.sp) } }
  Box(Modifier.size(104.dp).background(Color(0xFF1A1E26), RoundedCornerShape(16.dp)), contentAlignment=Alignment.Center) { Text("KW", color=accent, fontSize=32.sp, fontWeight=FontWeight.Black) }
  Text(title, color=Color(0xFFE9ECF2), fontSize=20.sp, fontWeight=FontWeight.Bold, maxLines=2, overflow=TextOverflow.Ellipsis)
  Text(artist, color=Color(0xFF9BA4B5), fontSize=13.sp)
  Column(Modifier.fillMaxWidth()) {
   val max = duration.coerceAtLeast(1L).toFloat()
   Slider(value=if(seeking) preview.coerceIn(0f,max) else position.toFloat().coerceIn(0f,max), onValueChange={ preview=it; seeking=true }, onValueChangeFinished={ controller?.seekTo(preview.toLong()); seeking=false }, valueRange=0f..max)
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(formatTime(if(seeking) preview.toLong() else position), color=Color(0xFF9BA4B5), fontSize=11.sp); Text("-${formatTime((duration-position).coerceAtLeast(0))}", color=Color(0xFF9BA4B5), fontSize=11.sp) }
  }
  Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically) {
   TextButton(onClick={ controller?.seekToPreviousMediaItem() }) { Text("PREV", color=Color.LightGray) }
   Button(onClick={ if(controller?.isPlaying==true) controller.pause() else controller?.play() }, modifier=Modifier.size(72.dp), shape=RoundedCornerShape(50), colors=ButtonDefaults.buttonColors(containerColor=accent, contentColor=Color(0xFF101318))) { Text(if(controller?.isPlaying==true) "Ⅱ" else "▶", fontSize=24.sp) }
   TextButton(onClick={ controller?.seekToNextMediaItem() }) { Text("NEXT", color=Color.LightGray) }
  }
  Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color(0xFF1A1E26))) { Column(Modifier.padding(12.dp)) {
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("ЭКВАЛАЙЗЕР", color=Color.White, fontWeight=FontWeight.Bold); Text(if(bands.isEmpty()) "Недоступен" else "Устройство", color=Color(0xFF9BA4B5), fontSize=10.sp) }
   if (bands.isEmpty()) Text("Эквалайзер не поддерживается этим аудиовыходом.", color=Color(0xFF9BA4B5), fontSize=11.sp)
   else bands.take(6).forEach { band -> key(band, refresh) { Row(verticalAlignment=Alignment.CenterVertically) { Text("${PlaybackEqualizer.centerFrequency(band)/1000}k", color=Color(0xFF9BA4B5), modifier=Modifier.width(38.dp), fontSize=10.sp); Slider(value=PlaybackEqualizer.level(band).toFloat(), onValueChange={ PlaybackEqualizer.setLevel(band,it.toInt()); refresh++ }, valueRange=range.first.toFloat()..range.last.coerceAtLeast(range.first+1).toFloat(), modifier=Modifier.weight(1f)) } } }
  } }
 }
}
private fun formatTime(ms:Long):String { val s=(ms/1000).coerceAtLeast(0); return String.format(Locale.US,"%02d:%02d",s/60,s%60) }
