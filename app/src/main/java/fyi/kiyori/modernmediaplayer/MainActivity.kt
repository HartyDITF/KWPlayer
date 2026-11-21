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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import fyi.kiyori.modernmediaplayer.data.settings.KwSettingsRepository
import fyi.kiyori.modernmediaplayer.feature.nowplaying.NowPlayingScreen
import fyi.kiyori.modernmediaplayer.feature.video.VideoPlayerScreen
import fyi.kiyori.modernmediaplayer.feature.themes.ThemesDialog
import fyi.kiyori.modernmediaplayer.core.design.theme.KwTheme

private data class PlayerColors(
    val background: Color, val surface: Color, val raised: Color,
    val accent: Color, val text: Color, val muted: Color
)

private val tabs = listOf("Видео", "Аудио", "Обзор", "Плейлист")
private val tabIcons = mapOf("Видео" to "▣", "Аудио" to "♫", "Обзор" to "▤", "Плейлист" to "≡", "Ещё" to "•••")

private fun colorsForSkin(skin: String) = when (skin) {
    "winamp" -> PlayerColors(Color(0xFF090E18), Color(0xFF18283B), Color(0xFF243D58), Color(0xFF62C4FF), Color(0xFFF1F6FC), Color(0xFF9AAABD))
    "amoled" -> PlayerColors(Color.Black, Color(0xFF080808), Color(0xFF171717), Color(0xFF5CF2CF), Color.White, Color(0xFFAAAAAA))
    "light" -> PlayerColors(Color(0xFFF2F4F7), Color.White, Color(0xFFE5E8ED), Color(0xFFCF6812), Color(0xFF171A20), Color(0xFF5D6674))
    "retro" -> PlayerColors(Color(0xFF17130D), Color(0xFF302719), Color(0xFF493A25), Color(0xFFE6B85C), Color(0xFFFFF3D8), Color(0xFFC4B28F))
    "neon" -> PlayerColors(Color(0xFF10091B), Color(0xFF21132F), Color(0xFF352047), Color(0xFFFF62C7), Color(0xFFFFF2FC), Color(0xFFC0A8C2))
    else -> PlayerColors(Color(0xFF101419), Color(0xFF1B222B), Color(0xFF27323E), Color(0xFF78B9F2), Color(0xFFF3F6FA), Color(0xFFAAB5C2))
}

private fun skinShape(skin: String) = when (skin) {
    "amoled" -> RoundedCornerShape(0.dp)
    "retro" -> RoundedCornerShape(3.dp)
    "neon" -> RoundedCornerShape(22.dp)
    "winamp" -> RoundedCornerShape(8.dp)
    else -> RoundedCornerShape(14.dp)
}

