package com.intervalpulse.timer.service

import com.intervalpulse.timer.model.TimerConfig
import com.intervalpulse.timer.model.TimerPhase
import com.intervalpulse.timer.model.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object TimerRepository {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun updateConfig(newConfig: TimerConfig) {
        _state.update { current ->
            if (!current.isActiveSession) {
                current.copy(
                    config = newConfig,
                    phase = TimerPhase.WORKOUT,
                    remainingSeconds = newConfig.workoutSeconds,
                    totalPhaseSeconds = newConfig.workoutSeconds,
                    currentRound = 1
                )
            } else {
                current.copy(config = newConfig)
            }
        }
    }

    fun updateState(transform: (TimerState) -> TimerState) {
        _state.update(transform)
    }
}
