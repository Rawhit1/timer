package com.intervalpulse.timer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.intervalpulse.timer.model.SoundOption
import com.intervalpulse.timer.model.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "interval_pulse_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val WORKOUT_SECONDS = intPreferencesKey("workout_seconds")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
        val TOTAL_ROUNDS = intPreferencesKey("total_rounds")
        val WORKOUT_SOUND = stringPreferencesKey("workout_sound")
        val REST_SOUND = stringPreferencesKey("rest_sound")
        val COUNTDOWN_TICKS = booleanPreferencesKey("countdown_ticks")
        val PIP_ASPECT_RATIO = floatPreferencesKey("pip_aspect_ratio")
        val USE_FLOATING_OVERLAY = booleanPreferencesKey("use_floating_overlay")
        val OVERLAY_WIDTH_DP = intPreferencesKey("overlay_width_dp")
    }

    val configFlow: Flow<TimerConfig> = context.dataStore.data.map { prefs ->
        TimerConfig(
            workoutSeconds = (prefs[Keys.WORKOUT_SECONDS] ?: 60).coerceIn(5, 3600),
            restSeconds = (prefs[Keys.REST_SECONDS] ?: 40).coerceIn(5, 3600),
            totalRounds = (prefs[Keys.TOTAL_ROUNDS] ?: 0).coerceIn(0, 99),
            workoutSound = SoundOption.fromId(prefs[Keys.WORKOUT_SOUND] ?: SoundOption.BOXING_BELL.id),
            restSound = SoundOption.fromId(prefs[Keys.REST_SOUND] ?: SoundOption.SOFT_CHIME.id),
            countdownTicksEnabled = prefs[Keys.COUNTDOWN_TICKS] ?: true,
            pipAspectRatio = (prefs[Keys.PIP_ASPECT_RATIO] ?: (16f / 9f)).coerceIn(1.0f, 2.39f),
            useFloatingOverlay = prefs[Keys.USE_FLOATING_OVERLAY] ?: false,
            overlayWidthDp = (prefs[Keys.OVERLAY_WIDTH_DP] ?: 220).coerceIn(140, 360)
        )
    }

    suspend fun saveConfig(config: TimerConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WORKOUT_SECONDS] = config.workoutSeconds
            prefs[Keys.REST_SECONDS] = config.restSeconds
            prefs[Keys.TOTAL_ROUNDS] = config.totalRounds
            prefs[Keys.WORKOUT_SOUND] = config.workoutSound.id
            prefs[Keys.REST_SOUND] = config.restSound.id
            prefs[Keys.COUNTDOWN_TICKS] = config.countdownTicksEnabled
            prefs[Keys.PIP_ASPECT_RATIO] = config.pipAspectRatio.coerceIn(1.0f, 2.39f)
            prefs[Keys.USE_FLOATING_OVERLAY] = config.useFloatingOverlay
            prefs[Keys.OVERLAY_WIDTH_DP] = config.overlayWidthDp.coerceIn(140, 360)
        }
    }
}
