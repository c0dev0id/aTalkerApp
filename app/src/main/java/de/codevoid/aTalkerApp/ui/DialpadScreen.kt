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

private val dialKeys = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("*", "0", "#"),
)
private const val ACTION_ROW = 4   // row index for Contacts / Backspace / Call

/**
 * Full-screen dialpad overlay.
 * D-pad: arrow keys move through the grid; CONFIRM activates the focused key.
 * Touch: all keys are also tappable.
 */
@Composable
fun DialpadScreen(onDial: (String) -> Unit, onContacts: () -> Unit, onClose: () -> Unit) {
    var digits by remember { mutableStateOf("") }
    var selRow by remember { mutableIntStateOf(3) }   // default focus: "0" key
    var selCol by remember { mutableIntStateOf(1) }
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
                    Key.DirectionUp -> { if (selRow > 0) selRow--; true }
                    Key.DirectionDown -> { if (selRow < ACTION_ROW) selRow++; true }
                    Key.DirectionLeft -> { selCol = (selCol - 1 + 3) % 3; true }
                    Key.DirectionRight -> { selCol = (selCol + 1) % 3; true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        handleConfirm(selRow, selCol, digits,
                            onDigit = { digits += it },
                            onBackspace = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                            onCall = { if (digits.isNotEmpty()) onDial(digits) },
                            onContacts = onContacts,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Number display
            Text(
                text = if (digits.isEmpty()) "Enter number" else digits,
                color = if (digits.isEmpty()) TextSecondary else TextPrimary,
                fontSize = TextSizeLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Digit rows (1–9, *, 0, #)
            dialKeys.forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { colIdx, key ->
                        DialKey(
                            label = key,
                            focused = selRow == rowIdx && selCol == colIdx,
                            onClick = { digits += key },
                        )
                    }
                }
            }

            // Action row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialKey(
                    label = "Contacts",
                    focused = selRow == ACTION_ROW && selCol == 0,
                    onClick = onContacts,
                )
                DialKey(
                    label = "⌫",
                    focused = selRow == ACTION_ROW && selCol == 1,
                    onClick = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                )
                DialKey(
                    label = "Call",
                    focused = selRow == ACTION_ROW && selCol == 2,
                    color = if (digits.isNotEmpty()) AcceptGreen else Color(0xFF1C4A2A),
                    onClick = { if (digits.isNotEmpty()) onDial(digits) },
                )
            }
        }
    }
}

@Composable
private fun DialKey(
    label: String,
    focused: Boolean,
    onClick: () -> Unit,
    color: Color = Color(0xFF1C2A3A),
) {
    Box(
        modifier = Modifier
            .size(120.dp, 80.dp)
            .background(color, RoundedCornerShape(12.dp))
            .border(3.dp, if (focused) FocusHighlight else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextPrimary, fontSize = TextSizeMedium)
    }
}

private fun handleConfirm(
    row: Int, col: Int, digits: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    onContacts: () -> Unit,
) {
    if (row < dialKeys.size) {
        onDigit(dialKeys[row][col])
    } else {
        when (col) {
            0 -> onContacts()
            1 -> onBackspace()
            2 -> onCall()
        }
    }
}
