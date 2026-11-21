package fyi.kiyori.modernmediaplayer.feature.video
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale
@Composable fun VideoPlayerScreen(controller:MediaController?, title:String, accent:Color, onClose:()->Unit) {
 val activity=LocalContext.current as? Activity; BackHandler(onBack=onClose)
 var pos by remember { mutableLongStateOf(0L) }; var dur by remember { mutableLongStateOf(0L) }; var preview by remember { mutableFloatStateOf(0f) }; var seeking by remember { mutableStateOf(false) }; var speed by remember { mutableFloatStateOf(1f) }
 DisposableEffect(activity) { val old=activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; onDispose { activity?.requestedOrientation=old } }
 LaunchedEffect(controller) { while(true) { pos=controller?.currentPosition?.coerceAtLeast(0L)?:0L; dur=controller?.duration?.takeIf{it>0}?:0L; delay(500) } }
 Box(Modifier.fillMaxSize().background(Color.Black)) {
  AndroidView(Modifier.fillMaxSize(), factory={ c -> PlayerView(c).apply { useController=false; resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT; player=controller } }, update={it.player=controller})
  Column(Modifier.fillMaxSize(), verticalArrangement=Arrangement.SpaceBetween) {
   Row(Modifier.fillMaxWidth().background(Color(0x99000000)).padding(10.dp), verticalAlignment=Alignment.CenterVertically) { TextButton(onClick=onClose){Text("‹",color=Color.White,fontSize=26.sp)}; Column(Modifier.weight(1f)){Text(title,color=Color.White,maxLines=1);Text("Сведения о кодеке зависят от декодера устройства",color=Color.LightGray,fontSize=10.sp)}; TextButton(onClick={controller?.let{p->val params=p.trackSelectionParameters; p.trackSelectionParameters=params.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,!params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)).build()}}){Text("SUB",color=accent)} }
   Row(Modifier.fillMaxWidth().background(Color(0x99000000)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) { Text(videoTime(pos),color=Color.White,fontSize=10.sp); Slider(Modifier.weight(1f),value=if(seeking)preview else pos.toFloat(),onValueChange={preview=it;seeking=true},onValueChangeFinished={controller?.seekTo(preview.toLong());seeking=false},valueRange=0f..dur.coerceAtLeast(1).toFloat()); Text("-${videoTime(dur-pos)}",color=Color.White,fontSize=10.sp); TextButton(onClick={speed=if(speed>=2f)1f else speed+0.25f;controller?.setPlaybackSpeed(speed)}){Text("${String.format(Locale.US,"%.2f",speed)}×",color=accent)} }
  }
  Row(Modifier.align(Alignment.Center),horizontalArrangement=Arrangement.spacedBy(22.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={controller?.seekBack()}){Text("−10",color=Color.White,fontSize=20.sp)};Button(onClick={if(controller?.isPlaying==true)controller.pause()else controller?.play()},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color.Black)){Text(if(controller?.isPlaying==true)"Ⅱ" else "▶",fontSize=26.sp)};TextButton(onClick={controller?.seekForward()}){Text("+10",color=Color.White,fontSize=20.sp)}}
 }
}
private fun videoTime(ms:Long)=String.format(Locale.US,"%02d:%02d",(ms/1000)/60,(ms/1000)%60)
