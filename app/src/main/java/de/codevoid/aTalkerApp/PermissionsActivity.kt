package de.codevoid.aTalkerApp

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Invisible launcher activity — no UI, no setContent(), window is fully transparent.
 *
 * Drives the one-time permission setup by surfacing system dialogs directly over
 * whatever app is running. Never shows our own UI so the foreground app is
 * never interrupted. Finishes itself the moment all permissions are in place.
 */
class PermissionsActivity : ComponentActivity() {

    private val corePermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    // Guard: only one in-flight system dialog at a time.
    private var requesting = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { requesting = false; proceed() }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { requesting = false; proceed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContent() — window is transparent, user sees their running app.
        proceed()
    }

    override fun onResume() {
        super.onResume()
        // Catches the return from overlay permission settings, which has no
        // ActivityResult callback. Guard prevents double-firing on first launch.
        if (!requesting) proceed()
    }

    private fun proceed() {
        when {
            !corePermissionsGranted() -> {
                requesting = true
                permissionLauncher.launch(corePermissions)
            }
            !Settings.canDrawOverlays(this) ->
                // No result callback — onResume() re-evaluates when user returns.
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                )
            !isDefaultDialer() -> {
                requesting = true
                val rm = getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                } else {
                    requesting = false
                    proceed()
                }
            }
            else -> {
                startForegroundService(Intent(this, OverlayService::class.java))
                finish()
            }
        }
    }

    private fun corePermissionsGranted() = corePermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun isDefaultDialer() =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
}
