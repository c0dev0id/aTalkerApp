package de.codevoid.aTalkerApp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.codevoid.aTalkerApp.bluetooth.BluetoothHeadsetManager
import de.codevoid.aTalkerApp.input.DmdRemoteReceiver
import de.codevoid.aTalkerApp.ui.OverlayWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "atalker_overlay"
        private const val NOTIF_ID = 1
        const val ACTION_SHOW_CONTACTS = "de.codevoid.aTalkerApp.SHOW_CONTACTS"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var overlayWindow: OverlayWindow
    private lateinit var bluetoothHeadset: BluetoothHeadsetManager
    private val dmdReceiver = DmdRemoteReceiver { keyCode, isDown ->
        overlayWindow.dispatchKey(keyCode, isDown)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        overlayWindow = OverlayWindow(this)
        overlayWindow.show { number ->
            val uri = Uri.parse("tel:$number")
            startActivity(Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        bluetoothHeadset = BluetoothHeadsetManager(this).apply {
            onHeadsetButton = ::handleHeadsetButton
            activate()
        }

        registerReceiver(
            dmdReceiver,
            IntentFilter(DmdRemoteReceiver.ACTION),
            RECEIVER_EXPORTED,
        )

        observeCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_CONTACTS -> CallManager.showContacts()
            // No action = started by user (PermissionsActivity) or by an incoming call
            // (PhoneService). Only open contacts when there is no active call.
            null -> if (CallManager.call.value is CallState.Idle) CallManager.showContacts()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(dmdReceiver)
        scope.cancel()
        overlayWindow.dismiss()
        bluetoothHeadset.release()
        super.onDestroy()
    }

    private fun handleHeadsetButton() {
        when (val s = CallManager.call.value) {
            is CallState.Incoming -> s.call.answer(0)
            is CallState.Active   -> s.call.disconnect()
            else -> Unit
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeCallState() {
        scope.launch {
            combine(CallManager.call, CallManager.nav) { call, nav -> call to nav }
                .collectLatest { (call, nav) ->
                    // Stop only when there is no active call AND the panel is closed.
                    if (call is CallState.Idle && nav == OverlayNav.Hidden) stopSelf()
                    getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
                }
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
