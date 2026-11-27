package fyi.kiyori.modernmediaplayer.feature.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import java.util.Locale

/** Everything the panel can ask the player screen to do. */
internal class PanelActions(
    val onSpeed: (Float) -> Unit,
    val onRepeat: (Int) -> Unit,
    val onShuffle: (Boolean) -> Unit,
    val onMarkA: () -> Unit,
    val onMarkB: () -> Unit,
    val onClearAb: () -> Unit,
    val onAddBookmark: () -> Unit,
    val onJumpBookmark: (Long) -> Unit,
    val onDeleteBookmark: (Long) -> Unit,
    val onSleep: (Int) -> Unit,
    val onScreenshot: () -> Unit,
    val onPickSubtitleFile: () -> Unit,
    val onBackgroundChange: (Boolean) -> Unit,
    val onOpenSoundOutput: () -> Unit,
    val onFrameStep: (Boolean) -> Unit
)

private enum class PanelPage {
    Main, Quality, Speed, Subtitles, SubtitleStyle, Audio, Aspect, Fields, Filters,
    LoopAb, Frames, Bookmarks, Sleep, Step, Repeat, Info
}

private val PanelBackground = Color(0xF2212121)
private val PanelMuted = Color(0xFFAAAAAA)
private val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)

internal fun formatSpeed(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) speed.toInt().toString()
    else String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    return text.replace('.', ',') + "×"
}

/**
 * YouTube-style settings sheet. Bottom-right card in landscape, full-width bottom sheet in
 * portrait, with a back arrow on every sub page. Pages hold the VLC extras.
 */
