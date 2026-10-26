package app.counterweight.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore("settings")

enum class Gender { HE, SHE }

data class Settings(
    val onboarded: Boolean = false,
    /** How Russian words address the person: «справился» or «справилась». */
    val gender: Gender? = null,
    /** The hour of worries: shelved stones wait for it, and it brings one reminder if any wait. -1 for no reminder. */
    val worryHour: Int = 18,
    /** The morning hour of the weight of the day and of checking what came true; -1 for none. */
    val morning: Int = 9,
    val sound: Boolean = true,
    /** What holds the person up, one per line: what lies at the bottom of the well. */
    val props: String = "",
    /** Shuffles the weight of the day so it differs from phone to phone. */
    val salt: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        gender = Gender.entries.firstOrNull { it.name == this[GENDER] },
        worryHour = this[WORRY_HOUR] ?: 18,
        morning = this[MORNING] ?: 9,
        sound = this[SOUND] ?: true,
        props = this[PROPS] ?: "",
        salt = this[SALT] ?: 0,
    )

    suspend fun now(): Settings = settings.first().let { s ->
        if (s.salt != 0L) s else { update { it.copy(salt = Random.nextLong(1, Long.MAX_VALUE)) }; settings.first() }
    }

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[GENDER] = s.gender?.name ?: ""
            it[WORRY_HOUR] = s.worryHour
            it[MORNING] = s.morning
            it[SOUND] = s.sound
            it[PROPS] = s.props
            it[SALT] = s.salt
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GENDER = stringPreferencesKey("gender")
        val WORRY_HOUR = intPreferencesKey("worry_hour")
        val MORNING = intPreferencesKey("morning")
        val SOUND = booleanPreferencesKey("sound")
        val PROPS = stringPreferencesKey("props")
        val SALT = longPreferencesKey("salt")
    }
}
