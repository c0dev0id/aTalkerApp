package de.codevoid.aTalkerApp

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.codevoid.aTalkerApp.ui.OverlayTheme
import de.codevoid.aTalkerApp.ui.TextSizeMedium
import de.codevoid.aTalkerApp.ui.TextSizeSmall

class PermissionsActivity : ComponentActivity() {

    private val corePermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private var resumeTick by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { tryStartService() }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { tryStartService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Core principle: this app is overlay-only and must never steal focus from
        // the foreground app. If setup is complete, start the service and get out
        // immediately — before any UI frame is rendered.
        tryStartService()
        if (allPermissionsGranted()) {
            finish()
            return
        }
        setContent { Screen() }
    }

    override fun onResume() {
        super.onResume()
        tryStartService()
        // Auto-close once the last permission is granted so the user lands back
        // in whichever app they were in before — not here.
        if (allPermissionsGranted()) {
            finish()
            return
        }
        resumeTick++
    }

    @Composable
    private fun Screen() {
        // Read resumeTick so this composable re-runs whenever onResume increments it.
        // This re-evaluates the permission/role checks without rebuilding the full composition.
        @Suppress("UNUSED_EXPRESSION") resumeTick

        OverlayTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SetupScreen(
                    onRequestPermissions = { permissionLauncher.launch(corePermissions) },
                    onRequestOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    onRequestDefaultDialer = ::requestDefaultDialer,
                    hasOverlay = Settings.canDrawOverlays(this),
                    hasRuntimePermissions = corePermissions.all {
                        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                    },
                    isDefaultDialer = getSystemService(RoleManager::class.java)
                        .isRoleHeld(RoleManager.ROLE_DIALER),
                )
            }
        }
    }

    private fun requestDefaultDialer() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    private fun allPermissionsGranted(): Boolean =
        Settings.canDrawOverlays(this) &&
        corePermissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED } &&
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)

    private fun tryStartService() {
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, OverlayService::class.java))
        }
    }
}

@Composable
private fun SetupScreen(
    onRequestPermissions: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    hasOverlay: Boolean,
    hasRuntimePermissions: Boolean,
    isDefaultDialer: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("aTalker Setup", fontSize = TextSizeMedium)

        PermissionRow("Overlay permission", hasOverlay, onRequestOverlay)
        PermissionRow("Runtime permissions (contacts, phone, notifications)", hasRuntimePermissions, onRequestPermissions)
        PermissionRow("Default dialer role", isDefaultDialer, onRequestDefaultDialer)

        Spacer(Modifier.height(16.dp))
        Text(
            "Grant all permissions above, then the overlay service starts automatically. " +
            "You can close this screen — the service runs in the background.",
            fontSize = TextSizeSmall,
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(if (granted) "✓" else "○", fontSize = TextSizeMedium)
        Text(label, modifier = Modifier.weight(1f), fontSize = TextSizeSmall)
        if (!granted) Button(onClick = onClick) { Text("Grant") }
    }
}
