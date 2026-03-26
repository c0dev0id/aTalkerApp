package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import android.view.InputDevice
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val cardShape = RoundedCornerShape(12.dp)

/**
 * Incoming-call card. Amber accent strip pulses while ringing.
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

    // Amber pulse — signals urgency while ringing
    val pulse = rememberInfiniteTransition(label = "ring")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    CardShell(
        accentColor    = IncomingAmber,
        pulseAlpha     = pulseAlpha,
        focusRequester = focusRequester,
        onKeyEvent     = { event ->
            if (event.nativeKeyEvent.source == InputDevice.SOURCE_KEYBOARD) return@CardShell false
            if (event.type != KeyEventType.KeyDown) return@CardShell false
            when (event.key) {
                Key.DirectionLeft  -> { focusedButton = 0; true }
                Key.DirectionRight -> { focusedButton = 1; true }
                Key.Enter, Key.NumPadEnter, Key.DirectionCenter ->
                    { if (focusedButton == 1) onAccept() else onDecline(); true }
                Key.Back, Key.Escape -> { onDecline(); true }
                else -> false
            }
        },
    ) {
        Text("Incoming Call", color = IncomingAmber, fontSize = TextSizeSmall,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(displayName, color = TextPrimary, fontSize = TextSizeLarge,
            fontWeight = FontWeight.Bold)
        Text(number, color = TextSecondary, fontSize = TextSizeMedium)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardButton("Decline", RejectRed,   focused = focusedButton == 0,
                modifier = Modifier.weight(1f), onClick = onDecline)
            CardButton("Accept",  AcceptGreen, focused = focusedButton == 1,
                modifier = Modifier.weight(1f), onClick = onAccept)
        }
        Spacer(Modifier.height(4.dp))
        Text("← Decline    Accept →", color = TextSecondary, fontSize = TextSizeSmall)
    }
}

/**
 * Active-call card. Accent strip turns green, steady.
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
    LaunchedEffect(Unit) { while (true) { delay(1_000); elapsed++ } }

    CardShell(
        accentColor    = AcceptGreen,
        pulseAlpha     = 1.0f,
        focusRequester = focusRequester,
        onKeyEvent     = { event ->
            if (event.nativeKeyEvent.source == InputDevice.SOURCE_KEYBOARD) return@CardShell false
            if (event.type != KeyEventType.KeyDown) return@CardShell false
            when (event.key) {
                Key.Enter, Key.NumPadEnter, Key.DirectionCenter,
                Key.Back, Key.Escape -> { onHangUp(); true }
                else -> false
            }
        },
    ) {
        Text("In Call", color = AcceptGreen, fontSize = TextSizeSmall,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(displayName, color = TextPrimary, fontSize = TextSizeLarge,
            fontWeight = FontWeight.Bold)
        Text(number, color = TextSecondary, fontSize = TextSizeMedium)
        Text(formatElapsed(elapsed), color = FocusHighlight, fontSize = TextSizeMedium,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        CardButton("End Call", RejectRed, focused = true,
            modifier = Modifier.fillMaxWidth(), onClick = onHangUp)
        Spacer(Modifier.height(4.dp))
        Text("CONFIRM / BACK = End Call", color = TextSecondary, fontSize = TextSizeSmall)
    }
}

// ─── Shared primitives ────────────────────────────────────────────────────────

@Composable
private fun CardShell(
    accentColor: Color,
    pulseAlpha: Float,
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
        // Card fills the zone width; top-aligned with a small inset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                .clip(cardShape)
                .background(CardSurface)
                .border(1.5.dp, accentColor.copy(alpha = pulseAlpha * 0.6f), cardShape),
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Accent strip — communicates state at a glance, matches history row style
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor.copy(alpha = pulseAlpha)),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun CardButton(
    label: String,
    color: Color,
    focused: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val btnShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(btnShape)
            .background(color.copy(alpha = if (focused) 1f else 0.7f))
            .border(2.dp, if (focused) FocusHighlight else Color.Transparent, btnShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = TextSizeMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
