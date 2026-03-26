package de.codevoid.aTalkerApp

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Stub activity required by Android to register this app as a valid default dialer candidate.
 * The real UI is handled by PhoneService (InCallService) + the overlay.
 */
class DialerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
