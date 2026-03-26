package de.codevoid.aTalkerApp.bluetooth

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent

/**
 * Captures Bluetooth headset button presses via a MediaSession.
 * Single button press = answer incoming / hang up active call.
 */
class BluetoothHeadsetManager(context: Context) {

    private val session = MediaSessionCompat(context, "aTalkerApp").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: android.content.Intent): Boolean {
                val event = intent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
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
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
    }

    var onHeadsetButton: () -> Unit = {}

    fun activate() {
        session.isActive = true
    }

    fun deactivate() {
        session.isActive = false
    }

    fun release() {
        session.release()
    }
}
