package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.CallManager
import de.codevoid.aTalkerApp.CallState
import de.codevoid.aTalkerApp.OverlayNav

// Fixed width reserved for the call card on the right edge.
// The panel always stops here so a call card never overlaps panel content.
private val CallZoneWidth = 270.dp

@Composable
fun OverlayRoot(onDial: (String) -> Unit) {
    val callState by CallManager.call.collectAsState()
    val nav       by CallManager.nav.collectAsState()

    // Keep last call state alive so the card can fade out gracefully.
    var cachedCall by remember { mutableStateOf<CallState?>(null) }
    LaunchedEffect(callState) {
        if (callState is CallState.Incoming || callState is CallState.Active) cachedCall = callState
    }
    val isCallActive = callState is CallState.Incoming || callState is CallState.Active

    OverlayTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Backdrop — tapping outside the panel dismisses it ─────────────
            if (nav != OverlayNav.Hidden) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { CallManager.hide() },
                )
            }

            // ── Left panel (contacts / history / dialpad) ─────────────────────
            // fillMaxSize + end padding constrains TabbedOverlay to the left zone,
            // leaving the right CallZoneWidth transparent for the call card.
            if (nav != OverlayNav.Hidden) {
                Box(modifier = Modifier.fillMaxSize().padding(end = CallZoneWidth)) {
                    TabbedOverlay(
                        initialTab = if (nav == OverlayNav.Dialpad) OverlayTab.Dialpad
                                     else OverlayTab.Contacts,
                        onDial     = onDial,
                        onClose    = { CallManager.hide() },
                    )
                }
            }

            // ── Right call zone ───────────────────────────────────────────────
            // Always anchored to the right edge; content fades in/out with call state.
            Box(
                modifier = Modifier
                    .width(CallZoneWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd),
            ) {
                AnimatedVisibility(
                    visible = isCallActive,
                    enter   = fadeIn(tween(150)),
                    exit    = fadeOut(tween(150)),
                ) {
                    when (val cs = cachedCall) {
                        is CallState.Incoming -> IncomingCallCard(
                            displayName = cs.displayName,
                            number      = cs.number,
                            onAccept    = { cs.call.answer(0) },
                            onDecline   = { cs.call.reject(false, null) },
                        )
                        is CallState.Active -> ActiveCallCard(
                            displayName = cs.displayName,
                            number      = cs.number,
                            onHangUp    = { cs.call.disconnect() },
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}
