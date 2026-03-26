package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp

/**
 * Full-screen incoming call overlay.
 * D-pad: LEFT = reject, RIGHT = accept, CONFIRM = accept, CANCEL/BACK = reject
 */
@Composable
fun IncomingCallScreen(
    displayName: String,
    number: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    // 0 = Reject focused, 1 = Accept focused (default to Accept so CONFIRM is safe)
    var focusedButton by remember { mutableIntStateOf(1) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { focusedButton = 0; true }
                    Key.DirectionRight -> { focusedButton = 1; true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (focusedButton == 1) onAccept() else onReject(); true
                    }
                    Key.Back, Key.Escape -> { onReject(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text("Incoming Call", color = TextSecondary, fontSize = TextSizeMedium)
            Text(displayName, color = TextPrimary, fontSize = TextSizeHuge)
            Text(number, color = TextSecondary, fontSize = TextSizeLarge)

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                CallButton(
                    label = "Reject",
                    color = RejectRed,
                    focused = focusedButton == 0,
                    onClick = onReject,
                )
                CallButton(
                    label = "Accept",
                    color = AcceptGreen,
                    focused = focusedButton == 1,
                    onClick = onAccept,
                )
            }

            Text(
                "← Reject    Accept →",
                color = TextSecondary,
                fontSize = TextSizeSmall,
            )
        }
    }
}

@Composable
internal fun CallButton(
    label: String,
    color: Color,
    focused: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (focused) FocusHighlight else Color.Transparent
    Box(
        modifier = Modifier
            .size(160.dp, 80.dp)
            .background(color, RoundedCornerShape(16.dp))
            .border(4.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = TextSizeLarge)
    }
}
