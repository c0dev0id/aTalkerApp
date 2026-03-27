package de.codevoid.aTalkerApp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.data.ContactsRepository
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val cardShape  = RoundedCornerShape(16.dp)
private val btnShape   = RoundedCornerShape(10.dp)
// Subtle top-lit depth gradient — matches aR2Launcher FocusableButton style
private val depthGradient = Brush.verticalGradient(
    0f    to Color.White.copy(alpha = 0.11f),
    0.45f to Color.Transparent,
    1f    to Color.Black.copy(alpha = 0.08f),
)

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

    // Resolve caller name: contacts > CallerID > "Unknown"
    val context = LocalContext.current
    var contactName by remember(number) { mutableStateOf<String?>(null) }
    LaunchedEffect(number) { contactName = ContactsRepository.lookupName(context, number) }
    val shownName = contactName
        ?: displayName.takeIf { it != number }
        ?: "Unknown"

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
        accentColor = IncomingAmber,
        pulseAlpha  = pulseAlpha,
        onKeyEvent  = { event ->
            when (event.key) {
                Key.DirectionLeft  -> { focusedButton = 0; true }
                Key.DirectionRight -> { focusedButton = 1; true }
                Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { onAccept(); true }
                Key.Back, Key.Escape -> { onDecline(); true }
                else -> false
            }
        },
    ) {
        Text("Incoming Call", color = IncomingAmber, fontSize = TextSizeSmall,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(shownName, color = TextPrimary, fontSize = TextSizeLarge,
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
    LaunchedEffect(Unit) { while (true) { delay(1_000); elapsed++ } }

    CardShell(
        accentColor = AcceptGreen,
        pulseAlpha  = 1.0f,
        onKeyEvent  = { event ->
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
    }
}

// ─── Shared primitives ────────────────────────────────────────────────────────

@Composable
private fun CardShell(
    accentColor: Color,
    pulseAlpha: Float,
    onKeyEvent: (KeyEvent) -> Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                onKeyEvent(event)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(CardSurface)
                .border(1.5.dp, accentColor.copy(alpha = pulseAlpha * 0.6f), cardShape),
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // 12dp accent strip — matches aR2Launcher left-bar style
                Box(
                    Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(accentColor.copy(alpha = pulseAlpha)),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
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
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(btnShape)
            .background(color.copy(alpha = if (focused) 1f else 0.7f))
            .background(depthGradient)   // top-lit highlight
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
