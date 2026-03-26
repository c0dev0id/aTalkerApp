package de.codevoid.aTalkerApp.bluetooth

import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent

/**
 * Captures Bluetooth headset button presses via a MediaSession.
 * Single button press = answer incoming / hang up active call.
 */
class BluetoothHeadsetManager(context: Context) {

    private val session = MediaSessionCompat(context, "aTalkerApp").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent): Boolean {
                val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    ?: return false

                if (event.action == KeyEvent.ACTION_DOWN &&
                    (event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                     event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                ) {
                    onHeadsetButton()
                    return true
                }
                return false
            }
        })
        // setFlags() for HANDLES_MEDIA_BUTTONS / HANDLES_TRANSPORT_CONTROLS is
        // deprecated since API 31; the session handles these automatically.
    }

    var onHeadsetButton: () -> Unit = {}

    fun activate() {
        session.isActive = true
    }

    fun release() {
        session.release()
    }
}
