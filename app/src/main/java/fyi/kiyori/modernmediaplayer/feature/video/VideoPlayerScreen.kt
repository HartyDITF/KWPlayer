package fyi.kiyori.modernmediaplayer.feature.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Rational
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import fyi.kiyori.modernmediaplayer.R
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private val YtRed = Color(0xFFFF0000)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun mediaKeyOf(item: MediaItem?, fallback: String): String =
    item?.localConfiguration?.uri?.toString()
        ?: item?.requestMetadata?.mediaUri?.toString()
        ?: item?.mediaId?.takeIf { it.isNotBlank() && it != MediaItem.DEFAULT_MEDIA_ID }
        ?: fallback

/**
 * YouTube-style video player with VLC-grade features. Same entry point as before:
 * it is shown full screen by MainActivity.
 */
@Composable
fun VideoPlayerScreen(
    controller: MediaController?,
    title: String,
    accent: Color,
    backgroundPlaybackEnabled: Boolean,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PlayerPrefs(context) }
    val ui = remember(prefs) { PlayerUiState(prefs) }
    val ctrl by rememberUpdatedState(controller)
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // ---- player state mirrored from the controller ----
    var isPlaying by remember(controller) { mutableStateOf(controller?.isPlaying == true) }
    var playbackState by remember(controller) { mutableIntStateOf(controller?.playbackState ?: Player.STATE_IDLE) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var shuffle by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var videoAspect by remember { mutableFloatStateOf(0f) }
    var mediaKey by remember { mutableStateOf(title) }
    var hasNext by remember { mutableStateOf(false) }
    var hasPrev by remember { mutableStateOf(false) }
    var playError by remember { mutableStateOf<PlaybackException?>(null) }

    // ---- ui state ----
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(true) }
    var panelOpen by remember { mutableStateOf(false) }
    var inPip by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableLongStateOf(0L) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintStamp by remember { mutableLongStateOf(0L) }
    var longPressing by remember { mutableStateOf(false) }
    var flashSide by remember { mutableIntStateOf(0) }
    var flashSec by remember { mutableIntStateOf(0) }
    var markA by remember { mutableStateOf<Long?>(null) }
    var markB by remember { mutableStateOf<Long?>(null) }
    var bookmarks by remember { mutableStateOf<List<Long>>(emptyList()) }
    var sleepEndAt by remember { mutableLongStateOf(0L) }
    var sleepLeft by remember { mutableStateOf<Int?>(null) }
    val playerViewState = remember { mutableStateOf<PlayerView?>(null) }

    fun showHint(text: String) {
        hint = text
        hintStamp = SystemClock.elapsedRealtime()
    }

    // ---- listener: keep everything in sync ----
    DisposableEffect(controller) {
        fun sync(p: Player) {
            isPlaying = p.isPlaying
            playbackState = p.playbackState
            duration = if (p.isCurrentMediaItemLive) 0L else p.duration.takeIf { it > 0L } ?: 0L
            position = p.currentPosition.coerceAtLeast(0L)
            buffered = p.bufferedPosition.coerceAtLeast(0L)
            speed = p.playbackParameters.speed
            repeatMode = p.repeatMode
            shuffle = p.shuffleModeEnabled
            tracks = p.currentTracks
            val size = p.videoSize
            videoAspect = if (size.width > 0 && size.height > 0) {
                val raw = size.width * size.pixelWidthHeightRatio / size.height
                if (size.unappliedRotationDegrees % 180 != 0) 1f / raw else raw
            } else 0f
            hasNext = p.hasNextMediaItem()
            hasPrev = p.hasPreviousMediaItem()
            playError = p.playerError
            mediaKey = mediaKeyOf(p.currentMediaItem, title)
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                val itemChanged = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                sync(player)
                if (itemChanged) {
                    markA = null
                    markB = null
                }
            }
        }
        controller?.let {
            sync(it)
            it.addListener(listener)
        }
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(mediaKey) { bookmarks = prefs.bookmarks(mediaKey) }

    // position loop, also enforces the A-B repeat
    LaunchedEffect(controller, isPlaying) {
        while (true) {
            val p = ctrl
            if (p != null) {
                val pos = p.currentPosition.coerceAtLeast(0L)
                val a = markA
                val b = markB
                if (isPlaying && a != null && b != null && b > a && pos >= b) {
                    p.seekTo(a)
                    position = a
                } else {
                    position = pos
                }
                buffered = p.bufferedPosition.coerceAtLeast(0L)
            }
            delay(if (markA != null && markB != null) 100L else 250L)
        }
    }

    // sleep timer
    LaunchedEffect(sleepEndAt) {
        if (sleepEndAt == 0L) {
            sleepLeft = null
            return@LaunchedEffect
        }
        while (true) {
            val left = sleepEndAt - SystemClock.elapsedRealtime()
            if (left <= 0L) {
                ctrl?.pause()
                sleepEndAt = 0L
                break
            }
            sleepLeft = ceil(left / 60_000.0).toInt()
            delay(10_000L)
        }
    }

    // auto-hide controls, hint
    LaunchedEffect(controlsVisible, isPlaying, panelOpen, scrubbing, locked) {
        if (controlsVisible && isPlaying && !panelOpen && !scrubbing) {
            delay(3500L)
            controlsVisible = false
        }
    }
    LaunchedEffect(hint, hintStamp) {
        if (hint != null) {
            delay(900L)
            hint = null
        }
    }

    // ---- window / orientation / brightness / keep screen on ----
    val originalBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    DisposableEffect(activity) {
        val oldOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val wasKeepOn = activity?.window?.let {
            (it.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        } ?: false
        onDispose {
            activity?.requestedOrientation = oldOrientation
            activity?.window?.let { window ->
                if (wasKeepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            if (Build.VERSION.SDK_INT >= 31) {
                runCatching {
                    activity?.setPictureInPictureParams(
                        PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                    )
                }
            }
        }
    }
    DisposableEffect(activity, isPlaying, ui.keepScreenOn) {
        val window = activity?.window
        if (ui.keepScreenOn && isPlaying) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }
    LaunchedEffect(activity, fullscreen, inPip) {
        val window = activity?.window ?: return@LaunchedEffect
        if (inPip) return@LaunchedEffect
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

    // ---- picture in picture ----
    fun pipParams(autoEnter: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < 26) return null
        val aspect = (if (videoAspect > 0f) videoAspect else 16f / 9f).coerceIn(0.42f, 2.38f)
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational((aspect * 1000).toInt(), 1000))
        if (Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(autoEnter).setSeamlessResizeEnabled(false)
        return builder.build()
    }
    fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) {
            showHint("Картинка в картинке недоступна")
            return
        }
        val params = pipParams(false) ?: return
        panelOpen = false
        controlsVisible = false
        runCatching { activity?.enterPictureInPictureMode(params) }
    }
    val canAutoPip by rememberUpdatedState(ui.pipOnLeave && isPlaying)
    LaunchedEffect(videoAspect, isPlaying, ui.pipOnLeave) {
        if (Build.VERSION.SDK_INT >= 31) {
            pipParams(ui.pipOnLeave && isPlaying)?.let { runCatching { activity?.setPictureInPictureParams(it) } }
        }
    }
    DisposableEffect(activity) {
        val owner = activity as? ComponentActivity
        val leave = Runnable {
            if (Build.VERSION.SDK_INT in 26..30 && canAutoPip) {
                pipParams(false)?.let { runCatching { activity?.enterPictureInPictureMode(it) } }
            }
        }
        val modeListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
            if (inPip) {
                panelOpen = false
                controlsVisible = false
            }
        }
        owner?.addOnUserLeaveHintListener(leave)
        owner?.addOnPictureInPictureModeChangedListener(modeListener)
        onDispose {
            owner?.removeOnUserLeaveHintListener(leave)
            owner?.removeOnPictureInPictureModeChangedListener(modeListener)
        }
    }

    // ---- subtitle file picker ----
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val suffix = uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mime = when (suffix) {
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                "vtt" -> MimeTypes.TEXT_VTT
                "ass", "ssa" -> MimeTypes.TEXT_SSA
                else -> context.contentResolver.getType(uri) ?: MimeTypes.APPLICATION_SUBRIP
            }
            val player = ctrl
            val current = player?.currentMediaItem
            val source = current?.localConfiguration?.uri ?: current?.requestMetadata?.mediaUri
            if (player != null && current != null && source != null) {
                val wasPlaying = player.isPlaying
                val resumeAt = player.currentPosition.coerceAtLeast(0L)
                val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(mime)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                val updated = current.buildUpon().setUri(source).setSubtitleConfigurations(listOf(subtitle)).build()
                player.setMediaItem(updated, resumeAt)
                player.prepare()
                if (wasPlaying) player.play()
                showHint("Внешние субтитры добавлены")
            } else {
                showHint("Не удалось добавить субтитры")
            }
        }
    }

    // ---- actions ----
    fun seekToClamped(ms: Long) {
        val p = ctrl ?: return
        val max = if (duration > 0L) duration else Long.MAX_VALUE
        p.seekTo(ms.coerceIn(0L, max))
    }
    fun togglePlay() {
        val p = ctrl ?: return
        if (p.playbackState == Player.STATE_ENDED) {
            p.seekTo(0L)
            p.play()
        } else if (p.isPlaying) p.pause() else {
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            p.play()
        }
    }
    fun takeScreenshot() {
        val texture = playerViewState.value?.videoSurfaceView as? TextureView
        val bitmap: Bitmap? = if (texture != null && texture.isAvailable) texture.bitmap else null
        if (bitmap == null || Build.VERSION.SDK_INT < 29) {
            showHint(if (bitmap == null) "Кадр недоступен" else "Снимок требует Android 10+")
            return
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "KW_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KW Player")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("no uri")
                    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    true
                }.getOrDefault(false)
            }
            showHint(if (ok) "Снимок сохранён в Pictures/KW Player" else "Не удалось сохранить снимок")
        }
    }
    val bgChange by rememberUpdatedState(onBackgroundPlaybackChange)
    val actions = remember {
        PanelActions(
            onSpeed = { value -> ctrl?.setPlaybackSpeed(value) },
            onRepeat = { mode -> ctrl?.repeatMode = mode },
            onShuffle = { on -> ctrl?.shuffleModeEnabled = on },
            onMarkA = {
                markA = position
                if (markB != null && markB!! <= position) markB = null
                showHint("A: ${formatPlayerTime(position)}")
            },
            onMarkB = {
                val a = markA
                if (a == null || position <= a) showHint("Сначала задайте A раньше текущей позиции")
                else {
                    markB = position
                    showHint("B: ${formatPlayerTime(position)}")
                }
            },
            onClearAb = {
                markA = null
                markB = null
            },
            onAddBookmark = {
                val updated = (bookmarks + position).distinct().sorted()
                bookmarks = updated
                prefs.saveBookmarks(mediaKey, updated)
                showHint("Закладка ${formatPlayerTime(position)}")
            },
            onJumpBookmark = { ms ->
                seekToClamped(ms)
                panelOpen = false
            },
            onDeleteBookmark = { ms ->
                val updated = bookmarks - ms
                bookmarks = updated
                prefs.saveBookmarks(mediaKey, updated)
            },
            onSleep = { minutes ->
                sleepEndAt = if (minutes <= 0) 0L else SystemClock.elapsedRealtime() + minutes * 60_000L
            },
            onScreenshot = { takeScreenshot() },
            onPickSubtitleFile = { subtitlePicker.launch(arrayOf("*/*")) },
            onBackgroundChange = { bgChange(it) },
            onOpenSoundOutput = {
                val intent = Intent(
                    if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS
                )
                runCatching { (activity ?: context).startActivity(intent) }
            },
            onFrameStep = { forward ->
                val p = ctrl
                if (p != null) {
                    val fps = tracks.selectedFrameRate() ?: 25f
                    val step = (1000f / fps).roundToInt().coerceAtLeast(1).toLong()
                    p.pause()
                    seekToClamped(p.currentPosition + if (forward) step else -step)
                }
            }
        )
    }

    // ---- gestures ----
    var flashJob by remember { mutableStateOf<Job?>(null) }
    var seekBase by remember { mutableLongStateOf(0L) }
    var seekTarget by remember { mutableLongStateOf(0L) }
    var gestureStartValue by remember { mutableFloatStateOf(0f) }
    var gestureAccum by remember { mutableFloatStateOf(0f) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1f) }

    val gestureListener = remember {
        object : PlayerGestureListener {
            override fun onTap() {
                controlsVisible = !controlsVisible
            }

            override fun onDoubleTap(xFraction: Float) {
                if (locked) return
                val side = when {
                    xFraction < 0.4f -> -1
                    xFraction > 0.6f -> 1
                    else -> 0
                }
                if (side == 0) {
                    togglePlay()
                    return
                }
                if (side != flashSide) flashSec = 0
                flashSide = side
                flashSec += ui.seekStepSec
                ctrl?.let { seekToClamped(it.currentPosition + side * ui.seekStepSec * 1000L) }
                flashJob?.cancel()
                flashJob = scope.launch {
                    delay(700L)
                    flashSide = 0
                    flashSec = 0
                }
            }

            override fun onLongPressStart() {
                if (locked) return
                val p = ctrl ?: return
                speedBeforeLongPress = p.playbackParameters.speed
                p.setPlaybackSpeed(2f)
                longPressing = true
            }

            override fun onLongPressEnd() {
                if (!longPressing) return
                longPressing = false
                ctrl?.setPlaybackSpeed(speedBeforeLongPress)
            }

            override fun onVerticalDragStart(leftHalf: Boolean) {
                gestureAccum = 0f
                gestureStartValue = if (leftHalf) {
                    val current = activity?.window?.attributes?.screenBrightness ?: -1f
                    if (current >= 0f) current else runCatching {
                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                    }.getOrDefault(0.5f)
                } else {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
                }
            }

            override fun onVerticalDrag(leftHalf: Boolean, upFraction: Float) {
                if (locked) return
                gestureAccum += upFraction * 1.3f
                val value = (gestureStartValue + gestureAccum).coerceIn(0f, 1f)
                if (leftHalf) {
                    activity?.window?.let { w ->
                        w.attributes = w.attributes.apply { screenBrightness = value.coerceAtLeast(0.01f) }
                    }
                    showHint("Яркость ${(value * 100).roundToInt()}%")
                } else {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).roundToInt(), 0)
                    showHint("Громкость ${(value * 100).roundToInt()}%")
                }
            }

            override fun onVerticalDragEnd() = Unit

            override fun onHorizontalDragStart() {
                seekBase = ctrl?.currentPosition ?: 0L
                seekTarget = seekBase
            }

            override fun onHorizontalDrag(totalFraction: Float) {
                if (locked || duration <= 0L) return
                seekTarget = (seekBase + (totalFraction * 90_000f).toLong()).coerceIn(0L, duration)
                val delta = (seekTarget - seekBase) / 1000L
                showHint("${formatPlayerTime(seekTarget)} (${if (delta >= 0) "+" else ""}$delta с)")
            }

            override fun onHorizontalDragEnd(commit: Boolean) {
                if (commit && !locked && duration > 0L) seekToClamped(seekTarget)
            }

            override fun onPinchEnd(zoom: Float) {
                if (locked) return
                when {
                    zoom > 1.1f -> {
                        ui.updateAspectMode(ASPECT_ZOOM)
                        showHint("Заполнить")
                    }
                    zoom < 0.9f -> {
                        ui.updateAspectMode(ASPECT_FIT)
                        showHint("По размеру")
                    }
                }
            }
        }
    }

    BackHandler(enabled = !inPip) {
        when {
            panelOpen -> panelOpen = false
            locked -> {
                locked = false
                controlsVisible = true
            }
            fullscreen -> {
                fullscreen = false
                controlsVisible = true
            }
            else -> onClose()
        }
    }

    // ================= UI =================
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val boxW = maxWidth
        val boxH = maxHeight
        val fillMode = ui.aspectMode == ASPECT_ZOOM || ui.aspectMode == ASPECT_STRETCH
        val targetAspect = when (ui.aspectMode) {
            ASPECT_16_9 -> 16f / 9f
            ASPECT_4_3 -> 4f / 3f
            ASPECT_21_9 -> 21f / 9f
            ASPECT_1_1 -> 1f
            else -> if (videoAspect > 0f) videoAspect else 16f / 9f
        }
        val dispW: Dp
        val dispH: Dp
        if (fillMode) {
            dispW = boxW
            dispH = boxH
        } else if (boxW / boxH > targetAspect) {
            dispH = boxH
            dispW = boxH * targetAspect
        } else {
            dispW = boxW
            dispH = boxW / targetAspect
        }
        val hasFields = !fillMode && (dispW < boxW - 1.dp || dispH < boxH - 1.dp)
        val edges by rememberAmbientEdges(
            playerView = { playerViewState.value },
            active = ui.fieldMode == FIELD_DYNAMIC && hasFields && !inPip
        )
        FieldsBackground(
            mode = ui.fieldMode,
            customColor = ui.customFieldColor,
            accent = accent,
            edges = edges,
            displayW = dispW,
            displayH = dispH,
            modifier = Modifier.fillMaxSize()
        )

        // video
        AndroidView(
            modifier = Modifier.align(Alignment.Center).size(dispW, dispH),
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.kw_player_view, null, false) as PlayerView)
                    .also { playerViewState.value = it }
            },
            update = { pv ->
                if (pv.player !== controller) pv.player = controller
                val mode = when (ui.aspectMode) {
                    ASPECT_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ASPECT_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
                if (pv.resizeMode != mode) pv.resizeMode = mode

                val subKey = ui.subtitleScale to ui.subtitleBackground
                if (pv.getTag(SUBTITLE_TAG) != subKey) {
                    pv.setTag(SUBTITLE_TAG, subKey)
                    pv.subtitleView?.apply {
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * ui.subtitleScale)
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                if (ui.subtitleBackground) 0xB0000000.toInt() else android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                        )
                    }
                }

                val filterKey = Triple(ui.brightness, ui.contrast, ui.saturation)
                if (pv.getTag(FILTER_TAG) != filterKey) {
                    pv.setTag(FILTER_TAG, filterKey)
                    val surface = pv.videoSurfaceView
                    if (surface is View) {
                        if (ui.filtersDefault) {
                            surface.setLayerType(View.LAYER_TYPE_NONE, null)
                        } else {
                            val sat = ColorMatrix().apply { setSaturation(ui.saturation) }
                            val c = ui.contrast
                            val t = (0.5f * (1f - c) + ui.brightness) * 255f
                            val cb = ColorMatrix(
                                floatArrayOf(
                                    c, 0f, 0f, 0f, t,
                                    0f, c, 0f, 0f, t,
                                    0f, 0f, c, 0f, t,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                            cb.preConcat(sat)
                            val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cb) }
                            surface.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                        }
                    }
                }
            },
            onRelease = { pv ->
                pv.player = null
                if (playerViewState.value === pv) playerViewState.value = null
            }
        )

        // gesture layer (single detector)
        Box(Modifier.fillMaxSize().playerGestures(gestureListener))

        val showUi = !inPip

        // double tap feedback
        if (showUi && flashSide != 0) {
            Box(
                Modifier
                    .align(if (flashSide < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 48.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (flashSide < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                        null, tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                    Text("$flashSec сек", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // hints
        val pill: String? = when {
            longPressing -> "2× ▶▶"
            else -> hint
        }
        if (showUi && pill != null) {
            Text(
                pill,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xB3000000))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        if (showUi && playbackState == Player.STATE_BUFFERING && playError == null) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(48.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        // controls
        AnimatedVisibility(
            visible = showUi && controlsVisible && !locked && playError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent)))
                )
                Box(
                    Modifier.fillMaxWidth().height(160.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                )

                // top bar
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (fullscreen) fullscreen = false else onClose() }) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        title, color = Color.White, fontSize = 15.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    val textOptions = tracks.options(C.TRACK_TYPE_TEXT)
                    val textOn = textOptions.any { it.selected } && controller?.isTrackTypeDisabled(C.TRACK_TYPE_TEXT) != true
                    IconButton(onClick = {
                        val p = ctrl
                        if (p != null) {
                            if (textOn) p.disableTrackType(C.TRACK_TYPE_TEXT)
                            else textOptions.firstOrNull()?.let { p.selectTrack(it) }
                                ?: showHint("Субтитров нет. Загрузите файл в настройках")
                        }
                    }) {
                        Icon(
                            if (textOn) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionOff,
                            "Субтитры", tint = Color.White
                        )
                    }
                    IconButton(onClick = { panelOpen = true }) {
                        Icon(Icons.Rounded.Settings, "Настройки", tint = Color.White)
                    }
                }

                // centre
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton(Icons.Rounded.SkipPrevious, "Предыдущее", 48.dp, enabled = hasPrev) { ctrl?.seekToPreviousMediaItem() }
                    CircleButton(
                        when {
                            playbackState == Player.STATE_ENDED -> Icons.Rounded.Replay
                            isPlaying -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        if (isPlaying) "Пауза" else "Играть", 68.dp, enabled = true
                    ) { togglePlay() }
                    CircleButton(Icons.Rounded.SkipNext, "Следующее", 48.dp, enabled = hasNext) { ctrl?.seekToNextMediaItem() }
                }

                // bottom
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val shown = if (scrubbing) scrubPos else position
                        Text(
                            formatPlayerTime(shown) + if (duration > 0L) " / ${formatPlayerTime(duration)}" else "",
                            color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            locked = true
                            controlsVisible = true
                        }) { Icon(Icons.Rounded.Lock, "Заблокировать", tint = Color.White, modifier = Modifier.size(22.dp)) }
                        IconButton(onClick = { enterPip() }) {
                            Icon(Icons.Rounded.PictureInPictureAlt, "Картинка в картинке", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { fullscreen = !fullscreen }) {
                            Icon(
                                if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                "Во весь экран", tint = Color.White
                            )
                        }
                    }
                    YtSeekBar(
                        positionMs = if (scrubbing) scrubPos else position,
                        durationMs = duration,
                        bufferedMs = buffered,
                        scrubbing = scrubbing,
                        color = if (ui.accentProgress) accent else YtRed,
                        bookmarks = bookmarks,
                        markA = markA,
                        markB = markB,
                        onScrubStart = {
                            scrubbing = true
                            scrubPos = position
                        },
                        onScrub = { scrubPos = it },
                        onScrubEnd = {
                            seekToClamped(it)
                            position = it
                            scrubbing = false
                        }
                    )
                }
            }
        }

        // locked mode: only an unlock button
        if (showUi && locked && controlsVisible) {
            IconButton(
                onClick = {
                    locked = false
                    controlsVisible = true
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
            ) { Icon(Icons.Rounded.LockOpen, "Разблокировать", tint = Color.White) }
        }

        // error
        val err = playError
        if (showUi && err != null) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Не удалось воспроизвести видео", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(err.errorCodeName, color = Color(0xFFAAAAAA), fontSize = 12.sp)
                Text(
                    "Повторить", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable {
                            ctrl?.let {
                                it.prepare()
                                it.play()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
                Text(
                    "Закрыть", color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.clickable { onClose() }.padding(8.dp)
                )
            }
        }

        if (showUi && panelOpen) {
            PlayerSettingsPanel(
                controller = controller,
                tracks = tracks,
                ui = ui,
                accent = accent,
                speed = speed,
                repeatMode = repeatMode,
                shuffle = shuffle,
                abA = markA,
                abB = markB,
                bookmarks = bookmarks,
                sleepMinutesLeft = sleepLeft,
                backgroundPlaybackEnabled = backgroundPlaybackEnabled,
                actions = actions,
                onDismiss = { panelOpen = false }
            )
        }
    }
}

private const val SUBTITLE_TAG = 0x4B570001
private const val FILTER_TAG = 0x4B570002

@Composable
private fun CircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size).clip(CircleShape).background(Color(0x66000000))
    ) {
        Icon(
            icon, description,
            tint = if (enabled) Color.White else Color(0x66FFFFFF),
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
