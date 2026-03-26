package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Active call overlay. Shows caller, elapsed time, and an End Call button.
 * D-pad: CONFIRM or CANCEL ends the call.
 */
@Composable
fun ActiveCallScreen(
    displayName: String,
    number: String,
    onHangUp: () -> Unit,
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsed++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter,
                    Key.Back, Key.Escape -> { onHangUp(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("In Call", color = TextSecondary, fontSize = TextSizeMedium)
            Text(displayName, color = TextPrimary, fontSize = TextSizeHuge)
            Text(number, color = TextSecondary, fontSize = TextSizeLarge)
            Text(formatElapsed(elapsed), color = FocusHighlight, fontSize = TextSizeLarge)

            Spacer(Modifier.height(24.dp))

            CallButton(label = "End Call", color = RejectRed, focused = true, onClick = onHangUp)

            Text("CONFIRM / BACK = End Call", color = TextSecondary, fontSize = TextSizeSmall)
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
