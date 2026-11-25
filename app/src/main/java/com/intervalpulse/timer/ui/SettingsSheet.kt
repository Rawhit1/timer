package com.intervalpulse.timer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intervalpulse.timer.model.PipWidthPreset
import com.intervalpulse.timer.model.SoundOption
import com.intervalpulse.timer.model.TimerConfig
import com.intervalpulse.timer.ui.theme.CardBorder
import com.intervalpulse.timer.ui.theme.CardDark
import com.intervalpulse.timer.ui.theme.RestCyan
import com.intervalpulse.timer.ui.theme.SurfaceDark
import com.intervalpulse.timer.ui.theme.TextPrimary
import com.intervalpulse.timer.ui.theme.TextSecondary
import com.intervalpulse.timer.ui.theme.WorkoutEmerald
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    config: TimerConfig,
    hasOverlayPermission: Boolean,
    onUpdateConfig: (TimerConfig) -> Unit,
    onPreviewSound: (SoundOption) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Timer & Mini-Mode Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // 1. PiP Width / Aspect Ratio
            SectionHeader("PiP (Mini) Window Width")
            Text(
                text = "Default matches YouTube (16:9). Adjust preset or slider to make the PiP window narrower or wider.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PipWidthPreset.entries.forEach { preset ->
                    val selected = abs(config.pipAspectRatio - preset.ratio) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateConfig(config.copy(pipAspectRatio = preset.ratio)) },
                        label = { Text(preset.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorkoutEmerald.copy(alpha = 0.25f),
                            selectedLabelColor = WorkoutEmerald
                        )
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Custom PiP Aspect Ratio", fontSize = 13.sp, color = TextPrimary)
                    Text(
                        text = String.format("%.2f : 1", config.pipAspectRatio),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutEmerald
                    )
                }
                Slider(
                    value = config.pipAspectRatio,
                    onValueChange = { onUpdateConfig(config.copy(pipAspectRatio = it)) },
                    valueRange = 1.0f..2.39f,
                    colors = SliderDefaults.colors(
                        thumbColor = WorkoutEmerald,
                        activeTrackColor = WorkoutEmerald
                    )
                )
            }

            // Optional Floating Overlay Mode for exact dp width
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardDark,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Overlay Mode (Custom Width)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Uses a draggable mini-window with exact dp width beyond Android PiP limits.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = config.useFloatingOverlay,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasOverlayPermission) {
                                    onRequestOverlayPermission()
                                } else {
                                    onUpdateConfig(config.copy(useFloatingOverlay = enabled))
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = WorkoutEmerald)
                        )
                    }

                    if (config.useFloatingOverlay) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overlay Width", fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = "${config.overlayWidthDp} dp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RestCyan
                                )
                            }
                            Slider(
                                value = config.overlayWidthDp.toFloat(),
                                onValueChange = {
                                    onUpdateConfig(config.copy(overlayWidthDp = it.toInt()))
                                },
                                valueRange = 140f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = RestCyan,
                                    activeTrackColor = RestCyan
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CardBorder)

            // 2. Rounds & Countdown Ticks
            SectionHeader("Loop & Countdown")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("3-2-1 Countdown Ticks", fontSize = 14.sp, color = TextPrimary)
                    Text("Play subtle tick in final 3 seconds", fontSize = 12.sp, color = TextSecondary)
                }
                Switch(
                    checked = config.countdownTicksEnabled,
                    onCheckedChange = { onUpdateConfig(config.copy(countdownTicksEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = WorkoutEmerald)
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rounds", fontSize = 14.sp, color = TextPrimary)
                    Text(
                        text = if (config.totalRounds == 0) "Continuous Loop (∞)" else "${config.totalRounds} Rounds",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutEmerald
                    )
                }
                Slider(
                    value = config.totalRounds.toFloat(),
                    onValueChange = { onUpdateConfig(config.copy(totalRounds = it.toInt())) },
                    valueRange = 0f..30f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = WorkoutEmerald,
                        activeTrackColor = WorkoutEmerald
                    )
                )
            }

            HorizontalDivider(color = CardBorder)

            // 3. Workout Sound Picker
            SectionHeader("Workout Start Sound", accentColor = WorkoutEmerald)
            SoundPickerList(
                selected = config.workoutSound,
                accentColor = WorkoutEmerald,
                onSelect = { sound ->
                    onUpdateConfig(config.copy(workoutSound = sound))
                    onPreviewSound(sound)
                }
            )

            HorizontalDivider(color = CardBorder)

            // 4. Rest / Timer Sound Picker
            SectionHeader("Rest / Timer Sound", accentColor = RestCyan)
            SoundPickerList(
                selected = config.restSound,
                accentColor = RestCyan,
                onSelect = { sound ->
                    onUpdateConfig(config.copy(restSound = sound))
                    onPreviewSound(sound)
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentColor: Color = TextPrimary) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor
    )
}

@Composable
private fun SoundPickerList(
    selected: SoundOption,
    accentColor: Color,
    onSelect: (SoundOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SoundOption.entries.forEach { option ->
            val isSelected = option == selected
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.16f) else CardDark,
                border = BorderStroke(1.dp, if (isSelected) accentColor else CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option.displayName,
                            fontSize = 14.sp,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
