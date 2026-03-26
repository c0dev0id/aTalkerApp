package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.CallManager
import de.codevoid.aTalkerApp.CallState
import de.codevoid.aTalkerApp.OverlayNav

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

            // ── Contacts / history / dialpad panel — centered ─────────────────
            if (nav != OverlayNav.Hidden) {
                // Transparent backdrop — tap outside the panel to dismiss
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { CallManager.hide() },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {},
                ) {
                    TabbedOverlay(
                        initialTab = if (nav == OverlayNav.Dialpad) OverlayTab.Dialpad
                                     else OverlayTab.Contacts,
                        onDial     = onDial,
                        onClose    = { CallManager.hide() },
                    )
                }
            }

            // ── Call card — top-right, independent of the panel ───────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .width(300.dp),
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
