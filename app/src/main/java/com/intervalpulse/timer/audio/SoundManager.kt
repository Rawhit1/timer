package com.intervalpulse.timer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.intervalpulse.timer.R
import com.intervalpulse.timer.model.SoundOption

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<SoundOption, Int>()
    private var tickSoundId: Int = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        SoundOption.entries.forEach { option ->
            option.rawResId?.let { resId ->
                soundIds[option] = soundPool.load(context, resId, 1)
            }
        }
        tickSoundId = soundPool.load(context, R.raw.sound_countdown_tick, 1)
    }

    fun playPhaseSound(option: SoundOption, vibrate: Boolean = true) {
        val soundId = soundIds[option]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        if (vibrate) {
            triggerVibration(180L)
        }
    }

    fun playCountdownTick() {
        if (tickSoundId != 0) {
            soundPool.play(tickSoundId, 0.7f, 0.7f, 0, 0, 1.0f)
        }
    }

    private fun triggerVibration(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun release() {
        soundPool.release()
    }
}
