package fyi.kiyori.modernmediaplayer.feature.video

import android.app.Activity
import android.graphics.Bitmap
import android.view.PixelCopy
import android.graphics.drawable.ColorDrawable
import android.content.ContentValues
import android.provider.MediaStore
import android.view.SurfaceView
import android.os.Handler
import android.os.Looper
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import android.content.pm.ActivityInfo
import org.json.JSONArray
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import fyi.kiyori.modernmediaplayer.data.settings.KwSettingsRepository
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs

@OptIn(androidx.media3.common.util.UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    controller: MediaController?,
    title: String,
    accent: Color,
    backgroundPlaybackEnabled: Boolean,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current
    val settingsRepository = remember(context) { KwSettingsRepository(context) }
    val settingsScope = rememberCoroutineScope()
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember(controller) { mutableStateOf(controller?.isPlaying == true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var fullscreen by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsInteractionKey by remember { mutableIntStateOf(0) }
    val revealControls: () -> Unit = { controlsVisible = true; controlsInteractionKey++ }
    var controlsLocked by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var aspectMenu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var pendingSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var sideFieldMode by remember { mutableIntStateOf(0) }
    var customSideFieldColorArgb by remember { mutableIntStateOf(0xFF202020.toInt()) }
    var customSideFieldColorText by remember { mutableStateOf("202020") }
    var keepScreenOn by remember { mutableStateOf(true) }
    var seekIntervalSeconds by remember { mutableIntStateOf(10) }
    var aspectMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var repeatMode by remember { mutableIntStateOf(controller?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var shuffleEnabled by remember { mutableStateOf(controller?.shuffleModeEnabled ?: false) }
    var subtitleNotice by remember { mutableStateOf<String?>(null) }
    var bookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkUri by remember { mutableStateOf<String?>(null) }
    var bookmarks by remember { mutableStateOf<List<Long>>(emptyList()) }
    var abStartPosition by remember(controller) { mutableStateOf<Long?>(null) }
    var abEndPosition by remember(controller) { mutableStateOf<Long?>(null) }
    val bookmarkPreferences = remember(context) {
        context.getSharedPreferences("kw_video_bookmarks", android.content.Context.MODE_PRIVATE)
    }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    val audioManager = remember(context) { context.getSystemService(Activity.AUDIO_SERVICE) as AudioManager }
    val sideFieldColor = when (sideFieldMode) {
        1 -> Color(0xFF101419)
        2 -> accent.copy(alpha = 0.24f)
        3 -> Color(customSideFieldColorArgb)
        else -> Color.Black
    }
    var dragStartBrightness by remember { mutableFloatStateOf(0.5f) }
    var dragSeekPosition by remember { mutableLongStateOf(0L) }
    var dragAxis by remember { mutableIntStateOf(0) }

    val updateAspectMode: (Int) -> Unit = { mode ->
        aspectMode = mode
        settingsScope.launch { settingsRepository.setVideoAspectMode(mode) }
    }
    val updateSideFieldMode: (Int) -> Unit = { mode ->
        sideFieldMode = mode
        settingsScope.launch { settingsRepository.setVideoSideFieldMode(mode) }
    }
    val updateCustomSideFieldColor: (Int) -> Unit = { argb ->
        customSideFieldColorArgb = argb
        settingsScope.launch { settingsRepository.setVideoSideFieldCustomColor(argb) }
    }
    val updateKeepScreenOn: (Boolean) -> Unit = { enabled ->
        keepScreenOn = enabled
        settingsScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }
    val updateSeekInterval: (Int) -> Unit = { seconds ->
        seekIntervalSeconds = seconds
        settingsScope.launch { settingsRepository.setSeekIntervalSeconds(seconds) }
    }
    val seekByInterval: (Long) -> Unit = { direction ->
        controller?.let { player ->
            val target = (player.currentPosition + direction * seekIntervalSeconds * 1000L).coerceAtLeast(0L)
            player.seekTo(if (player.duration > 0L) target.coerceAtMost(player.duration) else target)
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collect { settings ->
            aspectMode = settings.videoAspectMode
            sideFieldMode = settings.videoSideFieldMode
            customSideFieldColorArgb = settings.videoSideFieldCustomColorArgb
            customSideFieldColorText = String.format(Locale.ROOT, "%08X", settings.videoSideFieldCustomColorArgb).takeLast(6)
            keepScreenOn = settings.keepScreenOn
            seekIntervalSeconds = settings.seekIntervalSeconds
        }
    }

    val snapshotDocumentPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bitmap = pendingSnapshot
        pendingSnapshot = null
        if (uri != null && bitmap != null) {
            settingsScope.launch {
                val saved = withContext(Dispatchers.IO) { writeSnapshotToUri(context, uri, bitmap) }
                bitmap.recycle()
                subtitleNotice = if (saved) "Кадр сохранён" else "Не удалось сохранить кадр"
            }
        } else {
            bitmap?.recycle()
        }
    }

    val captureVideoFrame: () -> Unit = {
        val surface = playerView?.videoSurfaceView as? SurfaceView
        when {
            controller?.isPlaying == true -> subtitleNotice = "Поставьте видео на паузу для снимка"
            surface == null || !surface.isAttachedToWindow || surface.width <= 0 || surface.height <= 0 ->
                subtitleNotice = "Видеоповерхность пока недоступна"
            else -> {
                val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface, bitmap, { result ->
                    if (result != PixelCopy.SUCCESS) {
                        bitmap.recycle()
                        subtitleNotice = "Не удалось получить кадр (${result})"
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        settingsScope.launch {
                            val saved = withContext(Dispatchers.IO) { saveSnapshotToGallery(context, bitmap) }
                            bitmap.recycle()
                            subtitleNotice = if (saved) "Кадр сохранён в Галерею" else "Не удалось сохранить кадр"
                        }
                    } else {
                        pendingSnapshot = bitmap
                        snapshotDocumentPicker.launch("KWPlayer_${System.currentTimeMillis()}.png")
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val suffix = uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mime = when (suffix) {
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                "vtt" -> MimeTypes.TEXT_VTT
                "ass", "ssa" -> MimeTypes.TEXT_SSA
                else -> context.contentResolver.getType(uri) ?: MimeTypes.APPLICATION_SUBRIP
            }
            val current = controller?.currentMediaItem
            if (controller != null && current?.localConfiguration?.uri != null) {
                val wasPlaying = controller.isPlaying
                val resumeAt = controller.currentPosition.coerceAtLeast(0L)
                val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(mime)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                val updated = current.buildUpon().setSubtitleConfigurations(listOf(subtitle)).build()
                controller.setMediaItem(updated, resumeAt)
                controller.prepare()
                if (wasPlaying) controller.play()
                subtitleNotice = "Внешние субтитры добавлены"
            }
        }
    }

    BackHandler {
        when {
            controlsLocked -> {
                controlsLocked = false
                controlsVisible = true
            }
            fullscreen -> {
                fullscreen = false
                controlsVisible = true
            }
            else -> onClose()
        }
    }

    val originalScreenBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness
            ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    DisposableEffect(activity) {
        val oldOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val wasKeepScreenOn = activity?.window?.let { (it.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0 } ?: false
        onDispose {
            activity?.requestedOrientation = oldOrientation
            activity?.window?.let { window ->
                if (wasKeepScreenOn) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.attributes = window.attributes.apply {
                    screenBrightness = originalScreenBrightness
                }
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(activity, isPlaying, keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn && isPlaying) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
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

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = controller?.duration?.takeIf { it > 0L } ?: 0L
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleEnabled = shuffleModeEnabled
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                abStartPosition = null
                abEndPosition = null
            }

            override fun onEvents(player: Player, events: Player.Events) {
                duration = player.duration.takeIf { it > 0L } ?: 0L
            }
        }
        controller?.addListener(listener)
        duration = controller?.duration?.takeIf { it > 0L } ?: 0L
        repeatMode = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
        shuffleEnabled = controller?.shuffleModeEnabled ?: false
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(controller, abStartPosition, abEndPosition, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val player = controller ?: return@LaunchedEffect
        val start = abStartPosition ?: return@LaunchedEffect
        val end = abEndPosition ?: return@LaunchedEffect
        if (end <= start) return@LaunchedEffect
        while (true) {
            if (player.currentPosition >= end) player.seekTo(start)
            delay(100)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, controlsInteractionKey) {
        if (controlsVisible && isPlaying) {
            delay(4500)
            controlsVisible = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val videoFrameWidth = minOf(maxWidth, maxHeight * (4f / 3f))
        val videoFrameHeight = videoFrameWidth * (3f / 4f)
        val playerResizeMode = if (aspectMode == 5) AspectRatioFrameLayout.RESIZE_MODE_FIT else aspectMode
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    playerView = this
                    useController = false
                    resizeMode = playerResizeMode
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(sideFieldColor.toArgb())
                    setBackgroundColor(sideFieldColor.toArgb())
                    player = controller
                }
            },
            modifier = if (aspectMode == 5) {
                Modifier.align(Alignment.Center).width(videoFrameWidth).height(videoFrameHeight)
            } else Modifier.fillMaxSize(),
            update = { view ->
                // Avoid rebinding Media3's PlayerView on every progress-timer recomposition.
                if (view.player !== controller) view.player = controller
                if (view.resizeMode != playerResizeMode) view.resizeMode = playerResizeMode
                val color = sideFieldColor.toArgb()
                if ((view.background as? ColorDrawable)?.color != color) {
                    view.setShutterBackgroundColor(color)
                    view.setBackgroundColor(color)
                }
            }
        )

        Box(
            Modifier.fillMaxSize()
                .pointerInput(controller, controlsLocked, seekIntervalSeconds) {
                detectTapGestures(
                    onTap = { if (!controlsLocked) { controlsVisible = !controlsVisible; controlsInteractionKey++ } },
                    onDoubleTap = { offset ->
                        if (!controlsLocked) {
                            when {
                                offset.x < size.width / 3f -> seekByInterval(-1L)
                                offset.x > size.width * 2f / 3f -> seekByInterval(1L)
                                else -> if (controller?.isPlaying == true) controller.pause() else controller?.play()
                            }
                            controlsVisible = true
                        }
                    }
                )
                }
                .pointerInput(activity, audioManager, controller, duration, controlsLocked) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!controlsLocked) {
                                dragSeekPosition = controller?.currentPosition?.coerceIn(0L, duration.coerceAtLeast(0L)) ?: 0L
                                dragStartBrightness = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                                dragAxis = 0
                                gestureHint = "Перемотка"
                                revealControls()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!controlsLocked) {
                                change.consume()
                                if (dragAxis == 0) dragAxis = if (abs(dragAmount.x) >= abs(dragAmount.y)) 1 else 2
                                if (dragAxis == 1) {
                                    if (duration > 0L) {
                                        val delta = (dragAmount.x / size.width.coerceAtLeast(1).toFloat() * duration).toLong()
                                        dragSeekPosition = (dragSeekPosition + delta).coerceIn(0L, duration)
                                        gestureHint = formatVideoTime(dragSeekPosition)
                                    }
                                } else if (change.position.x < size.width / 2f) {
                                    val attributes = activity?.window?.attributes ?: return@detectDragGestures
                                    val current = attributes.screenBrightness.takeIf { it >= 0f } ?: dragStartBrightness
                                    val fraction = (-dragAmount.y / size.height.coerceAtLeast(1).toFloat()).coerceIn(-0.08f, 0.08f)
                                    val next = (current + fraction).coerceIn(0.02f, 1f)
                                    attributes.screenBrightness = next
                                    activity.window.attributes = attributes
                                    gestureHint = "Яркость ${(next * 100).roundToInt()}%"
                                } else {
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val fraction = (-dragAmount.y / size.height.coerceAtLeast(1).toFloat()).coerceIn(-0.08f, 0.08f)
                                    val delta = (fraction * max * 2f).roundToInt()
                                    val next = (current + delta).coerceIn(0, max)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                    gestureHint = "Громкость ${(next * 100f / max).roundToInt()}%"
                                }
                            }
                        },
                        onDragEnd = {
                            if (!controlsLocked && dragAxis == 1 && duration > 0L) {
                                controller?.seekTo(dragSeekPosition)
                            }
                            gestureHint = null
                            dragAxis = 0
                        },
                        onDragCancel = { gestureHint = null; dragAxis = 0 }
                    )
                }
        )

        gestureHint?.let { hint ->
            Surface(modifier = Modifier.align(Alignment.Center), color = Color(0xCC171A20), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                Text(hint, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }

        if (controlsLocked) {
            TextButton(
                onClick = { controlsLocked = false; controlsVisible = true },
                modifier = Modifier.align(Alignment.Center)
                    .background(Color(0xCC171A20), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("🔓 Разблокировать", color = Color.White) }
        }

        if (controlsVisible && !controlsLocked) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.fillMaxWidth().background(Color(0xB8000000))) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose) { Text("‹", color = Color.White, fontSize = 28.sp) }
                        Column(Modifier.weight(1f)) {
                            Text(title, color = Color.White, maxLines = 1)
                            Text("KW PLAYER", color = Color.LightGray, fontSize = 9.sp, letterSpacing = 1.2.sp)
                        }
                        TextButton(onClick = { controlsLocked = true; controlsVisible = false }) {
                            Text("🔒", color = Color.White, fontSize = 16.sp)
                        }
                        TextButton(onClick = {
                            if (isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                runCatching {
                                    activity?.enterPictureInPictureMode(
                                        PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                                    )
                                }
                            }
                        }, enabled = isPlaying) { Text("PiP", color = if (isPlaying) accent else Color.Gray, fontSize = 13.sp) }
                        TextButton(onClick = { settingsOpen = true; controlsVisible = true }) {
                            Text("⚙", color = Color.White, fontSize = 20.sp)
                        }
                        TextButton(onClick = { fullscreen = !fullscreen; controlsVisible = true }) {
                            Text(if (fullscreen) "⤢" else "⛶", color = Color.White, fontSize = 20.sp)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                controller?.let {
                                    TrackSelectionDialogBuilder(context, "Аудиодорожка", it, C.TRACK_TYPE_AUDIO)
                                        .setShowDisableOption(false)
                                        .setAllowAdaptiveSelections(true)
                                        .build().show()
                                }
                            },
                            enabled = controller != null
                        ) { Text("Аудио", color = accent) }
                        TextButton(
                            onClick = {
                                controller?.let {
                                    TrackSelectionDialogBuilder(context, "Субтитры", it, C.TRACK_TYPE_TEXT)
                                        .setShowDisableOption(true)
                                        .setAllowAdaptiveSelections(false)
                                        .build().show()
                                }
                            },
                            enabled = controller != null
                        ) { Text("Дорожки", color = accent) }
                        TextButton(onClick = {
                            subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream"))
                        }) { Text("Файл субтитров", color = accent) }
                        TextButton(onClick = { captureVideoFrame() }, enabled = controller != null && !isPlaying) {
                            Text("Кадр", color = if (controller != null && !isPlaying) accent else Color.Gray)
                        }
                        TextButton(onClick = {
                            val uri = controller?.currentMediaItem?.localConfiguration?.uri?.toString()
                            if (uri != null) {
                                bookmarkUri = uri
                                bookmarks = loadVideoBookmarks(bookmarkPreferences, uri)
                                bookmarkDialog = true
                            }
                        }, enabled = controller?.currentMediaItem != null) { Text("Закладки", color = accent) }
                        Box {
                            TextButton(onClick = { aspectMenu = true }) { Text("Формат кадра", color = accent) }
                            DropdownMenu(expanded = aspectMenu, onDismissRequest = { aspectMenu = false }) {
                                listOf(
                                    "По размеру" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                    "Заполнить" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                    "Растянуть" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                                    "4:3" to 5
                                ).forEach { (label, mode) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { updateAspectMode(mode); aspectMenu = false; controlsVisible = true }
                                    )
                                }
                            }
                        }
                        TextButton(onClick = {
                            repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                else -> Player.REPEAT_MODE_OFF
                            }
                            controller?.repeatMode = repeatMode
                        }) {
                            Text(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> "Повтор: 1"
                                    Player.REPEAT_MODE_ALL -> "Повтор: всё"
                                    else -> "Повтор: выкл."
                                },
                                color = accent
                            )
                        }
                        TextButton(onClick = {
                            val position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
                            when {
                                abStartPosition == null -> {
                                    abStartPosition = position
                                    abEndPosition = null
                                    subtitleNotice = "Точка A: ${formatVideoTime(position)}"
                                }
                                abEndPosition == null -> {
                                    if (position <= (abStartPosition ?: 0L) + 500L) {
                                        subtitleNotice = "Точка B должна быть позже точки A"
                                    } else {
                                        abEndPosition = position
                                        subtitleNotice = "Повтор A–B включён"
                                    }
                                }
                                else -> {
                                    abStartPosition = null
                                    abEndPosition = null
                                    subtitleNotice = "Повтор A–B выключен"
                                }
                            }
                            controlsVisible = true
                        }, enabled = controller?.currentMediaItem != null) {
                            Text(
                                when {
                                    abStartPosition == null -> "A–B"
                                    abEndPosition == null -> "A: ${formatVideoTime(abStartPosition ?: 0L)}"
                                    else -> "A–B: повтор"
                                },
                                color = if (abEndPosition != null) accent else Color.White
                            )
                        }
                        TextButton(onClick = {
                            shuffleEnabled = !shuffleEnabled
                            controller?.shuffleModeEnabled = shuffleEnabled
                        }) { Text(if (shuffleEnabled) "Перемешивание: вкл." else "Перемешивание", color = accent) }
                    }
                    subtitleNotice?.let {
                        Text(it, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(start = 14.dp, bottom = 3.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Фоновое воспроизведение", color = Color.White, fontSize = 12.sp)
                            Text(
                                if (backgroundPlaybackEnabled) "Продолжать звук после сворачивания" else "При сворачивании видео будет остановлено",
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = backgroundPlaybackEnabled,
                            onCheckedChange = {
                                onBackgroundPlaybackChange(it)
                                revealControls()
                            }
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(Color(0xB8000000)).navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoTimeline(controller = controller, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
                    Box {
                        TextButton(onClick = { speedMenu = true }) {
                            Text("${String.format(Locale.US, "%.2f", speed)}×", color = accent)
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f).forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(if (value == 1f) "Обычная скорость" else "${value}×") },
                                    onClick = {
                                        speed = value
                                        controller?.setPlaybackSpeed(value)
                                        speedMenu = false
                                        controlsVisible = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { seekByInterval(-1L); controlsVisible = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x99000000), contentColor = Color.White)
                ) { Text("−$seekIntervalSeconds") }
                Button(
                    onClick = {
                        if (controller?.isPlaying == true) controller.pause() else controller?.play()
                        controlsVisible = true
                    },
                    modifier = Modifier.size(68.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) { Text(if (controller?.isPlaying == true) "Ⅱ" else "▶", fontSize = 25.sp) }
                FilledTonalButton(
                    onClick = { seekByInterval(1L); controlsVisible = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x99000000), contentColor = Color.White)
                ) { Text("+$seekIntervalSeconds") }
            }
        }
    }

    if (bookmarkDialog) {
        AlertDialog(
            onDismissRequest = { bookmarkDialog = false },
            title = { Text("Закладки видео") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            val uri = bookmarkUri
                            val position = controller?.currentPosition
                            if (uri != null && position != null && position >= 0L) {
                                if (bookmarks.none { kotlin.math.abs(it - position) < 3000L }) {
                                    bookmarks = (bookmarks + position).sorted()
                                    saveVideoBookmarks(bookmarkPreferences, uri, bookmarks)
                                }
                            }
                        },
                        enabled = controller != null
                    ) { Text("＋ Сохранить текущий момент", color = accent) }
                    if (bookmarks.isEmpty()) {
                        Text("Закладок пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    bookmarks.forEach { position ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    controller?.seekTo(position)
                                    bookmarkDialog = false
                                    revealControls()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(formatVideoTime(position), color = MaterialTheme.colorScheme.onSurface) }
                            TextButton(onClick = {
                                val uri = bookmarkUri
                                bookmarks = bookmarks.filterNot { it == position }
                                if (uri != null) saveVideoBookmarks(bookmarkPreferences, uri, bookmarks)
                            }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { bookmarkDialog = false }) { Text("Готово", color = accent) }
            }
        )
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Настройки", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { settingsOpen = false }) { Text("Готово", color = accent) }
                }

                Text("Формат кадра", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "По размеру" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        "Заполнить" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        "Растянуть" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        "4:3" to 5
                    ).forEach { (label, mode) ->
                        FilterChip(
                            selected = aspectMode == mode,
                            onClick = { updateAspectMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Цвет боковых полей", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Чёрный" to 0, "Тёмно-серый" to 1, "Акцент" to 2, "Свой цвет" to 3).forEach { (label, mode) ->
                        FilterChip(
                            selected = sideFieldMode == mode,
                            onClick = { updateSideFieldMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }
                if (sideFieldMode == 3) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customSideFieldColorText,
                            onValueChange = { value ->
                                val hex = value.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6)
                                customSideFieldColorText = hex
                                if (hex.length == 6) {
                                    val parsed = runCatching { (0xFF000000L or hex.toLong(16)).toInt() }.getOrNull()
                                    if (parsed != null) updateCustomSideFieldColor(parsed)
                                }
                            },
                            label = { Text("HEX") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier.size(width = 52.dp, height = 48.dp)
                                .background(Color(customSideFieldColorArgb), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Шаг перемотки", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 30).forEach { seconds ->
                        FilterChip(
                            selected = seekIntervalSeconds == seconds,
                            onClick = { updateSeekInterval(seconds) },
                            label = { Text("$seconds сек") }
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Не выключать экран", style = MaterialTheme.typography.bodyLarge)
                        Text("Во время просмотра", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = keepScreenOn, onCheckedChange = updateKeepScreenOn)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Фоновое воспроизведение", style = MaterialTheme.typography.bodyLarge)
                        Text("Продолжать звук после сворачивания", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = backgroundPlaybackEnabled,
                        onCheckedChange = onBackgroundPlaybackChange
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }


private fun saveSnapshotToGallery(context: android.content.Context, bitmap: Bitmap): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "KWPlayer_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KW Player")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@runCatching false
    try {
        val written = resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } == true
        if (!written) {
            resolver.delete(uri, null, null)
            false
        } else {
            val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, published, null, null) > 0
        }
    } catch (error: Exception) {
        resolver.delete(uri, null, null)
        throw error
    }
}.getOrDefault(false)

private fun writeSnapshotToUri(
    context: android.content.Context,
    uri: android.net.Uri,
    bitmap: Bitmap
): Boolean = runCatching {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    } == true
}.getOrDefault(false)

private fun loadVideoBookmarks(
    preferences: android.content.SharedPreferences,
    uri: String
): List<Long> = runCatching {
    val values = JSONArray(preferences.getString(uri, "[]") ?: "[]")
    buildList {
        for (index in 0 until values.length()) {
            val position = values.optLong(index, -1L)
            if (position >= 0L) add(position)
        }
    }.distinct().sorted()
}.getOrDefault(emptyList())

private fun saveVideoBookmarks(
    preferences: android.content.SharedPreferences,
    uri: String,
    positions: List<Long>
) {
    val values = JSONArray()
    positions.distinct().sorted().forEach { values.put(it) }
    preferences.edit().putString(uri, values.toString()).apply()
}

@Composable
private fun VideoTimeline(controller: MediaController?, modifier: Modifier = Modifier) {
    var position by remember(controller) { mutableLongStateOf(controller?.currentPosition?.coerceAtLeast(0L) ?: 0L) }
    var duration by remember(controller) { mutableLongStateOf(controller?.duration?.takeIf { it > 0L } ?: 0L) }
    var preview by remember(controller) { mutableFloatStateOf(position.toFloat()) }
    var seeking by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                duration = player.duration.takeIf { it > 0L } ?: 0L
                if (!seeking) position = player.currentPosition.coerceAtLeast(0L)
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        val player = controller ?: return@LaunchedEffect
        while (true) {
            if (!seeking) position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: 0L
            delay(750)
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(formatVideoTime(position), color = Color.White, fontSize = 11.sp)
        Slider(
            value = if (seeking) preview.coerceIn(0f, duration.coerceAtLeast(1L).toFloat())
            else position.toFloat().coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
            onValueChange = { preview = it; seeking = true },
            onValueChangeFinished = {
                controller?.seekTo(preview.toLong())
                seeking = false
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
        )
        Text("-" + formatVideoTime((duration - position).coerceAtLeast(0L)), color = Color.White, fontSize = 11.sp)
    }
}

private fun formatVideoTime(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = seconds / 3600L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, (seconds / 60L) % 60L, seconds % 60L)
    } else {
        String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
    }
}
