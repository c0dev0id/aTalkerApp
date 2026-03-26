package de.codevoid.aTalkerApp.input

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives key-press/key-release broadcasts from DMD (Thork) remote controllers.
 *
 * Action : com.thorkracing.wireddevices.keypress
 * Extras : key_press   (Int) – keydown event with standard Android keycode
 *          key_release (Int) – keyup event with standard Android keycode
 *
 * Known keycodes: 19=UP, 20=DOWN, 21=LEFT, 22=RIGHT, 66=ENTER, 111=ESCAPE,
 *                 136=ZOOM_IN, 137=ZOOM_OUT
 */
class DmdRemoteReceiver(
    private val onKey: (keyCode: Int, isDown: Boolean) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        when {
            intent.hasExtra(EXTRA_PRESS)   -> onKey(intent.getIntExtra(EXTRA_PRESS, 0),   true)
            intent.hasExtra(EXTRA_RELEASE) -> onKey(intent.getIntExtra(EXTRA_RELEASE, 0), false)
        }
    }

    companion object {
        const val ACTION         = "com.thorkracing.wireddevices.keypress"
        private const val EXTRA_PRESS   = "key_press"
        private const val EXTRA_RELEASE = "key_release"
    }
}
