package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val CardBackground = Color(0xF0101820)
private val CardBorder = Color(0xFF2A3A4A)

/**
 * Compact incoming-call card, positioned top-start.
 * D-pad: LEFT = Decline, RIGHT = Accept (default), CONFIRM = focused button, BACK = Decline.
 */
@Composable
fun IncomingCallCard(
    displayName: String,
    number: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    var focusedButton by remember { mutableIntStateOf(1) }  // 0 = Decline, 1 = Accept
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CardShell(
        focusRequester = focusRequester,
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@CardShell false
            when (event.key) {
                Key.DirectionLeft -> { focusedButton = 0; true }
                Key.DirectionRight -> { focusedButton = 1; true }
                Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                    if (focusedButton == 1) onAccept() else onDecline(); true
                }
                Key.Back, Key.Escape -> { onDecline(); true }
                else -> false
            }
        },
    ) {
        Text("Incoming Call", color = TextSecondary, fontSize = TextSizeSmall)
        Spacer(Modifier.height(4.dp))
        Text(displayName, color = TextPrimary, fontSize = TextSizeLarge)
        Text(number, color = TextSecondary, fontSize = TextSizeMedium)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardButton("Decline", RejectRed, focusedButton == 0, onDecline)
            CardButton("Accept", AcceptGreen, focusedButton == 1, onAccept)
        }
        Spacer(Modifier.height(8.dp))
        Text("← Decline    Accept →", color = TextSecondary, fontSize = TextSizeSmall)
    }
}

/**
 * Compact active-call card, positioned top-start.
 * D-pad: CONFIRM or BACK ends the call.
 */
@Composable
fun ActiveCallCard(
    displayName: String,
    number: String,
    onHangUp: () -> Unit,
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(Unit) {
        while (true) { delay(1_000); elapsed++ }
    }

    CardShell(
        focusRequester = focusRequester,
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@CardShell false
            when (event.key) {
                Key.Enter, Key.NumPadEnter, Key.DirectionCenter,
                Key.Back, Key.Escape -> { onHangUp(); true }
                else -> false
            }
        },
    ) {
        Text("In Call", color = TextSecondary, fontSize = TextSizeSmall)
        Spacer(Modifier.height(4.dp))
        Text(displayName, color = TextPrimary, fontSize = TextSizeLarge)
        Text(number, color = TextSecondary, fontSize = TextSizeMedium)
        Text(formatElapsed(elapsed), color = FocusHighlight, fontSize = TextSizeMedium)
        Spacer(Modifier.height(20.dp))
        CardButton("End Call", RejectRed, focused = true, onClick = onHangUp)
        Spacer(Modifier.height(8.dp))
        Text("CONFIRM / BACK = End Call", color = TextSecondary, fontSize = TextSizeSmall)
    }
}

// ─── Shared card primitives ───────────────────────────────────────────────────

@Composable
private fun CardShell(
    focusRequester: FocusRequester,
    onKeyEvent: (KeyEvent) -> Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent(onKeyEvent),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp)
                .width(360.dp)
                .align(Alignment.TopStart)
                .background(CardBackground, RoundedCornerShape(20.dp))
                .border(2.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun CardButton(
    label: String,
    color: Color,
    focused: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(64.dp)
            .width(150.dp)
            .background(color, RoundedCornerShape(14.dp))
            .border(3.dp, if (focused) FocusHighlight else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = TextSizeMedium)
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
