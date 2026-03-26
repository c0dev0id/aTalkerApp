package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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

    // Cache the last call state so the card can still render during the exit animation.
    var cachedCall by remember { mutableStateOf<CallState?>(null) }
    LaunchedEffect(callState) {
        if (callState is CallState.Incoming || callState is CallState.Active) cachedCall = callState
    }
    val isCallActive = callState is CallState.Incoming || callState is CallState.Active

    OverlayTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Tabbed overlay (contacts / history / dialpad) ─────────────────
            when (nav) {
                OverlayNav.Contacts -> OverlayFrame(dismissible = true) {
                    TabbedOverlay(
                        initialTab = OverlayTab.Contacts,
                        onDial     = onDial,
                        onClose    = { CallManager.hide() },
                    )
                }
                OverlayNav.Dialpad -> OverlayFrame(dismissible = true) {
                    TabbedOverlay(
                        initialTab = OverlayTab.Dialpad,
                        onDial     = onDial,
                        onClose    = { CallManager.hide() },
                    )
                }
                OverlayNav.Hidden -> Unit
            }

            // ── Call card (slides in from right, out to right) ────────────────
            AnimatedVisibility(
                visible = isCallActive,
                enter = slideInHorizontally(animationSpec = tween(380)) {  it },
                exit  = slideOutHorizontally(animationSpec = tween(320)) {  it },
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
