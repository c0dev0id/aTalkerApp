package de.codevoid.aTalkerApp.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.codevoid.aTalkerApp.CallManager
import de.codevoid.aTalkerApp.CallUiState
import de.codevoid.aTalkerApp.data.Contact
import de.codevoid.aTalkerApp.data.ContactsRepository

/**
 * Top-level Composable hosted in the overlay window.
 * Observes CallManager.state and renders the appropriate screen.
 */
@Composable
fun OverlayRoot(context: Context, onDial: (String) -> Unit) {
    val state by CallManager.state.collectAsState()

    OverlayTheme {
        when (val s = state) {
            is CallUiState.Idle -> Unit
            is CallUiState.ShowingContacts -> ContactsScreenConnected(context, onDial)
            is CallUiState.Incoming -> IncomingCallScreen(
                displayName = s.displayName,
                number = s.number,
                onAccept = { s.call.answer(0) },
                onReject = { s.call.reject(false, null) },
            )
            is CallUiState.Active -> ActiveCallScreen(
                displayName = s.displayName,
                number = s.number,
                onHangUp = { s.call.disconnect() },
            )
        }
    }
}

@Composable
private fun ContactsScreenConnected(context: Context, onDial: (String) -> Unit) {
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        contacts = ContactsRepository.load(context)
        loading = false
    }

    if (loading) {
        LoadingScreen()
    } else {
        ContactsScreen(
            contacts = contacts,
            onCall = { contact ->
                CallManager.hideContacts()
                onDial(contact.phoneNumber)
            },
            onDismiss = { CallManager.hideContacts() },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(OverlayBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loading contacts…", color = TextSecondary, fontSize = TextSizeLarge)
    }
}
