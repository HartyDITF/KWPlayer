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
    val backgroundPlaybackEnabled: Boolean = false
)

class KwSettingsRepository(context: Context) {
    private val store = context.applicationContext.kwSettingsDataStore

    val settings: Flow<KwSettings> = store.data.map {
        KwSettings(
            skin = it[SKIN] ?: "standard",
            accentArgb = it[ACCENT] ?: 0xFFFF8F1F.toInt(),
            backgroundPlaybackEnabled = it[BACKGROUND_PLAYBACK] ?: false
        )
    }

    suspend fun setSkin(value: String) { store.edit { it[SKIN] = value } }
    suspend fun setAccent(argb: Int) { store.edit { it[ACCENT] = argb } }
    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        store.edit { it[BACKGROUND_PLAYBACK] = enabled }
    }

    private companion object {
        val SKIN = stringPreferencesKey("skin")
        val ACCENT = intPreferencesKey("accent_argb")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback_enabled")
    }
}
