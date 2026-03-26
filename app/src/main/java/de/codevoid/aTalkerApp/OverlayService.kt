package de.codevoid.aTalkerApp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.codevoid.aTalkerApp.bluetooth.BluetoothHeadsetManager
import de.codevoid.aTalkerApp.ui.OverlayWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "atalker_overlay"
        private const val NOTIF_ID = 1
        const val ACTION_SHOW_CONTACTS = "de.codevoid.aTalkerApp.SHOW_CONTACTS"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var overlayWindow: OverlayWindow
    private lateinit var bluetoothHeadset: BluetoothHeadsetManager
    private var overlayShown = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        overlayWindow = OverlayWindow(this)
        bluetoothHeadset = BluetoothHeadsetManager(this).apply {
            onHeadsetButton = ::handleHeadsetButton
            activate()
        }
        observeCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_CONTACTS) {
            CallManager.showContacts()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        overlayWindow.dismiss()
        bluetoothHeadset.release()
        super.onDestroy()
    }

    private fun handleHeadsetButton() {
        when (val s = CallManager.state.value) {
            is CallUiState.Incoming -> s.call.answer(0)
            is CallUiState.Active -> s.call.disconnect()
            else -> Unit
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeCallState() {
        scope.launch {
            CallManager.state.collectLatest { state ->
                when (state) {
                    is CallUiState.Idle -> hideOverlay()
                    else -> showOverlay()
                }
                // Refresh notification to reflect contacts-toggle availability
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, buildNotification())
            }
        }
    }

    private fun showOverlay() {
        if (!overlayShown) {
            overlayWindow.show { number ->
                val uri = Uri.parse("tel:$number")
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            overlayShown = true
        }
    }

    private fun hideOverlay() {
        if (overlayShown) {
            overlayWindow.dismiss()
            overlayShown = false
        }
    }

    private fun buildNotification(): Notification {
        val contactsIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_SHOW_CONTACTS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val setupIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, PermissionsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("aTalker")
            .setContentText("Phone overlay active")
            .setOngoing(true)
            .setContentIntent(setupIntent)
            .addAction(android.R.drawable.ic_menu_call, "Contacts", contactsIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Phone Overlay",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Persistent overlay for phone calls" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
