package de.codevoid.aTalkerApp

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CallState {
    object Idle : CallState()
    data class Incoming(
        val call: Call,
        val displayName: String,
        val number: String,
    ) : CallState()
    data class Active(
        val call: Call,
        val displayName: String,
        val number: String,
    ) : CallState()
}

enum class OverlayNav { Hidden, Contacts, Dialpad }

object CallManager {
    private val _call = MutableStateFlow<CallState>(CallState.Idle)
    val call: StateFlow<CallState> = _call.asStateFlow()

    private val _nav = MutableStateFlow(OverlayNav.Hidden)
    val nav: StateFlow<OverlayNav> = _nav.asStateFlow()

    fun updateCall(state: CallState) { _call.value = state }

    fun showContacts() { _nav.value = OverlayNav.Contacts }
    fun showDialpad()  { _nav.value = OverlayNav.Dialpad }
    fun hide()         { _nav.value = OverlayNav.Hidden }
}