private fun topTabs(skin: String) = skin == "winamp" || skin == "retro"

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
                    try { controllerState.value = future.get() }
                    catch (_: Exception) { controllerState.value = null }
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
    val prefs = remember { context.getSharedPreferences("kw_player_settings", android.content.Context.MODE_PRIVATE) }
    var skin by remember { mutableStateOf(prefs.getString("skin", "standard") ?: "standard") }
    var tab by remember { mutableStateOf("Видео") }
    var media by remember { mutableStateOf(MediaLibrary.loadItems(context)) }
    var folders by remember { mutableStateOf(MediaLibrary.folders(context)) }
    var query by remember { mutableStateOf("") }
    var sortNewest by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var scanBusy by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Добавь папку и запусти сканирование.") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf("Проверка обновлений доступна вручную.") }
    var availableRelease by remember { mutableStateOf<KwRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var showSkins by remember { mutableStateOf(false) }
    var activePlayerScreen by remember { mutableStateOf("library") }
    var isVideoItem by remember { mutableStateOf(false) }
    var accentColor by remember { mutableStateOf(Color(0xFFFF8F1F)) }
    val settingsRepository = remember { KwSettingsRepository(context) }
    val settingsScope = rememberCoroutineScope()
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    val colors = colorsForSkin(skin).copy(accent = accentColor)
    val shape = skinShape(skin)
    val favorites = MediaLibrary.favorites(context)
    val history = MediaLibrary.history(context)

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collect { settings ->
            skin = settings.skin
            accentColor = Color(settings.accentArgb)
        }
    }
    LaunchedEffect(context) {
        val stored = withContext(Dispatchers.IO) { MediaLibrary.loadFromRoom(context) }
        if (stored.isNotEmpty()) media = stored
        else withContext(Dispatchers.IO) { MediaLibrary.saveToRoom(context, media) }
    }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) {}
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Медиафайл"
            isVideoItem = context.contentResolver.getType(uri)?.startsWith("video/") == true
            activePlayerScreen = if (isVideoItem) "video" else "audio"
            playItem(uri, name, controller) { selectedUri = it; selectedName = name; playbackError = null }
            MediaLibrary.addHistory(context, uri.toString())
        }
    }
    val openFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try { context.contentResolver.takePersistableUriPermission(uri, flags) }
            catch (_: SecurityException) {
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: SecurityException) {}
            }
            MediaLibrary.addFolder(context, uri)
            folders = MediaLibrary.folders(context)
            scanMessage = "Папка добавлена. Нажми «Сканировать»."
        }
    }

    LaunchedEffect(selectedUri, controller) {
        val uri = selectedUri ?: return@LaunchedEffect
        val player = controller ?: return@LaunchedEffect
        val item = MediaItem.Builder().setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(selectedName ?: "KW Player").setArtist("KW Player").build())
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Не удалось воспроизвести файл. Проверь формат и доступ к нему."
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    if (showSkins) {
        ThemesDialog(
            selected = skin,
            selectedAccent = accentColor,
            onSelect = { key -> skin = key; settingsScope.launch { settingsRepository.setSkin(key) } },
            onAccent = { color -> accentColor = color; settingsScope.launch { settingsRepository.setAccent(color.value.toLong().toInt()) } },
            onDismiss = { showSkins = false }
        )
    }

    KwTheme(accent = colors.accent) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (tab == "Настройки") "Настройки" else if (skin == "retro") "KW PLAYER / ${tab.uppercase(Locale.ROOT)}" else "KW Player",
                            color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if (tab == "Настройки") "ПЕРСОНАЛИЗАЦИЯ И ПАРАМЕТРЫ" else "ЛОКАЛЬНЫЙ МЕДИАПЛЕЕР",
                        color = colors.muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    }
                    TextButton(onClick = { tab = if (tab == "Настройки") "Видео" else "Настройки" }) {
                        Text(if (tab == "Настройки") "←" else "⚙", color = colors.accent, fontSize = 22.sp)
                    }
                }

                if (topTabs(skin)) {
                    Row(Modifier.fillMaxWidth().background(colors.surface), horizontalArrangement = Arrangement.SpaceEvenly) {
                        tabs.forEach { name ->
                            TextButton(
                                onClick = { tab = name },
                                modifier = Modifier.weight(1f).background(
                                    if (tab == name) colors.raised else Color.Transparent,
                                    skinShape(skin)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tabIcons[name] ?: "", color = if (tab == name) colors.accent else colors.muted)
                                    Text(name.uppercase(Locale.ROOT), color = if (tab == name) colors.text else colors.muted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (skin == "amoled") 0.dp else 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (tab) {
                        "Видео" -> {
                            PageTitle("Видео", "Видеофайлы из выбранных папок", colors)
                            LibraryControls(query, { query = it }, sortNewest, { sortNewest = it }, favoritesOnly, { favoritesOnly = it }, colors)
                            MediaList(
                                items = filterMedia(media.filter { it.isVideo }, query, sortNewest, favoritesOnly, favorites),
                                favorites = favorites, colors = colors, skin = skin,
                                onPlay = { item -> startLibraryItem(item, controller, { selectedUri = it.uri; selectedName = it.name; isVideoItem = it.isVideo; activePlayerScreen = if (it.isVideo) "video" else "audio"; playbackError = null }, context) },
                                onFavorite = { item -> MediaLibrary.toggleFavorite(context, item.uri.toString()); media = media.toList() }
                            )
                            TextButton(onClick = { openFile.launch(arrayOf("video/*")) }) { Text("Открыть видеофайл", color = colors.accent) }
                        }
                        "Аудио" -> {
                            PageTitle("Аудио", "Музыкальная библиотека", colors)
                            LibraryControls(query, { query = it }, sortNewest, { sortNewest = it }, favoritesOnly, { favoritesOnly = it }, colors)
                            MediaList(
                                items = filterMedia(media.filter { it.isAudio }, query, sortNewest, favoritesOnly, favorites),
                                favorites = favorites, colors = colors, skin = skin,
                                onPlay = { item -> startLibraryItem(item, controller, { selectedUri = it.uri; selectedName = it.name; playbackError = null }, context) },
                                onFavorite = { item -> MediaLibrary.toggleFavorite(context, item.uri.toString()); media = media.toList() }
                            )
                            TextButton(onClick = { openFile.launch(arrayOf("audio/*")) }) { Text("Открыть аудиофайл", color = colors.accent) }
                        }
                        "Обзор" -> {
                            PageTitle("Обзор", "Папки, история и избранное", colors)
                            Panel(skin, colors) {
                                Text("ПАПКИ", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Доступ только к папкам, которые ты выбрал. Сканирование запускается вручную.", color = colors.muted, fontSize = 12.sp)
                                ActionButton(colors, "Добавить папку") { openFolder.launch(null) }
                                ActionButton(colors, if (scanBusy) "Сканирование…" else "Сканировать медиатеку", enabled = !scanBusy) {
                                    scanBusy = true
                                    scanMessage = "Идёт поиск аудио и видео…"
                                    Thread {
                                        val result = MediaLibrary.scan(context)
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            media = result
                                            scanBusy = false
                                            scanMessage = "Найдено файлов: ${result.size}"
                                        }
                                    }.start()
                                }
                                Text(scanMessage, color = colors.muted, fontSize = 11.sp)
                                folders.forEach { folder ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(folder.substringAfterLast('/').take(38), color = colors.text, modifier = Modifier.weight(1f), fontSize = 11.sp)
                                        TextButton(onClick = {
                                            MediaLibrary.removeFolder(context, folder)
                                            folders = MediaLibrary.folders(context)
                                        }) { Text("Убрать", color = colors.muted) }
                                    }
                                }
                            }
                            Panel(skin, colors) {
                                Text("ИЗБРАННОЕ", color = colors.accent, fontWeight = FontWeight.Bold)
                                MediaList(
                                    media.filter { favorites.contains(it.uri.toString()) }, favorites, colors, skin,
                                    onPlay = { item -> startLibraryItem(item, controller, { selectedUri = it.uri; selectedName = it.name; playbackError = null }, context) },
                                    onFavorite = { item -> MediaLibrary.toggleFavorite(context, item.uri.toString()); media = media.toList() }
                                )
                            }
                            Panel(skin, colors) {
                                Text("НЕДАВНО ОТКРЫТЫЕ", color = colors.accent, fontWeight = FontWeight.Bold)
                                history.take(12).forEach { uri ->
                                    val item = media.firstOrNull { it.uri.toString() == uri }
                                    if (item != null) {
                                        TextButton(onClick = { startLibraryItem(item, controller, { selectedUri = it.uri; selectedName = it.name; playbackError = null }, context) }) {
                                            Text(item.name, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                if (history.isEmpty()) Text("История пока пуста.", color = colors.muted, fontSize = 12.sp)
                            }
                        }
                        "Плейлист" -> {
                            PageTitle("Плейлист", "Текущая очередь Media3", colors)
                            Panel(skin, colors) {
                                if (controller != null && controller.mediaItemCount > 0) {
                                    repeat(controller.mediaItemCount) { index ->
                                        val title = controller.getMediaItemAt(index).mediaMetadata.title?.toString() ?: "Медиафайл ${index + 1}"
                                        TextButton(onClick = { controller.seekToDefaultPosition(index); controller.play() }) {
                                            Text("${index + 1}. $title", color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                } else {
                                    Text("Очередь пуста. Запусти файл из медиатеки.", color = colors.muted)
                                }
                            }
                        }
                        else -> {
                            PageTitle("Настройки", "Внешний вид, воспроизведение и обновления", colors)
                            Panel(skin, colors) {
                                Text("ТЕКУЩЕЕ ВОСПРОИЗВЕДЕНИЕ", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(selectedName ?: "Ничего не воспроизводится", color = colors.text, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { if (isPlaying) controller?.pause() else controller?.play() }) {
                                        Text(if (isPlaying) "Пауза" else "Продолжить", color = colors.accent)
                                    }
                                    TextButton(onClick = {
                                        controller?.stop(); controller?.clearMediaItems()
                                        selectedUri = null; selectedName = null
                                    }) { Text("Остановить", color = colors.muted) }
                                }
                                Text("Скорость: ${"%.2f".format(Locale.US, playbackSpeed)}×", color = colors.text)
                                Slider(value = playbackSpeed, onValueChange = { playbackSpeed = it }, valueRange = 0.5f..2f, steps = 5,
                                    onValueChangeFinished = { controller?.setPlaybackSpeed(playbackSpeed) })
                                if (controller != null) {
                                    Text("Доступно изменение скорости воспроизведения.", color = colors.muted, fontSize = 11.sp)
                                }
                            }
                            Panel(skin, colors) {
                                Text("ВНЕШНИЙ ВИД", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Выбери оформление. Каждый скин меняет палитру, форму панелей и расположение навигации.", color = colors.muted, fontSize = 12.sp)
                                ActionButton(colors, "Настроить оформление") { showSkins = true }
                            }
                            Panel(skin, colors) {
                                Text("ОБНОВЛЕНИЯ", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Установлена версия ${BuildConfig.VERSION_NAME}", color = colors.text)
                                Text(updateMessage, color = colors.muted, fontSize = 12.sp)
                                availableRelease?.let { release ->
                                    Text("Доступна версия ${release.versionName} • ${String.format(Locale.US, "%.1f", release.apkSize / 1_048_576f)} МБ", color = colors.accent)
                                    Text(release.notes.take(350), color = colors.muted, fontSize = 11.sp)
                                }
                                ActionButton(colors, if (updateBusy) "Проверяем…" else "Проверить обновления", enabled = !updateBusy) {
                                    updateBusy = true
                                    AppUpdater.check { release, error ->
                                        updateBusy = false
                                        availableRelease = release
                                        downloadedApk = null
                                        updateMessage = when {
                                            error != null -> "Ошибка: $error"
                                            release != null -> "Доступно обновление."
                                            else -> "Установлена последняя версия."
                                        }
                                    }
                                }
                                if (availableRelease != null && downloadedApk == null) {
                                    ActionButton(colors, "Скачать APK", enabled = !updateBusy) {
                                        updateBusy = true
                                        AppUpdater.download(context, availableRelease!!) { file, error ->
                                            updateBusy = false; downloadedApk = file
                                            updateMessage = error ?: "APK загружен."
                                        }
                                    }
                                }
                                downloadedApk?.let { apk ->
                                    ActionButton(colors, "Установить обновление") {
                                        val activity = context as? Activity ?: return@ActionButton
                                        val launched = AppUpdater.install(activity, apk)
                                        updateMessage = if (launched) "Подтверди установку в Android." else "Разреши установку APK."
                                    }
                                }
                            }
                            Panel(skin, colors) {
                                Text("KW Player — Kibato Watch Player", color = colors.text, fontWeight = FontWeight.SemiBold)
                                Text("Локальный плеер без рекламы, аккаунтов и отправки медиаданных.", color = colors.muted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (selectedUri != null && tab != "Видео" && tab != "Аудио" && activePlayerScreen == "library") {
                        MiniPlayback(colors, selectedName, isPlaying, onToggle = {
                            if (isPlaying) controller?.pause() else controller?.play()
                        }, onStop = {
                            controller?.stop(); controller?.clearMediaItems()
                            selectedUri = null; selectedName = null
                        })
                    }
                    playbackError?.let { Text(it, color = Color(0xFFFF8B8B), fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                }

                if (!topTabs(skin)) {
                    NavigationBar(containerColor = colors.surface) {
                        tabs.forEach { name ->
                            NavigationBarItem(
                                selected = tab == name,
                                onClick = { tab = name; query = ""; favoritesOnly = false },
                                icon = { Text(tabIcons[name] ?: "", color = if (tab == name) colors.accent else colors.muted) },
                                label = { Text(name, fontSize = 9.sp) }
                            )
                        }
                    }
                } else {
                    Text("KIBATO WATCH PLAYER • ${BuildConfig.VERSION_NAME}", color = colors.muted, fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding().padding(8.dp))
                }
            }
            if (activePlayerScreen == "audio") {
                Dialog(onDismissRequest = { activePlayerScreen = "library" }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = colors.background) {
                        NowPlayingScreen(controller, selectedName ?: "KW Player", colors.accent) { activePlayerScreen = "library" }
                    }
                }
            } else if (activePlayerScreen == "video") {
                Dialog(onDismissRequest = { activePlayerScreen = "library" }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    VideoPlayerScreen(controller, selectedName ?: "Видео", colors.accent) { activePlayerScreen = "library" }
                }
            }
        }
    }
}

private fun playItem(uri: Uri, name: String, controller: MediaController?, onSelected: (Uri) -> Unit) {
    onSelected(uri)
    controller?.let {
        it.setMediaItem(MediaItem.Builder().setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(name).setArtist("KW Player").build()).build())
        it.prepare()
        it.play()
    }
}

private fun startLibraryItem(
    item: LocalMediaItem,
    controller: MediaController?,
    onSelected: (LocalMediaItem) -> Unit,
    context: android.content.Context
) {
    onSelected(item)
    MediaLibrary.addHistory(context, item.uri.toString())
    controller?.let {
        it.setMediaItem(MediaItem.Builder().setUri(item.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(item.name).setArtist("KW Player").build()).build())
        it.prepare()
        it.play()
    }
}

private fun filterMedia(
    source: List<LocalMediaItem>, query: String, newest: Boolean,
    favoritesOnly: Boolean, favorites: Set<String>
): List<LocalMediaItem> {
    val filtered = source.filter {
        it.name.contains(query, ignoreCase = true) &&
            (!favoritesOnly || favorites.contains(it.uri.toString()))
    }
    return if (newest) filtered.sortedByDescending { it.modified } else filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
}

@androidx.compose.runtime.Composable
private fun PageTitle(title: String, subtitle: String, colors: PlayerColors) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = colors.muted, fontSize = 12.sp)
    }
}

@androidx.compose.runtime.Composable
private fun Panel(
    skin: String, colors: PlayerColors,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(colors.surface, skinShape(skin))
            .padding(if (skin == "retro") 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

@androidx.compose.runtime.Composable
private fun ActionButton(colors: PlayerColors, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(46.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@androidx.compose.runtime.Composable
private fun LibraryControls(
    query: String, onQuery: (String) -> Unit, newest: Boolean, onSort: (Boolean) -> Unit,
    favoritesOnly: Boolean, onFavorites: (Boolean) -> Unit, colors: PlayerColors
) {
    OutlinedTextField(
        value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
        label = { Text("Поиск по названию") }, singleLine = true
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onSort(!newest) }) {
            Text(if (newest) "Сначала новые" else "По имени А–Я", color = colors.accent)
        }
        TextButton(onClick = { onFavorites(!favoritesOnly) }) {
            Text(if (favoritesOnly) "Все файлы" else "Только избранное", color = colors.accent)
        }
    }
}

@androidx.compose.runtime.Composable
private fun MediaList(
    items: List<LocalMediaItem>, favorites: Set<String>, colors: PlayerColors, skin: String,
    onPlay: (LocalMediaItem) -> Unit, onFavorite: (LocalMediaItem) -> Unit
) {
    if (items.isEmpty()) {
        Text("Файлы не найдены. Добавь папку в разделе «Обзор» и запусти сканирование.", color = colors.muted, fontSize = 12.sp)
    } else {
        items.take(500).forEach { item ->
            Row(
                Modifier.fillMaxWidth().clickable { onPlay(item) }
                    .background(colors.surface, skinShape(skin)).padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).background(colors.raised, skinShape(skin)), contentAlignment = Alignment.Center) {
                    Text(if (item.isVideo) "▶" else "♫", color = colors.accent, fontSize = 17.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, color = colors.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (item.size > 0) formatBytes(item.size) else item.mimeType, color = colors.muted, fontSize = 10.sp)
                }
                TextButton(onClick = { onFavorite(item) }) {
                    Text(if (favorites.contains(item.uri.toString())) "★" else "☆", color = colors.accent, fontSize = 19.sp)
                }
            }
        }
        if (items.size > 500) Text("Показаны первые 500 файлов. Уточни поиск.", color = colors.muted, fontSize = 11.sp)
    }
}

private fun formatBytes(size: Long): String = when {
    size >= 1_073_741_824L -> String.format(Locale.US, "%.1f ГБ", size / 1_073_741_824.0)
    size >= 1_048_576L -> String.format(Locale.US, "%.1f МБ", size / 1_048_576.0)
    size >= 1024L -> String.format(Locale.US, "%.0f КБ", size / 1024.0)
    else -> "$size Б"
}

@androidx.compose.runtime.Composable
private fun MiniPlayback(
    colors: PlayerColors, title: String?, playing: Boolean,
    onToggle: () -> Unit, onStop: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("♫", color = colors.accent, fontSize = 22.sp)
        Text(title ?: "Воспроизведение", color = colors.text, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        TextButton(onClick = onToggle) { Text(if (playing) "Ⅱ" else "▶", color = colors.accent) }
        TextButton(onClick = onStop) { Text("×", color = colors.muted) }
    }
}