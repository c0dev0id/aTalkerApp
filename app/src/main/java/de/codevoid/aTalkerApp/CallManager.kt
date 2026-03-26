package de.codevoid.aTalkerApp

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CallUiState {
    /** No active call, contacts list not open */
    object Idle : CallUiState()
    /** User opened the contacts overlay */
    object ShowingContacts : CallUiState()
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

    fun showContacts() {
        if (_state.value is CallUiState.Idle) _state.value = CallUiState.ShowingContacts
    }

    fun hideContacts() {
        if (_state.value is CallUiState.ShowingContacts) _state.value = CallUiState.Idle
    }
}
