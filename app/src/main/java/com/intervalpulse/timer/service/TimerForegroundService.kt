package com.intervalpulse.timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.intervalpulse.timer.MainActivity
import com.intervalpulse.timer.R
import com.intervalpulse.timer.audio.SoundManager
import com.intervalpulse.timer.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "interval_pulse_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.intervalpulse.timer.ACTION_START"
        const val ACTION_PAUSE_RESUME = "com.intervalpulse.timer.ACTION_PAUSE_RESUME"
        const val ACTION_SKIP = "com.intervalpulse.timer.ACTION_SKIP"
        const val ACTION_STOP = "com.intervalpulse.timer.ACTION_STOP"
        const val ACTION_ADJUST_TIME = "com.intervalpulse.timer.ACTION_ADJUST_TIME"
        const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"

        fun adjustTime(context: Context, deltaSeconds: Int) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                this.action = ACTION_ADJUST_TIME
                putExtra(EXTRA_DELTA_SECONDS, deltaSeconds)
            }
            context.startService(intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                this.action = action
            }
            if (action == ACTION_START && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private lateinit var soundManager: SoundManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession()
            ACTION_PAUSE_RESUME -> togglePauseResume()
            ACTION_SKIP -> skipPhase()
            ACTION_STOP -> stopSession()
            ACTION_ADJUST_TIME -> adjustRemainingTime(intent.getIntExtra(EXTRA_DELTA_SECONDS, 0))
        }
        return START_NOT_STICKY
    }

    private fun startSession() {
        val cfg = TimerRepository.state.value.config
        TimerRepository.updateState {
            it.copy(
                phase = TimerPhase.WORKOUT,
                remainingSeconds = cfg.workoutSeconds,
                totalPhaseSeconds = cfg.workoutSeconds,
                currentRound = 1,
                isRunning = true,
                isPaused = false
            )
        }
        soundManager.playPhaseSound(cfg.workoutSound)
        promoteToForeground()
        startTicker()
    }

    private fun togglePauseResume() {
        val current = TimerRepository.state.value
        if (!current.isActiveSession) return

        if (current.isRunning) {
            tickerJob?.cancel()
            TimerRepository.updateState { it.copy(isRunning = false, isPaused = true) }
            updateNotification()
        } else if (current.isPaused) {
            TimerRepository.updateState { it.copy(isRunning = true, isPaused = false) }
            updateNotification()
            startTicker()
        }
    }

    private fun adjustRemainingTime(deltaSeconds: Int) {
        val current = TimerRepository.state.value
        if (!current.isActiveSession || deltaSeconds == 0) return
        val newRemaining = (current.remainingSeconds + deltaSeconds).coerceIn(1, 3600)
        val newTotal = maxOf(current.totalPhaseSeconds, newRemaining)
        TimerRepository.updateState {
            it.copy(
                remainingSeconds = newRemaining,
                totalPhaseSeconds = newTotal
            )
        }
        updateNotification()
    }

    private fun skipPhase() {
        val current = TimerRepository.state.value
        if (!current.isActiveSession) return
        advanceToNextPhase()
        if (TimerRepository.state.value.isRunning) {
            startTicker()
        }
    }

    private fun stopSession() {
        tickerJob?.cancel()
        val cfg = TimerRepository.state.value.config
        TimerRepository.updateState {
            it.copy(
                phase = TimerPhase.WORKOUT,
                remainingSeconds = cfg.workoutSeconds,
                totalPhaseSeconds = cfg.workoutSeconds,
                currentRound = 1,
                isRunning = false,
                isPaused = false
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var nextTickAt = SystemClock.elapsedRealtime() + 1000L
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val sleepMs = (nextTickAt - now).coerceAtLeast(0L)
                delay(sleepMs)
                nextTickAt += 1000L

                val current = TimerRepository.state.value
                if (!current.isRunning) break

                val nextRemaining = current.remainingSeconds - 1
                if (nextRemaining > 0) {
                    TimerRepository.updateState { it.copy(remainingSeconds = nextRemaining) }
                    if (current.config.countdownTicksEnabled && nextRemaining in 1..3) {
                        soundManager.playCountdownTick()
                    }
                    updateNotification()
                } else {
                    advanceToNextPhase()
                    if (!TimerRepository.state.value.isRunning) break
                }
            }
        }
    }

    private fun advanceToNextPhase() {
        val current = TimerRepository.state.value
        val cfg = current.config

        if (current.phase == TimerPhase.WORKOUT) {
            TimerRepository.updateState {
                it.copy(
                    phase = TimerPhase.REST,
                    remainingSeconds = cfg.restSeconds,
                    totalPhaseSeconds = cfg.restSeconds
                )
            }
            soundManager.playPhaseSound(cfg.restSound)
            updateNotification()
        } else {
            val nextRound = current.currentRound + 1
            if (cfg.totalRounds in 1 until nextRound) {
                // Completed all requested rounds
                soundManager.playPhaseSound(cfg.restSound)
                stopSession()
                return
            }
            TimerRepository.updateState {
                it.copy(
                    phase = TimerPhase.WORKOUT,
                    remainingSeconds = cfg.workoutSeconds,
                    totalPhaseSeconds = cfg.workoutSeconds,
                    currentRound = nextRound
                )
            }
            soundManager.playPhaseSound(cfg.workoutSound)
            updateNotification()
        }
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = TimerRepository.state.value
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TimerForegroundService::class.java).apply { action = ACTION_PAUSE_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TimerForegroundService::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, TimerForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mins = state.remainingSeconds / 60
        val secs = state.remainingSeconds % 60
        val timeText = String.format("%02d:%02d", mins, secs)
        val roundText = if (state.config.totalRounds > 0) {
            "Round ${state.currentRound}/${state.config.totalRounds}"
        } else {
            "Round ${state.currentRound} (Loop)"
        }
        val statusSuffix = if (state.isPaused) " • Paused" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${state.phase.label} — $timeText")
            .setContentText("$roundText$statusSuffix")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(0, if (state.isRunning) "Pause" else "Resume", pauseResumeIntent)
            .addAction(0, "Skip", skipIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Interval Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active workout or rest countdown in the notification bar"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        soundManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
