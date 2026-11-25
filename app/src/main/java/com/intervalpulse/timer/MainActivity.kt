package com.intervalpulse.timer

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intervalpulse.timer.audio.SoundManager
import com.intervalpulse.timer.data.PreferencesRepository
import com.intervalpulse.timer.service.FloatingOverlayService
import com.intervalpulse.timer.service.TimerForegroundService
import com.intervalpulse.timer.service.TimerRepository
import com.intervalpulse.timer.ui.PipTimerView
import com.intervalpulse.timer.ui.SettingsSheet
import com.intervalpulse.timer.ui.TimerScreen
import com.intervalpulse.timer.ui.theme.IntervalPulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var soundManager: SoundManager
    private var isInPipMode by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepo = PreferencesRepository(this)
        soundManager = SoundManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            IntervalPulseTheme {
                val timerState by TimerRepository.state.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var showSettings by remember { mutableStateOf(false) }

                // Load persisted config on startup
                LaunchedEffect(Unit) {
                    prefsRepo.configFlow.collect { savedConfig ->
                        TimerRepository.updateConfig(savedConfig)
                        updatePipParams()
                    }
                }

                // Keep PiP actions & auto-enter state synced with timer state
                LaunchedEffect(timerState.isRunning, timerState.isActiveSession, timerState.config.pipAspectRatio, timerState.config.useFloatingOverlay) {
                    updatePipParams()
                }

                if (isInPipMode) {
                    PipTimerView(state = timerState)
                } else {
                    TimerScreen(
                        state = timerState,
                        onStart = {
                            TimerForegroundService.sendAction(this, TimerForegroundService.ACTION_START)
                        },
                        onPauseResume = {
                            TimerForegroundService.sendAction(this, TimerForegroundService.ACTION_PAUSE_RESUME)
                        },
                        onSkip = {
                            TimerForegroundService.sendAction(this, TimerForegroundService.ACTION_SKIP)
                        },
                        onStop = {
                            TimerForegroundService.sendAction(this, TimerForegroundService.ACTION_STOP)
                        },
                        onAdjustRemaining = { delta ->
                            TimerForegroundService.adjustTime(this, delta)
                        },
                        onEnterPip = { enterMiniMode() },
                        onOpenSettings = { showSettings = true },
                        onUpdateConfig = { newConfig ->
                            TimerRepository.updateConfig(newConfig)
                            scope.launch { prefsRepo.saveConfig(newConfig) }
                        }
                    )

                    if (showSettings) {
                        SettingsSheet(
                            config = timerState.config,
                            hasOverlayPermission = Settings.canDrawOverlays(this),
                            onUpdateConfig = { newConfig ->
                                TimerRepository.updateConfig(newConfig)
                                scope.launch { prefsRepo.saveConfig(newConfig) }
                            },
                            onPreviewSound = { sound ->
                                soundManager.playPhaseSound(sound, vibrate = false)
                            },
                            onRequestOverlayPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            },
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingOverlayService.hide(this)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = TimerRepository.state.value
        if (state.isActiveSession) {
            if (state.config.useFloatingOverlay && Settings.canDrawOverlays(this)) {
                FloatingOverlayService.show(this)
            } else if (!isInPictureInPictureMode) {
                enterPictureInPictureMode(buildPipParams())
            }
        }
    }

    private fun enterMiniMode() {
        val state = TimerRepository.state.value
        if (state.config.useFloatingOverlay && Settings.canDrawOverlays(this)) {
            FloatingOverlayService.show(this)
            moveTaskToBack(true)
        } else {
            enterPictureInPictureMode(buildPipParams())
        }
    }

    private fun updatePipParams() {
        if (!isInPictureInPictureMode) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val state = TimerRepository.state.value
        val ratio = state.config.pipAspectRatio.coerceIn(1.0f, 2.39f)
        val aspect = Rational((ratio * 100).toInt(), 100)

        val pauseResumeIntent = PendingIntent.getService(
            this,
            10,
            Intent(this, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_PAUSE_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeAction = RemoteAction(
            Icon.createWithResource(
                this,
                if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (state.isRunning) "Pause" else "Resume",
            if (state.isRunning) "Pause timer" else "Resume timer",
            pauseResumeIntent
        )

        val skipIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_SKIP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Skip",
            "Skip phase",
            skipIntent
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspect)
            .setActions(listOf(pauseResumeAction, skipAction))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(state.isActiveSession && !state.config.useFloatingOverlay)
        }

        return builder.build()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}
