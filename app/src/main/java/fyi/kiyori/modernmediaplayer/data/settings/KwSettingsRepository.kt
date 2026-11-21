package fyi.kiyori.modernmediaplayer.data.settings
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.kwSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kw_player_settings", produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "kw_player_settings")) })
data class KwSettings(val skin: String = "standard", val accentArgb: Int = 0xFFFF8F1F.toInt())
class KwSettingsRepository(context: Context) {
 private val store = context.applicationContext.kwSettingsDataStore
 val settings: Flow<KwSettings> = store.data.map { KwSettings(it[SKIN] ?: "standard", it[ACCENT] ?: 0xFFFF8F1F.toInt()) }
 suspend fun setSkin(value: String) { store.edit { it[SKIN] = value } }
 suspend fun setAccent(argb: Int) { store.edit { it[ACCENT] = argb } }
 private companion object { val SKIN = stringPreferencesKey("skin"); val ACCENT = intPreferencesKey("accent_argb") }
}
