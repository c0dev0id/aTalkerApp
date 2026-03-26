package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.CallManager
import de.codevoid.aTalkerApp.CallUiState
import de.codevoid.aTalkerApp.data.Contact
import de.codevoid.aTalkerApp.data.ContactsRepository

/**
 * Top-level Composable hosted in the overlay window.
 * All screens are framed at 90% width/height with a transparent backdrop.
 * Tapping the backdrop (or pressing Back/Escape) hides the overlay for
 * ShowingContacts and ShowingDialpad states.
 */
@Composable
fun OverlayRoot(onDial: (String) -> Unit) {
    val state by CallManager.state.collectAsState()

    OverlayTheme {
        when (val s = state) {
            is CallUiState.Idle, is CallUiState.Hidden -> Unit

            is CallUiState.ShowingContacts -> OverlayFrame(dismissible = true) {
                ContactsScreenConnected(onDial)
            }
            is CallUiState.ShowingDialpad -> OverlayFrame(dismissible = true) {
                DialpadScreen(
                    onDial = onDial,
                    onContacts = { CallManager.showContacts() },
                    onClose = { CallManager.hide() },
                )
            }
            is CallUiState.Incoming -> OverlayFrame(dismissible = false) {
                IncomingCallScreen(
                    displayName = s.displayName,
                    number = s.number,
                    onAccept = { s.call.answer(0) },
                    onReject = { s.call.reject(false, null) },
                )
            }
            is CallUiState.Active -> OverlayFrame(dismissible = false) {
                ActiveCallScreen(
                    displayName = s.displayName,
                    number = s.number,
                    onHangUp = { s.call.disconnect() },
                )
            }
        }
    }
}

/**
 * Wraps content in a 90 % × 90 % centered box.
 * When [dismissible] is true a transparent full-screen backdrop sits behind
 * the content; tapping it calls [CallManager.hide].
 */
@Composable
private fun OverlayFrame(
    dismissible: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (dismissible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { CallManager.hide() },
            )
        }
        // 90 % content area — consumes clicks so they don't reach the backdrop
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            content = content,
        )
    }
}

@Composable
private fun ContactsScreenConnected(onDial: (String) -> Unit) {
    val context = LocalContext.current
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
            onCall = { contact -> onDial(contact.phoneNumber) },
            onDialpad = { CallManager.showDialpad() },
            onClose = { CallManager.hide() },
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
