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
        // Ensure the overlay service is running to display the call UI.
        startService(Intent(this, OverlayService::class.java))
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        // If another call is still active (e.g. call-waiting), show its state instead of going Idle.
        val remaining = calls.firstOrNull { it != call }
        if (remaining != null) updateState(remaining)
        else CallManager.updateCall(CallState.Idle)
    }

    private fun updateState(call: Call) {
        val details = call.details
        val number = details.handle?.schemeSpecificPart ?: ""
        val displayName = details.callerDisplayName
            ?.takeIf { it.isNotBlank() } ?: number

        CallManager.updateCall(
            when (call.details.state) {
                Call.STATE_RINGING -> CallState.Incoming(call, displayName, number)
                Call.STATE_ACTIVE,
                Call.STATE_DIALING,
                Call.STATE_CONNECTING -> CallState.Active(call, displayName, number)
                else -> CallState.Idle
            }
        )
    }
}
