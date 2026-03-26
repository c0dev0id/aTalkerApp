package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

@Composable
fun OverlayRoot(onDial: (String) -> Unit) {
    val state by CallManager.state.collectAsState()

    // Cache the last call state so the card can still render during the exit animation
    // (state may already be Idle/ShowingContacts by the time the slide-out completes).
    var cachedCall by remember { mutableStateOf<CallUiState?>(null) }
    LaunchedEffect(state) {
        if (state is CallUiState.Incoming || state is CallUiState.Active) cachedCall = state
    }
    val isCallActive = state is CallUiState.Incoming || state is CallUiState.Active

    OverlayTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Contacts / Dialpad ──────────────────────────────────────────
            when (val s = state) {
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
                else -> Unit
            }

            // ── Call card (slides in from left, out to right) ───────────────
            AnimatedVisibility(
                visible = isCallActive,
                enter = slideInHorizontally(animationSpec = tween(380)) { -it },
                exit  = slideOutHorizontally(animationSpec = tween(320)) {  it },
            ) {
                when (val cs = cachedCall) {
                    is CallUiState.Incoming -> IncomingCallCard(
                        displayName = cs.displayName,
                        number = cs.number,
                        onAccept  = { cs.call.answer(0) },
                        onDecline = { cs.call.reject(false, null) },
                    )
                    is CallUiState.Active -> ActiveCallCard(
                        displayName = cs.displayName,
                        number = cs.number,
                        onHangUp = { cs.call.disconnect() },
                    )
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Wraps content in a 90 % × 90 % centered box.
 * When [dismissible] is true, tapping the backdrop calls [CallManager.hide].
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
