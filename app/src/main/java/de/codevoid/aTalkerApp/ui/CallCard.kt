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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val cardShape = RoundedCornerShape(20.dp)
private val cardGradient = Brush.verticalGradient(listOf(CardSurfaceElevated, CardSurface))

/**
 * Incoming-call card. Slides in from the left with an amber accent strip
 * that pulses while ringing.
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
        accentColor   = IncomingAmber,
        pulseAlpha    = pulseAlpha,
        focusRequester = focusRequester,
        onKeyEvent    = { event ->
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
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardButton("Decline", RejectRed,   focused = focusedButton == 0, onClick = onDecline)
            CardButton("Accept",  AcceptGreen, focused = focusedButton == 1, onClick = onAccept)
        }
        Spacer(Modifier.height(6.dp))
        Text("← Decline    Accept →", color = TextSecondary, fontSize = TextSizeSmall)
    }
}

/**
 * Active-call card. Same position; accent strip turns green.
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
        pulseAlpha     = 1.0f,              // steady — call is connected
        focusRequester = focusRequester,
        onKeyEvent     = { event ->
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
        Spacer(Modifier.height(20.dp))
        CardButton("End Call", RejectRed, focused = true, onClick = onHangUp)
        Spacer(Modifier.height(6.dp))
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
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp)
                .width(370.dp)
                .align(Alignment.TopStart)
                .shadow(
                    elevation    = 24.dp,
                    shape        = cardShape,
                    ambientColor = accentColor.copy(alpha = 0.25f),
                    spotColor    = accentColor.copy(alpha = 0.40f),
                )
                .clip(cardShape)
                .background(cardGradient)
                .border(1.5.dp, accentColor.copy(alpha = pulseAlpha * 0.55f), cardShape),
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Left accent strip — color communicates state at a glance
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor.copy(alpha = pulseAlpha)),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, top = 22.dp, end = 24.dp, bottom = 22.dp),
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
    onClick: () -> Unit,
) {
    val btnShape = RoundedCornerShape(14.dp)
    // Subtle top-highlight gradient — implies pressability without skeuomorphism
    val gradient = Brush.verticalGradient(listOf(lerp(color, Color.White, 0.18f), color))
    Box(
        modifier = Modifier
            .height(64.dp)
            .width(152.dp)
            .shadow(
                elevation    = if (focused) 10.dp else 4.dp,
                shape        = btnShape,
                spotColor    = color.copy(alpha = 0.55f),
                ambientColor = color.copy(alpha = 0.25f),
            )
            .clip(btnShape)
            .background(gradient)
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
