package fyi.kiyori.modernmediaplayer

import android.app.Activity
import android.net.Uri
import java.io.File
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private val Midnight = Color(0xFF080C12)
private val Panel = Color(0xFF141D29)
private val PanelRaised = Color(0xFF1B2837)
private val Accent = Color(0xFF62C4FF)
private val TextPrimary = Color(0xFFF1F6FC)
private val Muted = Color(0xFF9AAABD)
private val Error = Color(0xFFFF8B8B)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Midnight,
                    surface = Panel,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                KwPlayerScreen(player)
            }
        }
    }

    override fun onStop() {
        player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun KwPlayerScreen(player: ExoPlayer) {
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf("Проверяйте наличие новых версий KW Player.") }
    var availableRelease by remember { mutableStateOf<KwRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateBusy by remember { mutableStateOf(false) }

    val openVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers do not offer persistable access.
            }
            selectedUri = uri
            playbackError = null
        }
    }

    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError =
                    "Не удалось воспроизвести файл. Возможно, формат или кодек не поддерживается устройством."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Midnight),
        color = Midnight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KW PLAYER",
                        color = TextPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        text = "KIBATO WATCH PLAYER",
                        color = Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(PanelRaised, RoundedCornerShape(10.dp))
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text("v0.1.0", color = Muted, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(20.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color(0xFF05080D), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("▶", color = Accent, fontSize = 46.sp)
                            Spacer(Modifier.height(9.dp))
                            Text(
                                "Ваше видео появится здесь",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Откройте локальный видеофайл",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            }
                        },
                        update = { view -> view.player = player }
                    )
                }

                if (playbackError != null) {
                    Text(
                        text = playbackError.orEmpty(),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = Error,
                        fontSize = 12.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ВОСПРОИЗВЕДЕНИЕ",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Button(
                    onClick = { openVideo.launch(arrayOf("video/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF06111C)
                    )
                ) {
                    Text("ОТКРЫТЬ ВИДЕО", fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }

                if (selectedUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPlaying) player.pause() else player.play()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PanelRaised)
                        ) {
                            Text(
                                if (isPlaying) "ПАУЗА" else "ВОСПРОИЗВЕСТИ",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                player.stop()
                                selectedUri = null
                                playbackError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PanelRaised)
                        ) {
                            Text("ЗАКРЫТЬ", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(16.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "ЛОКАЛЬНЫЙ ПЛЕЕР",
                    color = Accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    "Ваши файлы остаются на устройстве",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Без рекламы, аккаунтов и фонового сканирования",
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "KIBATO WATCH PLAYER  •  0.1.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.1.sp
            )
        }
    }
}
