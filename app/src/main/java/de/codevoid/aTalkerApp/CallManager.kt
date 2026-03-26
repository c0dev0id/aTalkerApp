package de.codevoid.aTalkerApp

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CallUiState {
    /** No active call — transient state; OverlayService auto-transitions to ShowingContacts */
    object Idle : CallUiState()
    /** Contacts list is visible */
    object ShowingContacts : CallUiState()
    /** Dialpad is visible */
    object ShowingDialpad : CallUiState()
    /** Incoming call ringing */
    data class Incoming(
        val call: Call,
        val displayName: String,
        val number: String,
    ) : CallUiState()
    /** Call connected and in progress */
    data class Active(
        val call: Call,
        val displayName: String,
        val number: String,
    ) : CallUiState()
}

object CallManager {
    private val _state = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    fun update(state: CallUiState) {
        _state.value = state
    }

    /** Switch to contacts list. No-op if a call is active. */
    fun showContacts() {
        val s = _state.value
        if (s !is CallUiState.Incoming && s !is CallUiState.Active) {
            _state.value = CallUiState.ShowingContacts
        }
    }

    /** Switch to dialpad. No-op if a call is active. */
    fun showDialpad() {
        val s = _state.value
        if (s !is CallUiState.Incoming && s !is CallUiState.Active) {
            _state.value = CallUiState.ShowingDialpad
        }
    }
}
