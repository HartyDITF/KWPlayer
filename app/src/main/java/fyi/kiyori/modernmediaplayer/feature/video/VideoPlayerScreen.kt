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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VideoPlayerScreen(controller: MediaController?, title: String, accent: Color, onClose: () -> Unit) {
    val activity = LocalContext.current as? Activity
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekPreview by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var fullscreen by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler {
        if (fullscreen) {
            fullscreen = false
            controlsVisible = true
        } else onClose()
    }

    DisposableEffect(activity) {
        val oldOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = oldOrientation
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(activity, fullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val insets = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(controller) {
        while (true) {
            position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            duration = controller?.duration?.takeIf { it > 0L } ?: 0L
            delay(500)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setKeepContentOnPlayerReset(true)
                    player = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = controller }
        )

        Box(Modifier.fillMaxSize().pointerInput(controller) {
            detectTapGestures(
                onTap = { controlsVisible = !controlsVisible },
                onDoubleTap = { offset ->
                    when {
                        offset.x < size.width / 3f -> controller?.seekBack()
                        offset.x > size.width * 2f / 3f -> controller?.seekForward()
                        else -> if (controller?.isPlaying == true) controller.pause() else controller?.play()
                    }
                    controlsVisible = true
                }
            )
        })

        if (controlsVisible) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xB8000000)).statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("‹", color = Color.White, fontSize = 28.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, maxLines = 1)
                        Text("KW PLAYER", color = Color.LightGray, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    }
                    TextButton(onClick = {
                        controller?.let { player ->
                            val params = player.trackSelectionParameters
                            player.trackSelectionParameters = params.buildUpon()
                                .setTrackTypeDisabled(
                                    C.TRACK_TYPE_TEXT,
                                    !params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                                ).build()
                        }
                    }) { Text("SUB", color = accent) }
                    TextButton(onClick = { fullscreen = !fullscreen; controlsVisible = true }) {
                        Text(if (fullscreen) "⤢" else "⛶", color = Color.White, fontSize = 20.sp)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(Color(0xB8000000)).navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatVideoTime(position), color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = if (seeking) seekPreview.coerceIn(0f, duration.coerceAtLeast(1L).toFloat())
                        else position.toFloat().coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                        onValueChange = { seekPreview = it; seeking = true },
                        onValueChangeFinished = {
                            controller?.seekTo(seekPreview.toLong())
                            seeking = false
                        },
                        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("-${formatVideoTime((duration - position).coerceAtLeast(0L))}", color = Color.White, fontSize = 11.sp)
                    TextButton(onClick = {
                        speed = if (speed >= 2f) 0.5f else speed + 0.25f
                        controller?.setPlaybackSpeed(speed)
                    }) { Text("${String.format(Locale.US, "%.2f", speed)}×", color = accent) }
                }
            }

            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { controller?.seekBack() },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x99000000), contentColor = Color.White)
                ) { Text("−10") }
                Button(
                    onClick = { if (controller?.isPlaying == true) controller.pause() else controller?.play() },
                    modifier = Modifier.size(68.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) { Text(if (controller?.isPlaying == true) "Ⅱ" else "▶", fontSize = 25.sp) }
                FilledTonalButton(
                    onClick = { controller?.seekForward() },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x99000000), contentColor = Color.White)
                ) { Text("+10") }
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
}
