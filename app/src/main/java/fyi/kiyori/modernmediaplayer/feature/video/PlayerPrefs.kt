package fyi.kiyori.modernmediaplayer.feature.video

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal const val FIELD_BLACK = 0
internal const val FIELD_DYNAMIC = 1
internal const val FIELD_ACCENT = 2
internal const val FIELD_CUSTOM = 3

internal const val ASPECT_FIT = 0
internal const val ASPECT_ZOOM = 1
internal const val ASPECT_STRETCH = 2
internal const val ASPECT_16_9 = 3
internal const val ASPECT_4_3 = 4
internal const val ASPECT_21_9 = 5
internal const val ASPECT_1_1 = 6

internal val aspectNames = listOf("По размеру", "Заполнить (обрезка)", "Растянуть", "16:9", "4:3", "21:9", "1:1")
internal val fieldNames = listOf("Чёрные", "Динамические (по кадру)", "Цвет акцента", "Свой цвет")

/** Colours offered for the "custom" side fields. */
internal val customFieldColors = listOf(
    0xFF1B3A5C.toInt(), 0xFF3A1B5C.toInt(), 0xFF14402F.toInt(),
    0xFF5C2A1B.toInt(), 0xFF2A2A2A.toInt(), 0xFF5C1B3A.toInt()
)

/** Small SharedPreferences wrapper for player-only settings and per-video bookmarks. */
internal class PlayerPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var fieldMode: Int
        get() = sp.getInt("field_mode", FIELD_DYNAMIC)
        set(value) { sp.edit().putInt("field_mode", value).apply() }

    var customFieldColor: Int
        get() = sp.getInt("field_custom", customFieldColors.first())
        set(value) { sp.edit().putInt("field_custom", value).apply() }

    var aspectMode: Int
        get() = sp.getInt("aspect_mode", ASPECT_FIT)
        set(value) { sp.edit().putInt("aspect_mode", value).apply() }

    var seekStepSec: Int
        get() = sp.getInt("seek_step_sec", 10)
        set(value) { sp.edit().putInt("seek_step_sec", value).apply() }

    var keepScreenOn: Boolean
        get() = sp.getBoolean("keep_screen_on", true)
        set(value) { sp.edit().putBoolean("keep_screen_on", value).apply() }

    var pipOnLeave: Boolean
        get() = sp.getBoolean("pip_on_leave", true)
        set(value) { sp.edit().putBoolean("pip_on_leave", value).apply() }

    /** false = YouTube red progress bar, true = the app accent colour. */
    var accentProgress: Boolean
        get() = sp.getBoolean("accent_progress", false)
        set(value) { sp.edit().putBoolean("accent_progress", value).apply() }

    var subtitleScale: Float
        get() = sp.getFloat("subtitle_scale", 1f)
        set(value) { sp.edit().putFloat("subtitle_scale", value).apply() }

    var subtitleBackground: Boolean
        get() = sp.getBoolean("subtitle_bg", false)
        set(value) { sp.edit().putBoolean("subtitle_bg", value).apply() }

    fun bookmarks(mediaKey: String): List<Long> =
        sp.getString(bookmarkKey(mediaKey), "").orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
            .sorted()

    fun saveBookmarks(mediaKey: String, values: List<Long>) {
        sp.edit().putString(bookmarkKey(mediaKey), values.joinToString(",")).apply()
    }

    private fun bookmarkKey(mediaKey: String) = "bm_" + mediaKey.hashCode()

    private companion object {
        const val NAME = "kw_video_player"
    }
}

/** Observable player settings. Persistent values are written through to [PlayerPrefs]. */
@Stable
internal class PlayerUiState(private val prefs: PlayerPrefs) {
    var fieldMode by mutableIntStateOf(prefs.fieldMode); private set
    var customFieldColor by mutableIntStateOf(prefs.customFieldColor); private set
    var aspectMode by mutableIntStateOf(prefs.aspectMode); private set
    var seekStepSec by mutableIntStateOf(prefs.seekStepSec); private set
    var keepScreenOn by mutableStateOf(prefs.keepScreenOn); private set
    var pipOnLeave by mutableStateOf(prefs.pipOnLeave); private set
    var accentProgress by mutableStateOf(prefs.accentProgress); private set
    var subtitleScale by mutableFloatStateOf(prefs.subtitleScale); private set
    var subtitleBackground by mutableStateOf(prefs.subtitleBackground); private set

    // Image filters apply to the current session only.
    var brightness by mutableFloatStateOf(0f); private set
    var contrast by mutableFloatStateOf(1f); private set
    var saturation by mutableFloatStateOf(1f); private set

    val filtersDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f

    fun updateFieldMode(value: Int) { fieldMode = value; prefs.fieldMode = value }
    fun updateCustomFieldColor(value: Int) { customFieldColor = value; prefs.customFieldColor = value }
    fun updateAspectMode(value: Int) { aspectMode = value; prefs.aspectMode = value }
    fun updateSeekStep(value: Int) { seekStepSec = value; prefs.seekStepSec = value }
    fun updateKeepScreenOn(value: Boolean) { keepScreenOn = value; prefs.keepScreenOn = value }
    fun updatePipOnLeave(value: Boolean) { pipOnLeave = value; prefs.pipOnLeave = value }
    fun updateAccentProgress(value: Boolean) { accentProgress = value; prefs.accentProgress = value }
    fun updateSubtitleScale(value: Float) { subtitleScale = value; prefs.subtitleScale = value }
    fun updateSubtitleBackground(value: Boolean) { subtitleBackground = value; prefs.subtitleBackground = value }
    fun updateBrightness(value: Float) { brightness = value }
    fun updateContrast(value: Float) { contrast = value }
    fun updateSaturation(value: Float) { saturation = value }
    fun resetFilters() { brightness = 0f; contrast = 1f; saturation = 1f }
}
