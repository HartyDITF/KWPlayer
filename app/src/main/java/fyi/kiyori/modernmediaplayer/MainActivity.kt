package fyi.kiyori.modernmediaplayer

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.File

private data class PlayerColors(val background: Color, val surface: Color, val raised: Color, val accent: Color, val text: Color, val muted: Color)

private fun colorsForSkin(skin: String) = when (skin) {
    "winamp" -> PlayerColors(Color(0xFF090E18), Color(0xFF18283B), Color(0xFF243D58), Color(0xFF62C4FF), Color(0xFFF1F6FC), Color(0xFF9AAABD))
    "amoled" -> PlayerColors(Color.Black, Color(0xFF101010), Color(0xFF202020), Color(0xFF80CBC4), Color.White, Color(0xFFAAAAAA))
    "retro" -> PlayerColors(Color(0xFF17130D), Color(0xFF302719), Color(0xFF493A25), Color(0xFFE6B85C), Color(0xFFFFF3D8), Color(0xFFC4B28F))
    "neon" -> PlayerColors(Color(0xFF10091B), Color(0xFF21132F), Color(0xFF352047), Color(0xFFFF62C7), Color(0xFFFFF2FC), Color(0xFFC0A8C2))
    else -> PlayerColors(Color(0xFF101419), Color(0xFF1B222B), Color(0xFF27323E), Color(0xFF78B9F2), Color(0xFFF3F6FA), Color(0xFFAAB5C2))
}

class MainActivity : ComponentActivity() {
    private val controllerState = mutableStateOf<MediaController?>(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            val controller by controllerState
            KwPlayerScreen(controller)
        }
    }

    override fun onStart() {
        super.onStart()
        if (controllerFuture == null) {
            val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(this, token).buildAsync().also { future ->
                future.addListener({
                    try { controllerState.value = future.get() } catch (_: Exception) { controllerState.value = null }
                }, MoreExecutors.directExecutor())
            }
        }
    }

    override fun onDestroy() {
        controllerState.value = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun KwPlayerScreen(controller: MediaController?) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("kw_player_settings", android.content.Context.MODE_PRIVATE) }
    var skin by remember { mutableStateOf(preferences.getString("skin", "standard") ?: "standard") }
    var showSettings by remember { mutableStateOf(false) }
    val colors = colorsForSkin(skin)

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf("Проверка обновлений доступна вручную.") }
    var availableRelease by remember { mutableStateOf<KwRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateBusy by remember { mutableStateOf(false) }

    val openVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            selectedUri = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Видео"
            playbackError = null
        }
    }
    val openAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            selectedUri = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Аудио"
            playbackError = null
        }
    }

    LaunchedEffect(selectedUri, controller) {
        val uri = selectedUri
        val mediaController = controller
        if (uri != null && mediaController != null) {
            val metadata = MediaMetadata.Builder().setTitle(selectedName ?: "KWPlayer").setArtist("KWPlayer").build()
            mediaController.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build())
            mediaController.prepare()
            mediaController.play()
        }
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Не удалось воспроизвести файл. Возможно, кодек не поддерживается устройством."
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Оформление") },
            text = {
                Column {
                    listOf("standard" to "Стандартное", "winamp" to "Winamp Modern", "amoled" to "AMOLED Black", "retro" to "Classic Retro", "neon" to "Neon").forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = skin == key, onClick = {
                                skin = key
                                preferences.edit().putString("skin", key).apply()
                            })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Готово") } }
        )
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = colors.accent, background = colors.background, surface = colors.surface, onBackground = colors.text, onSurface = colors.text)) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("KWPlayer", color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("ЛОКАЛЬНЫЙ МЕДИАПЛЕЕР", color = colors.muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    TextButton(onClick = { showSettings = true }) { Text("Настройки", color = colors.accent) }
                }

                Column(modifier = Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(20.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (controller != null && selectedUri != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                            factory = { viewContext -> PlayerView(viewContext).apply {
                                player = controller
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            } },
                            update = { it.player = controller }
                        )
                        Text(selectedName ?: "Сейчас воспроизводится", color = colors.text, fontWeight = FontWeight.SemiBold)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("▶", color = colors.accent, fontSize = 42.sp)
                                Text("Выберите медиафайл", color = colors.text, fontWeight = FontWeight.SemiBold)
                                Text("Видео и музыка с вашего устройства", color = colors.muted, fontSize = 12.sp)
                            }
                        }
                    }
                    if (playbackError != null) Text(playbackError.orEmpty(), color = Color(0xFFFF8B8B), fontSize = 12.sp)
                    if (controller == null) Text("Подключаем аудиосессию…", color = colors.muted, fontSize = 12.sp)
                }

                Text("ОТКРЫТЬ ФАЙЛ", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Button(onClick = { openVideo.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)) {
                    Text("Открыть видео", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { openAudio.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.raised, contentColor = colors.text)) {
                    Text("Открыть аудио", fontWeight = FontWeight.SemiBold)
                }

                if (selectedUri != null && controller != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { if (isPlaying) controller.pause() else controller.play() }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.raised)) {
                            Text(if (isPlaying) "Пауза" else "Продолжить", color = colors.text)
                        }
                        Button(onClick = {
                            controller.stop()
                            controller.clearMediaItems()
                            selectedUri = null
                            selectedName = null
                            playbackError = null
                        }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.raised)) {
                            Text("Закрыть", color = colors.text)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(16.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ОБНОВЛЕНИЯ", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Установлена версия ${BuildConfig.VERSION_NAME}", color = colors.text, fontSize = 13.sp)
                    Text(updateMessage, color = colors.muted, fontSize = 12.sp)
                    availableRelease?.let { release ->
                        Text("Доступна версия ${release.versionName} • ${String.format("%.1f", release.apkSize / 1_048_576f)} МБ", color = colors.accent, fontSize = 12.sp)
                        Text(release.notes.take(400), color = colors.muted, fontSize = 11.sp)
                    }
                    Button(onClick = {
                        updateBusy = true
                        updateMessage = "Проверяем GitHub Releases…"
                        AppUpdater.check { release, error ->
                            updateBusy = false
                            availableRelease = release
                            downloadedApk = null
                            updateMessage = when {
                                error != null -> "Ошибка проверки: $error"
                                release != null -> "Доступно обновление. Можно скачать APK."
                                else -> "Установлена последняя версия."
                            }
                        }
                    }, enabled = !updateBusy, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.raised)) {
                        Text("Проверить обновления", color = colors.text)
                    }
                    if (availableRelease != null && downloadedApk == null) {
                        Button(onClick = {
                            val release = availableRelease ?: return@Button
                            updateBusy = true
                            updateMessage = "Скачиваем APK…"
                            AppUpdater.download(context, release) { file, error ->
                                updateBusy = false
                                downloadedApk = file
                                updateMessage = if (error == null) "APK загружен. Можно установить." else "Ошибка: $error"
                            }
                        }, enabled = !updateBusy, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                            Text("Скачать обновление", color = colors.background)
                        }
                    }
                    downloadedApk?.let { apk ->
                        Button(onClick = {
                            val activity = context as? Activity ?: return@Button
                            val launched = AppUpdater.install(activity, apk)
                            updateMessage = if (launched) "Подтвердите установку в Android." else "Разрешите установку APK и повторите."
                        }, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                            Text("Установить обновление", color = colors.background)
                        }
                    }
                }
                Text("Kibato Watch Player • ${BuildConfig.VERSION_NAME}", color = colors.muted, fontSize = 10.sp)
            }
        }
    }
}
