package de.codevoid.aTalkerApp.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import de.codevoid.aTalkerApp.OverlayLifecycleOwner

/**
 * Manages a full-screen ComposeView added to WindowManager.
 * The window is focusable so it receives D-pad key events.
 */
class OverlayWindow(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private var composeView: ComposeView? = null
    val isShown: Boolean get() = composeView != null

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Focusable so D-pad/key events reach our Compose tree
        // NOT_TOUCH_MODAL so touches outside the view still reach the underlying app
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )

    fun show(onDial: (String) -> Unit) {
        if (composeView != null) return

        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { OverlayRoot(onDial) }
        }

        windowManager.addView(composeView, layoutParams)
    }

    /**
     * Synthesizes a key event from a DMD remote broadcast and injects it into the Compose tree.
     * The event is tagged with SOURCE_GAMEPAD so the keyboard-source filter in onKeyEvent
     * handlers does not suppress it (only native HID keyboard events are suppressed).
     */
    fun dispatchKey(keyCode: Int, isDown: Boolean) {
        val view = composeView ?: return
        val now = android.os.SystemClock.uptimeMillis()
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        val raw = KeyEvent(now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0)
        view.dispatchKeyEvent(KeyEvent.changeSource(raw, InputDevice.SOURCE_GAMEPAD))
    }

    fun dismiss() {
        composeView?.let {
            lifecycleOwner.onPause()
            lifecycleOwner.onStop()
            lifecycleOwner.onDestroy()
            windowManager.removeView(it)
            composeView = null
        }
    }
}
