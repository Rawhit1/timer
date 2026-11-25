package com.intervalpulse.timer.model

import androidx.annotation.RawRes
import com.intervalpulse.timer.R

enum class TimerPhase(val label: String) {
    WORKOUT("WORKOUT"),
    REST("REST")
}

enum class SoundOption(
    val id: String,
    val displayName: String,
    @RawRes val rawResId: Int?
) {
    BOXING_BELL("boxing_bell", "Boxing Bell", R.raw.sound_boxing_bell),
    WHISTLE("whistle", "Coach Whistle", R.raw.sound_whistle),
    DIGITAL_BEEP("digital_beep", "Digital Beep", R.raw.sound_digital_beep),
    ENERGETIC_CHIME("energetic_chime", "Energetic Chime", R.raw.sound_energetic_chime),
    SOFT_CHIME("soft_chime", "Soft Chime", R.raw.sound_soft_chime),
    CALM_GONG("calm_gong", "Calm Gong", R.raw.sound_calm_gong),
    DESCENDING_TONE("descending_tone", "Descending Tone", R.raw.sound_descending_tone),
    SILENT("silent", "Silent (Vibrate Only)", null);

    companion object {
        fun fromId(id: String?): SoundOption {
            return entries.firstOrNull { it.id == id } ?: BOXING_BELL
        }
    }
}

enum class PipWidthPreset(
    val label: String,
    val ratio: Float,
    val numerator: Int,
    val denominator: Int
) {
    COMPACT("Compact (4:3)", 4f / 3f, 4, 3),
    YOUTUBE("YouTube (16:9)", 16f / 9f, 16, 9),
    WIDE("Wide (21:9)", 21f / 9f, 21, 9),
    MAX_WIDE("Max Wide (2.39:1)", 2.39f, 239, 100)
}

data class TimerConfig(
    val workoutSeconds: Int = 60,
    val restSeconds: Int = 40,
    val totalRounds: Int = 0, // 0 = continuous loop
    val workoutSound: SoundOption = SoundOption.BOXING_BELL,
    val restSound: SoundOption = SoundOption.SOFT_CHIME,
    val countdownTicksEnabled: Boolean = true,
    val pipAspectRatio: Float = 16f / 9f, // Default YouTube 16:9, clamped 1.0..2.39
    val useFloatingOverlay: Boolean = false,
    val overlayWidthDp: Int = 220
)

data class TimerState(
    val phase: TimerPhase = TimerPhase.WORKOUT,
    val remainingSeconds: Int = 60,
    val totalPhaseSeconds: Int = 60,
    val currentRound: Int = 1,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val config: TimerConfig = TimerConfig()
) {
    val progress: Float
        get() = if (totalPhaseSeconds > 0) {
            (remainingSeconds.toFloat() / totalPhaseSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isActiveSession: Boolean
        get() = isRunning || isPaused
}
