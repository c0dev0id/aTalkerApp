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

private val dialKeys = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("*", "0", "#"),
)
private const val ACTION_ROW = 4   // Contacts | Backspace
private const val CALL_ROW   = 5   // Call (single dominant button)

// Subtle blue-dark key gradient — gives keys physical presence without loud color
private val keyGradient = Brush.verticalGradient(listOf(Color(0xFF1C2E42), Color(0xFF0D1825)))
// Active call gradient (enabled)
private fun callGradient(enabled: Boolean) = if (enabled)
    Brush.verticalGradient(listOf(lerp(AcceptGreen, Color.White, 0.15f), AcceptGreen))
else
    Brush.verticalGradient(listOf(Color(0xFF1A3528), Color(0xFF0D1F19)))

/**
 * Full-screen dialpad.
 * D-pad: arrows navigate the grid; CONFIRM activates; BACK closes.
 * Action row (row 4): Contacts | Backspace.
 * Call row (row 5): dominant full-width Call button.
 */
@Composable
fun DialpadScreen(onDial: (String) -> Unit, onContacts: () -> Unit, onClose: () -> Unit) {
    var digits by remember { mutableStateOf("") }
    var selRow  by remember { mutableIntStateOf(3) }   // default: "0" key
    var selCol  by remember { mutableIntStateOf(1) }
    val focusRequester = remember { FocusRequester() }

    // Blinking cursor
    var cursorOn by remember { mutableStateOf(true) }
    LaunchedEffect(digits) {
        cursorOn = true
        while (true) { delay(530); cursorOn = !cursorOn }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        if (selRow > 0) {
                            selRow--
                            // Entering ACTION_ROW from CALL_ROW: default to Backspace (col 1)
                            if (selRow == ACTION_ROW && selCol > 1) selCol = 1
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        if (selRow < CALL_ROW) {
                            selRow++
                            when (selRow) {
                                ACTION_ROW -> selCol = selCol.coerceAtMost(1)
                                CALL_ROW   -> selCol = 0
                            }
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        val cols = when (selRow) { ACTION_ROW -> 2; CALL_ROW -> 1; else -> 3 }
                        selCol = (selCol - 1 + cols) % cols
                        true
                    }
                    Key.DirectionRight -> {
                        val cols = when (selRow) { ACTION_ROW -> 2; CALL_ROW -> 1; else -> 3 }
                        selCol = (selCol + 1) % cols
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        handleConfirm(selRow, selCol, digits,
                            onDigit     = { digits += it },
                            onBackspace = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                            onCall      = { if (digits.isNotEmpty()) onDial(digits) },
                            onContacts  = onContacts,
                        )
                        true
                    }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Number display with blinking cursor ───────────────────────────
            val displayText = when {
                digits.isEmpty() -> "Enter number"
                else             -> digits + if (cursorOn) "|" else " "
            }
            Box(
                modifier = Modifier
                    .width(384.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurfaceElevated)
                    .border(1.dp, FocusHighlight.copy(0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text  = displayText,
                    color = if (digits.isEmpty()) TextSecondary else TextPrimary,
                    fontSize   = TextSizeLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Digit rows ────────────────────────────────────────────────────
            dialKeys.forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { colIdx, key ->
                        DialKey(
                            label   = key,
                            focused = selRow == rowIdx && selCol == colIdx,
                            onClick = { digits += key },
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // ── Action row: Contacts | Backspace ─────────────────────────────
            Row(
                modifier = Modifier.width(384.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionKey(
                    label   = "Contacts",
                    focused = selRow == ACTION_ROW && selCol == 0,
                    onClick = onContacts,
                    modifier = Modifier.weight(1f).height(68.dp),
                )
                ActionKey(
                    label   = "⌫",
                    focused = selRow == ACTION_ROW && selCol == 1,
                    onClick = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                    modifier = Modifier.weight(1f).height(68.dp),
                )
            }

            // ── Call button: dominant, full width ─────────────────────────────
            val callEnabled = digits.isNotEmpty()
            val callShape   = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .width(384.dp)
                    .height(90.dp)
                    .shadow(
                        elevation    = if (selRow == CALL_ROW) 16.dp else 6.dp,
                        shape        = callShape,
                        spotColor    = AcceptGreen.copy(if (callEnabled) 0.5f else 0.15f),
                        ambientColor = AcceptGreen.copy(if (callEnabled) 0.2f else 0.05f),
                    )
                    .clip(callShape)
                    .background(callGradient(callEnabled))
                    .border(
                        2.dp,
                        if (selRow == CALL_ROW) FocusHighlight else Color.Transparent,
                        callShape,
                    )
                    .clickable(enabled = callEnabled) { onDial(digits) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Call",
                    color      = if (callEnabled) Color.White else TextSecondary,
                    fontSize   = TextSizeLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── Key composables ──────────────────────────────────────────────────────────

@Composable
private fun DialKey(label: String, focused: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(120.dp, 80.dp)
            .clip(shape)
            .run { if (focused) background(RowSelected) else background(keyGradient) }
            .border(
                width = 1.5.dp,
                color = if (focused) FocusHighlight else Color(0xFF1A2D3E),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color      = TextPrimary,
            fontSize   = TextSizeMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActionKey(
    label: String,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (focused) FocusHighlight else FocusHighlight.copy(alpha = 0.25f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color      = if (focused) FocusHighlight else TextSecondary,
            fontSize   = TextSizeMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─── Navigation logic ─────────────────────────────────────────────────────────

private fun handleConfirm(
    row: Int, col: Int, digits: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    onContacts: () -> Unit,
) = when {
    row < dialKeys.size -> onDigit(dialKeys[row][col])
    row == ACTION_ROW   -> when (col) { 0 -> onContacts(); 1 -> onBackspace(); else -> Unit }
    row == CALL_ROW     -> onCall()
    else                -> Unit
}
