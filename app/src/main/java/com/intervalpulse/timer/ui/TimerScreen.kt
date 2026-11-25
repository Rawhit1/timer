package com.intervalpulse.timer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intervalpulse.timer.model.TimerConfig
import com.intervalpulse.timer.model.TimerPhase
import com.intervalpulse.timer.model.TimerState
import com.intervalpulse.timer.ui.theme.CardBorder
import com.intervalpulse.timer.ui.theme.CardDark
import com.intervalpulse.timer.ui.theme.RestCyan
import com.intervalpulse.timer.ui.theme.SurfaceDark
import com.intervalpulse.timer.ui.theme.TextPrimary
import com.intervalpulse.timer.ui.theme.TextSecondary
import com.intervalpulse.timer.ui.theme.WorkoutEmerald

@Composable
fun TimerScreen(
    state: TimerState,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    onAdjustRemaining: (Int) -> Unit,
    onEnterPip: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateConfig: (TimerConfig) -> Unit
) {
    val accentColor by animateColorAsState(
        targetValue = if (state.phase == TimerPhase.WORKOUT) WorkoutEmerald else RestCyan,
        label = "phaseAccent"
    )

    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    Scaffold(
        containerColor = SurfaceDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IntervalPulse",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.config.totalRounds == 0) {
                            "Round ${state.currentRound} • Continuous Loop"
                        } else {
                            "Round ${state.currentRound} of ${state.config.totalRounds}"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onEnterPip,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CardDark,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Mini PiP Mode")
                    }
                    FilledTonalIconButton(
                        onClick = onOpenSettings,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CardDark,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Phase Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (state.isPaused) "${state.phase.label} • PAUSED" else state.phase.label,
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Countdown Ring
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * state.progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (state.phase == TimerPhase.WORKOUT) {
                            "Next: Rest (${formatShort(state.config.restSeconds)})"
                        } else {
                            "Next: Workout (${formatShort(state.config.workoutSeconds)})"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Controls
            if (!state.isActiveSession) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkoutEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Workout Loop", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPauseResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (state.isRunning) "Pause" else "Resume",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444))
                        }
                    }

                    // Live mid-timer +/- adjustments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-30, -15, -10, 10, 15, 30).forEach { delta ->
                            val label = if (delta > 0) "+${delta}s" else "${delta}s"
                            OutlinedButton(
                                onClick = { onAdjustRemaining(delta) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (delta > 0) accentColor else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Interval Configuration Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DurationAdjustCard(
                    title = "Workout",
                    seconds = state.config.workoutSeconds,
                    accentColor = WorkoutEmerald,
                    enabled = !state.isActiveSession,
                    modifier = Modifier.weight(1f),
                    onChangeSeconds = { newSecs ->
                        onUpdateConfig(state.config.copy(workoutSeconds = newSecs.coerceIn(5, 3600)))
                    }
                )

                DurationAdjustCard(
                    title = "Rest",
                    seconds = state.config.restSeconds,
                    accentColor = RestCyan,
                    enabled = !state.isActiveSession,
                    modifier = Modifier.weight(1f),
                    onChangeSeconds = { newSecs ->
                        onUpdateConfig(state.config.copy(restSeconds = newSecs.coerceIn(5, 3600)))
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Presets
            if (!state.isActiveSession) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip("60s / 40s", modifier = Modifier.weight(1f)) {
                        onUpdateConfig(state.config.copy(workoutSeconds = 60, restSeconds = 40))
                    }
                    PresetChip("45s / 15s", modifier = Modifier.weight(1f)) {
                        onUpdateConfig(state.config.copy(workoutSeconds = 45, restSeconds = 15))
                    }
                    PresetChip("30s / 30s", modifier = Modifier.weight(1f)) {
                        onUpdateConfig(state.config.copy(workoutSeconds = 30, restSeconds = 30))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PiP & Sound Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettings
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mini Mode & Sounds",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.config.useFloatingOverlay) {
                            "Floating Overlay (${state.config.overlayWidthDp} dp) • Workout: ${state.config.workoutSound.displayName} • Rest: ${state.config.restSound.displayName}"
                        } else {
                            "PiP Ratio ${String.format("%.2f:1", state.config.pipAspectRatio)} • Workout: ${state.config.workoutSound.displayName} • Rest: ${state.config.restSound.displayName}"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationAdjustCard(
    title: String,
    seconds: Int,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChangeSeconds: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = formatShort(seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            // Add row: +10s, +15s, +30s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(10, 15, 30).forEach { step ->
                    OutlinedButton(
                        onClick = { onChangeSeconds(seconds + step) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Text("+${step}s", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Subtract row: -10s, -15s, -30s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(10, 15, 30).forEach { step ->
                    OutlinedButton(
                        onClick = { onChangeSeconds(seconds - step) },
                        enabled = enabled && seconds > 5,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("-${step}s", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Text(label, fontSize = 12.sp, color = TextPrimary)
    }
}

private fun formatShort(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0 && s == 0) "${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"
}
