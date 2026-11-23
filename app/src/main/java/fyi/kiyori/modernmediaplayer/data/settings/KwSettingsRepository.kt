package fyi.kiyori.modernmediaplayer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kwSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kw_player_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "kw_player_settings")) }
)

data class KwSettings(
    val skin: String = "standard",
    val accentArgb: Int = 0xFFFF8F1F.toInt(),
    val backgroundPlaybackEnabled: Boolean = false,
    val videoLayout: Int = 0,
    val videoAspectMode: Int = 0,
    val videoSideFieldMode: Int = 0,
    val keepScreenOn: Boolean = true
)

class KwSettingsRepository(context: Context) {
    private val store = context.applicationContext.kwSettingsDataStore

    val settings: Flow<KwSettings> = store.data.map {
        KwSettings(
            skin = when (it[SKIN]) {
                "light" -> "standard"
                "retro" -> "winamp"
                "neon" -> "amoled"
                "standard", "winamp", "spotify", "oneui", "amoled", "material_you", "classic_vlc" -> it[SKIN]!!
                else -> "standard"
            },
            accentArgb = it[ACCENT] ?: 0xFFFF8F1F.toInt(),
            backgroundPlaybackEnabled = it[BACKGROUND_PLAYBACK] ?: false,
            videoLayout = (it[VIDEO_LAYOUT] ?: 0).coerceIn(0, 2),
            videoAspectMode = when (it[VIDEO_ASPECT_MODE]) { 0, 3, 4, 5 -> it[VIDEO_ASPECT_MODE]!!; else -> 0 },
            videoSideFieldMode = (it[VIDEO_SIDE_FIELD_MODE] ?: 0).coerceIn(0, 2),
            keepScreenOn = it[KEEP_SCREEN_ON] ?: true
        )
    }

    suspend fun setSkin(value: String) { store.edit { it[SKIN] = value } }
    suspend fun setAccent(argb: Int) { store.edit { it[ACCENT] = argb } }
    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        store.edit { it[BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setVideoLayout(value: Int) {
        store.edit { it[VIDEO_LAYOUT] = value.coerceIn(0, 2) }
    }

    suspend fun setVideoAspectMode(value: Int) {
        store.edit { it[VIDEO_ASPECT_MODE] = if (value == 0 || value == 3 || value == 4 || value == 5) value else 0 }
    }
    suspend fun setVideoSideFieldMode(value: Int) {
        store.edit { it[VIDEO_SIDE_FIELD_MODE] = value.coerceIn(0, 2) }
    }
    suspend fun setKeepScreenOn(enabled: Boolean) {
        store.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    private companion object {
        val VIDEO_ASPECT_MODE = intPreferencesKey("video_aspect_mode")
        val VIDEO_SIDE_FIELD_MODE = intPreferencesKey("video_side_field_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VIDEO_LAYOUT = intPreferencesKey("video_layout")
        val SKIN = stringPreferencesKey("skin")
        val ACCENT = intPreferencesKey("accent_argb")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback_enabled")
    }
}
