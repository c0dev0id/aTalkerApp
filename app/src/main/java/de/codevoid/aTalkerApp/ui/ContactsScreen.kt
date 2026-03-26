package de.codevoid.aTalkerApp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.view.InputDevice
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.data.Contact
import kotlin.math.abs

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onDialpad: () -> Unit,
    onClose: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                // Ignore raw keyboard events — the DMD remote sends these in addition to its
                // broadcast, which we handle separately to avoid double-firing.
                if (event.nativeKeyEvent.source == InputDevice.SOURCE_KEYBOARD) return@onKeyEvent false
                if (event.type != KeyEventType.KeyDown || contacts.isEmpty()) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { selectedIndex = navigateUp(selectedIndex, contacts.size); true }
                    Key.DirectionDown, Key.DirectionRight ->
                        { selectedIndex = navigateDown(selectedIndex, contacts.size); true }
                    Key.DirectionLeft    -> { onDialpad(); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter ->
                        { onCall(contacts[selectedIndex]); true }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
    ) {
        Column {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(CardSurfaceElevated, OverlayBackground))
                    )
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Contacts",
                    color = TextPrimary,
                    fontSize = TextSizeLarge,
                    fontWeight = FontWeight.Bold,
                )
                // Ghost button — secondary action, doesn't compete with the list
                Box(
                    modifier = Modifier
                        .border(1.5.dp, FocusHighlight.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onDialpad)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("Dialpad", color = FocusHighlight, fontSize = TextSizeSmall,
                        fontWeight = FontWeight.Medium)
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(contacts) { index, contact ->
                    ContactRow(
                        contact  = contact,
                        focused  = index == selectedIndex,
                        onClick  = { onCall(contact) },
                    )
                }
            }
        }

        // Hint bar at bottom
        Text(
            "↑↓ Navigate   CONFIRM Call   ← Dialpad   BACK Close",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = TextSizeSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, focused: Boolean, onClick: () -> Unit) {
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(rowShape)
            .background(if (focused) RowSelected else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (focused) FocusHighlight.copy(alpha = 0.5f) else Color.Transparent,
                shape = rowShape,
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent strip — lights up on focus
        Box(
            Modifier
                .width(4.dp)
                .height(72.dp)
                .background(if (focused) FocusHighlight else Color.Transparent),
        )
        Spacer(Modifier.width(18.dp))
        ContactAvatar(name = contact.displayName)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
        ) {
            Text(
                contact.displayName,
                color = TextPrimary,
                fontSize = TextSizeMedium,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                buildPhoneLabel(contact),
                color = TextSecondary,
                fontSize = TextSizeSmall,
            )
        }
        Spacer(Modifier.width(18.dp))
    }
}

@Composable
private fun ContactAvatar(name: String) {
    val color = remember(name) { avatarColor(name) }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, fontSize = TextSizeMedium, fontWeight = FontWeight.Bold)
    }
}

private fun avatarColor(name: String): Color {
    val palette = listOf(
        Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF06B6D4),
        Color(0xFFF97316), Color(0xFFEC4899),
    )
    return palette[abs(name.hashCode()) % palette.size]
}

private fun buildPhoneLabel(contact: Contact): String =
    if (contact.phoneType.isBlank()) contact.phoneNumber
    else "${contact.phoneType}  ${contact.phoneNumber}"

// ─── D-pad navigation ─────────────────────────────────────────────────────────

fun navigateUp(current: Int, size: Int): Int = (current - 1 + size) % size
fun navigateDown(current: Int, size: Int): Int = (current + 1) % size