@Composable
internal fun PlayerSettingsPanel(
    controller: MediaController?,
    tracks: Tracks,
    ui: PlayerUiState,
    accent: Color,
    speed: Float,
    repeatMode: Int,
    shuffle: Boolean,
    abA: Long?,
    abB: Long?,
    bookmarks: List<Long>,
    sleepMinutesLeft: Int?,
    backgroundPlaybackEnabled: Boolean,
    actions: PanelActions,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(PanelPage.Main) }
    BackHandler(enabled = page != PanelPage.Main) { page = PanelPage.Main }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
        Surface(
            modifier = Modifier
                .align(if (landscape) Alignment.BottomEnd else Alignment.BottomCenter)
                .then(if (landscape) Modifier.width(380.dp).padding(12.dp) else Modifier.fillMaxWidth())
                .heightIn(max = maxHeight * 0.88f),
            color = PanelBackground,
            shape = RoundedCornerShape(if (landscape) 14.dp else 16.dp)
        ) {
            Column(
                Modifier
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                val videoOptions = tracks.options(C.TRACK_TYPE_VIDEO)
                val audioOptions = tracks.options(C.TRACK_TYPE_AUDIO)
                val textOptions = tracks.options(C.TRACK_TYPE_TEXT)
                val textOff = controller?.isTrackTypeDisabled(C.TRACK_TYPE_TEXT) == true ||
                    textOptions.none { it.selected }

                when (page) {
                    PanelPage.Main -> {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Настройки", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Закрыть", tint = Color.White) }
                        }
                        val autoQuality = controller?.hasTrackOverride(C.TRACK_TYPE_VIDEO) != true
                        val currentQuality = videoOptions.firstOrNull { it.selected }?.label
                        PanelRow(
                            Icons.Rounded.HighQuality, "Качество",
                            when {
                                currentQuality == null -> "—"
                                autoQuality && videoOptions.size > 1 -> "Авто · $currentQuality"
                                else -> currentQuality
                            }
                        ) { page = PanelPage.Quality }
                        PanelRow(Icons.Rounded.Speed, "Скорость воспроизведения", formatSpeed(speed)) { page = PanelPage.Speed }
                        PanelRow(
                            Icons.Rounded.Subtitles, "Субтитры",
                            if (textOff) "Выкл." else textOptions.firstOrNull { it.selected }?.label ?: "—"
                        ) { page = PanelPage.Subtitles }
                        PanelRow(
                            Icons.Rounded.Audiotrack, "Звуковая дорожка",
                            audioOptions.firstOrNull { it.selected }?.label ?: "—"
                        ) { page = PanelPage.Audio }
                        PanelRow(Icons.Rounded.AspectRatio, "Формат кадра", aspectNames[ui.aspectMode]) { page = PanelPage.Aspect }
                        PanelRow(Icons.Rounded.Palette, "Боковые поля", fieldNames[ui.fieldMode]) { page = PanelPage.Fields }
                        PanelRow(Icons.Rounded.Tune, "Фильтры изображения", if (ui.filtersDefault) "Выкл." else "Изменены") { page = PanelPage.Filters }
                        PanelDivider()
                        PanelRow(
                            Icons.Rounded.SwapHoriz, "Повтор отрезка A–B",
                            when {
                                abA != null && abB != null -> "${formatPlayerTime(abA)} – ${formatPlayerTime(abB)}"
                                abA != null -> "A ${formatPlayerTime(abA)}"
                                else -> "Выкл."
                            }
                        ) { page = PanelPage.LoopAb }
                        PanelRow(Icons.Rounded.SkipNext, "Покадровая перемотка", null) { page = PanelPage.Frames }
                        PanelRow(Icons.Rounded.Bookmark, "Закладки", bookmarks.size.toString()) { page = PanelPage.Bookmarks }
                        PanelRow(Icons.Rounded.Bedtime, "Таймер сна", sleepMinutesLeft?.let { "$it мин" } ?: "Выкл.") { page = PanelPage.Sleep }
                        PanelRow(Icons.Rounded.FastForward, "Шаг перемотки", "${ui.seekStepSec} с") { page = PanelPage.Step }
                        PanelRow(
                            Icons.Rounded.Repeat, "Повтор и перемешивание",
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> "Один"
                                Player.REPEAT_MODE_ALL -> "Все"
                                else -> "Выкл."
                            } + if (shuffle) " · перемешано" else ""
                        ) { page = PanelPage.Repeat }
                        PanelRow(Icons.Rounded.Headphones, "Вывод звука", "Системная панель") { actions.onOpenSoundOutput() }
                        PanelRow(Icons.Rounded.PhotoCamera, "Снимок кадра", null) { actions.onScreenshot() }
                        PanelRow(Icons.Rounded.Info, "Информация о медиа", null) { page = PanelPage.Info }
                        PanelDivider()
                        PanelSwitch("Воспроизведение в фоне", "Звук продолжается после сворачивания", backgroundPlaybackEnabled, accent, actions.onBackgroundChange)
                        PanelSwitch("Картинка в картинке", "Окно появляется при выходе, пока видео играет", ui.pipOnLeave, accent) { ui.updatePipOnLeave(it) }
                        PanelSwitch("Не выключать экран", "Пока видео воспроизводится", ui.keepScreenOn, accent) { ui.updateKeepScreenOn(it) }
                        PanelSwitch("Ползунок цвета акцента", "Выкл. — красный, как в YouTube", ui.accentProgress, accent) { ui.updateAccentProgress(it) }
                    }

                    PanelPage.Quality -> {
                        PanelHeader("Качество") { page = PanelPage.Main }
                        PanelOption(
                            "Авто", controller?.hasTrackOverride(C.TRACK_TYPE_VIDEO) != true,
                            sub = "Подбирается плеером"
                        ) { controller?.clearTrackOverride(C.TRACK_TYPE_VIDEO) }
                        videoOptions.forEach { option ->
                            PanelOption(
                                option.label,
                                option.selected && controller?.hasTrackOverride(C.TRACK_TYPE_VIDEO) == true
                            ) { controller?.selectTrack(option) }
                        }
                        if (videoOptions.size <= 1) {
                            PanelHint("В этом файле одна видеодорожка, выбор доступен только для потоков с несколькими качествами.")
                        }
                    }

                    PanelPage.Speed -> {
                        PanelHeader("Скорость воспроизведения") { page = PanelPage.Main }
                        PanelSlider(
                            "Точно: ${formatSpeed(speed)}", speed, 0.25f..4f, 74, accent
                        ) { actions.onSpeed(((it * 20f).toInt() / 20f).coerceIn(0.25f, 4f)) }
                        speedPresets.forEach { preset ->
                            PanelOption(if (preset == 1f) "Обычная" else formatSpeed(preset), speed == preset) { actions.onSpeed(preset) }
                        }
                    }

                    PanelPage.Subtitles -> {
                        PanelHeader("Субтитры") { page = PanelPage.Main }
                        PanelOption("Выключены", textOff) { controller?.disableTrackType(C.TRACK_TYPE_TEXT) }
                        textOptions.forEach { option ->
                            PanelOption(option.label, option.selected && !textOff) { controller?.selectTrack(option) }
                        }
                        PanelRow(null, "Загрузить файл субтитров…", null) { actions.onPickSubtitleFile() }
                        PanelRow(null, "Оформление субтитров", null) { page = PanelPage.SubtitleStyle }
                    }

                    PanelPage.SubtitleStyle -> {
                        PanelHeader("Оформление субтитров") { page = PanelPage.Subtitles }
                        listOf(0.75f to "Мелкие", 1f to "Обычные", 1.25f to "Крупные", 1.5f to "Очень крупные", 2f to "Огромные")
                            .forEach { (scale, label) ->
                                PanelOption(label, ui.subtitleScale == scale) { ui.updateSubtitleScale(scale) }
                            }
                        PanelSwitch("Тёмная подложка", "Лучше читаются на светлом кадре", ui.subtitleBackground, accent) { ui.updateSubtitleBackground(it) }
                    }

                    PanelPage.Audio -> {
                        PanelHeader("Звуковая дорожка") { page = PanelPage.Main }
                        audioOptions.forEach { option ->
                            PanelOption(option.label, option.selected) { controller?.selectTrack(option) }
                        }
                        if (audioOptions.isEmpty()) PanelHint("Звуковые дорожки не найдены.")
                    }

                    PanelPage.Aspect -> {
                        PanelHeader("Формат кадра") { page = PanelPage.Main }
                        aspectNames.forEachIndexed { index, name ->
                            PanelOption(name, ui.aspectMode == index) { ui.updateAspectMode(index) }
                        }
                    }

                    PanelPage.Fields -> {
                        PanelHeader("Боковые поля") { page = PanelPage.Main }
                        fieldNames.forEachIndexed { index, name ->
                            PanelOption(
                                name, ui.fieldMode == index,
                                sub = if (index == FIELD_DYNAMIC) "Цвет берётся с краёв кадра" else null
                            ) { ui.updateFieldMode(index) }
                        }
                        if (ui.fieldMode == FIELD_CUSTOM) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                customFieldColors.forEach { argb ->
                                    val selected = ui.customFieldColor == argb
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(argb))
                                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0x44FFFFFF), CircleShape)
                                            .clickable { ui.updateCustomFieldColor(argb) }
                                    )
                                }
                            }
                        }
                        PanelHint("Поля видны, когда видео не заполняет экран: по бокам в альбомной ориентации, сверху и снизу в портретной.")
                    }

                    PanelPage.Filters -> {
                        PanelHeader("Фильтры изображения") { page = PanelPage.Main }
                        PanelSlider("Яркость: ${(ui.brightness * 100).toInt()}", ui.brightness, -0.5f..0.5f, 0, accent) { ui.updateBrightness(it) }
                        PanelSlider("Контраст: ${(ui.contrast * 100).toInt()}%", ui.contrast, 0.5f..1.5f, 0, accent) { ui.updateContrast(it) }
                        PanelSlider("Насыщенность: ${(ui.saturation * 100).toInt()}%", ui.saturation, 0f..2f, 0, accent) { ui.updateSaturation(it) }
                        PanelButton("Сбросить", accent) { ui.resetFilters() }
                    }

                    PanelPage.LoopAb -> {
                        PanelHeader("Повтор отрезка A–B") { page = PanelPage.Main }
                        PanelHint(
                            "A: ${abA?.let { formatPlayerTime(it) } ?: "не задана"}    B: ${abB?.let { formatPlayerTime(it) } ?: "не задана"}"
                        )
                        PanelButton("Задать A (текущая позиция)", accent) { actions.onMarkA() }
                        PanelButton("Задать B (текущая позиция)", accent) { actions.onMarkB() }
                        PanelButton("Сбросить", accent) { actions.onClearAb() }
                    }

                    PanelPage.Frames -> {
                        PanelHeader("Покадровая перемотка") { page = PanelPage.Main }
                        PanelButton("Кадр назад", accent) { actions.onFrameStep(false) }
                        PanelButton("Кадр вперёд", accent) { actions.onFrameStep(true) }
                        PanelHint("Шаг считается по частоте кадров видео и может отличаться на один кадр.")
                    }

                    PanelPage.Bookmarks -> {
                        PanelHeader("Закладки") { page = PanelPage.Main }
                        PanelButton("Добавить закладку здесь", accent) { actions.onAddBookmark() }
                        bookmarks.forEach { ms ->
                            Row(
                                Modifier.fillMaxWidth().clickable { actions.onJumpBookmark(ms) }.padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatPlayerTime(ms), color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
                                IconButton(onClick = { actions.onDeleteBookmark(ms) }) {
                                    Icon(Icons.Rounded.Delete, "Удалить закладку ${formatPlayerTime(ms)}", tint = PanelMuted)
                                }
                            }
                        }
                        if (bookmarks.isEmpty()) PanelHint("Закладок пока нет.")
                    }

                    PanelPage.Sleep -> {
                        PanelHeader("Таймер сна") { page = PanelPage.Main }
                        PanelOption("Выкл.", sleepMinutesLeft == null) { actions.onSleep(0) }
                        listOf(15, 30, 45, 60, 90).forEach { minutes ->
                            PanelOption("$minutes мин", false) { actions.onSleep(minutes) }
                        }
                        PanelHint("Таймер работает, пока открыт плеер.")
                    }

                    PanelPage.Step -> {
                        PanelHeader("Шаг перемотки") { page = PanelPage.Main }
                        listOf(5, 10, 15, 30).forEach { sec ->
                            PanelOption("$sec с", ui.seekStepSec == sec) { ui.updateSeekStep(sec) }
                        }
                    }

                    PanelPage.Repeat -> {
                        PanelHeader("Повтор и перемешивание") { page = PanelPage.Main }
                        PanelOption("Без повтора", repeatMode == Player.REPEAT_MODE_OFF) { actions.onRepeat(Player.REPEAT_MODE_OFF) }
                        PanelOption("Повторять видео", repeatMode == Player.REPEAT_MODE_ONE) { actions.onRepeat(Player.REPEAT_MODE_ONE) }
                        PanelOption("Повторять всё", repeatMode == Player.REPEAT_MODE_ALL) { actions.onRepeat(Player.REPEAT_MODE_ALL) }
                        PanelSwitch("Перемешать", null, shuffle, accent, actions.onShuffle)
                    }

                    PanelPage.Info -> {
                        PanelHeader("Информация о медиа") { page = PanelPage.Main }
                        val facts = tracks.describe()
                        facts.forEach { (name, value) ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(name, color = PanelMuted, fontSize = 12.sp)
                                Text(value, color = Color.White, fontSize = 15.sp)
                            }
                        }
                        if (facts.isEmpty()) PanelHint("Данные появятся, когда начнётся воспроизведение.")
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Color.White) }
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PanelRow(icon: ImageVector?, label: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                value, color = PanelMuted, fontSize = 14.sp, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp).padding(start = 8.dp)
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = PanelMuted)
    }
}

@Composable
private fun PanelOption(label: String, selected: Boolean, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, "Выбрано", tint = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 15.sp)
            if (sub != null) Text(sub, color = PanelMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PanelSwitch(label: String, desc: String?, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 15.sp)
            if (desc != null) Text(desc, color = PanelMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
        )
    }
}

@Composable
private fun PanelSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
        )
    }
}

@Composable
private fun PanelButton(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PanelHint(text: String) {
    Text(text, color = PanelMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun PanelDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color(0x22FFFFFF)))
}
