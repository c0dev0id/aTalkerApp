package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import de.codevoid.aTalkerApp.data.Contact

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Scroll to keep selected item visible
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || contacts.isEmpty()) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.DirectionLeft -> {
                        selectedIndex = navigateUp(selectedIndex, contacts.size)
                        true
                    }
                    Key.DirectionDown, Key.DirectionRight -> {
                        selectedIndex = navigateDown(selectedIndex, contacts.size)
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        onCall(contacts[selectedIndex]); true
                    }
                    Key.Back, Key.Escape -> { onDismiss(); true }
                    else -> false
                }
            },
    ) {
        Column {
            Text(
                "Contacts",
                color = TextPrimary,
                fontSize = TextSizeLarge,
                modifier = Modifier.padding(24.dp),
            )

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(contacts) { index, contact ->
                    ContactRow(
                        contact = contact,
                        focused = index == selectedIndex,
                        onClick = { onCall(contact) },
                    )
                }
            }
        }

        Text(
            "↑↓ Navigate   CONFIRM Call   BACK Close",
            color = TextSecondary,
            fontSize = TextSizeSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, focused: Boolean, onClick: () -> Unit) {
    val bg = if (focused) Color(0xFF1C3A5A) else Color.Transparent
    val border = if (focused) FocusHighlight else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .border(3.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, color = TextPrimary, fontSize = TextSizeMedium)
            Text(
                "${contact.phoneType}  ${contact.phoneNumber}",
                color = TextSecondary,
                fontSize = TextSizeSmall,
            )
        }
    }
}

// ─── D-pad navigation logic ──────────────────────────────────────────────────

/**
 * Move selection up by one with wrap-around.
 * Pressing UP at index 0 jumps to the last contact — useful for quickly
 * reaching the bottom of a short favorites list without scrolling all the way down.
 */
fun navigateUp(current: Int, size: Int): Int =
    (current - 1 + size) % size

/**
 * Move selection down by one with wrap-around.
 * Pressing DOWN at the last contact jumps back to index 0.
 */
fun navigateDown(current: Int, size: Int): Int =
    (current + 1) % size
