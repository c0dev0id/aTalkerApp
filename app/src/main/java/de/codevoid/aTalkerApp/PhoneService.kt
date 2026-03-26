package de.codevoid.aTalkerApp

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class PhoneService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = updateState(call)
    }

    override fun onCallAdded(call: Call) {
        call.registerCallback(callCallback)
        updateState(call)
        // Ensure overlay service is running to display the UI
        startService(Intent(this, OverlayService::class.java))
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        CallManager.update(CallUiState.Idle)
    }

    private fun updateState(call: Call) {
        val details = call.details
        val number = details.handle?.schemeSpecificPart ?: ""
        val displayName = details.callerDisplayName
            ?.takeIf { it.isNotBlank() } ?: number

        CallManager.update(
            when (call.state) {
                Call.STATE_RINGING -> CallUiState.Incoming(call, displayName, number)
                Call.STATE_ACTIVE,
                Call.STATE_DIALING,
                Call.STATE_CONNECTING -> CallUiState.Active(call, displayName, number)
                else -> CallUiState.Idle
            }
        )
    }
}
