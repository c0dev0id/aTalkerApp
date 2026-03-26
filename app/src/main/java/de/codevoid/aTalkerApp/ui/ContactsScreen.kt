package de.codevoid.aTalkerApp.ui

import android.view.InputDevice
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.data.Contact
import kotlin.math.abs

// Filter groups. "YZ*" catches Y, Z, and anything not covered by the others
// (non-letters, empty names, numeric first chars, special characters).
private val filterGroups = listOf("ABC", "DEF", "GHI", "JKL", "MNO", "PQR", "STU", "VWX", "YZ*")

// Letters explicitly covered by groups 0–7 (A through X). Anything outside
// this set — including Y and Z — falls into the last "YZ*" group.
private val coveredLetters = ('A'..'X').toHashSet()

private fun nameForFilter(contact: Contact, byLastName: Boolean): String {
    val words = contact.displayName.trim().split("\\s+".toRegex())
    return if (byLastName) words.lastOrNull() ?: "" else words.firstOrNull() ?: ""
}

private fun matchesFilterGroup(contact: Contact, groupIdx: Int, byLastName: Boolean): Boolean {
    val name = nameForFilter(contact, byLastName)
    val ch   = name.firstOrNull()?.uppercaseChar()
    return if (groupIdx == filterGroups.lastIndex) {        // "YZ*"
        ch == null || !ch.isLetter() || ch !in coveredLetters
    } else {
        ch != null && ch in filterGroups[groupIdx]
    }
}

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onDialpad: () -> Unit,
    onClose: () -> Unit,
) {
    var selectedIndex  by remember { mutableIntStateOf(0) }
    var filterFocused  by remember { mutableStateOf(false) }
    var filterIndex    by remember { mutableIntStateOf(0) }
    var byLastName     by remember { mutableStateOf(false) }

    val filtered = remember(contacts, filterIndex, byLastName) {
        contacts.filter { matchesFilterGroup(it, filterIndex, byLastName) }
    }

    // Reset list position whenever the filter changes.
    LaunchedEffect(filterIndex, byLastName) { selectedIndex = 0 }

    val listState     = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(selectedIndex) {
        if (filtered.isNotEmpty())
            listState.animateScrollToItem(selectedIndex.coerceAtMost(filtered.lastIndex))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.source == InputDevice.SOURCE_KEYBOARD) return@onKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (filterFocused) {
                    when (event.key) {
                        Key.DirectionUp    -> { filterIndex = (filterIndex - 1 + filterGroups.size) % filterGroups.size; true }
                        Key.DirectionDown  -> { filterIndex = (filterIndex + 1) % filterGroups.size; true }
                        Key.DirectionRight -> { filterFocused = false; true }
                        Key.DirectionLeft  -> { onDialpad(); true }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { byLastName = !byLastName; true }
                        Key.Back, Key.Escape -> { onClose(); true }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.DirectionUp                       -> { if (filtered.isNotEmpty()) selectedIndex = navigateUp(selectedIndex, filtered.size); true }
                        Key.DirectionDown, Key.DirectionRight -> { if (filtered.isNotEmpty()) selectedIndex = navigateDown(selectedIndex, filtered.size); true }
                        Key.DirectionLeft                     -> { filterFocused = true; true }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { if (filtered.isNotEmpty()) onCall(filtered[selectedIndex]); true }
                        Key.Back, Key.Escape -> { onClose(); true }
                        else -> false
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                Box(
                    modifier = Modifier
                        .border(1.5.dp, FocusHighlight.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onDialpad)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Dialpad",
                        color = FocusHighlight,
                        fontSize = TextSizeSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ── Filter column + contact list ──────────────────────────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                FilterColumn(
                    filterIndex  = filterIndex,
                    byLastName   = byLastName,
                    panelFocused = filterFocused,
                    onSelect     = { idx -> filterIndex = idx; filterFocused = true },
                )

                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    itemsIndexed(filtered) { index, contact ->
                        ContactRow(
                            contact = contact,
                            focused = !filterFocused && index == selectedIndex,
                            onClick = { onCall(contact) },
                        )
                    }
                }
            }
        }

        // ── Hint bar ──────────────────────────────────────────────────────────
        Text(
            if (filterFocused)
                "↑↓ Filter group   CONFIRM ${if (byLastName) "→ First name" else "→ Last name"}   → Contacts   ← Dialpad   BACK Close"
            else
                "↑↓ Navigate   CONFIRM Call   ← Filter   BACK Close",
            color    = TextSecondary.copy(alpha = 0.7f),
            fontSize = TextSizeSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}

// ─── Filter column ────────────────────────────────────────────────────────────

@Composable
private fun FilterColumn(
    filterIndex: Int,
    byLastName: Boolean,
    panelFocused: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 48.dp), // bottom pad clears hint bar
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        filterGroups.forEachIndexed { idx, label ->
            FilterButton(
                label        = label,
                isActive     = idx == filterIndex,
                isFocused    = panelFocused && idx == filterIndex,
                byLastName   = byLastName,
                modifier     = Modifier.weight(1f).fillMaxWidth(),
                onClick      = { onSelect(idx) },
            )
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    isActive: Boolean,
    isFocused: Boolean,
    byLastName: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    isFocused -> RowSelected
                    isActive  -> CardSurfaceElevated
                    else      -> Color.Transparent
                }
            )
            .border(
                1.5.dp,
                when {
                    isFocused -> FocusHighlight
                    isActive  -> FocusHighlight.copy(alpha = 0.45f)
                    else      -> FocusHighlight.copy(alpha = 0.12f)
                },
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = when {
                    isFocused -> FocusHighlight
                    isActive  -> FocusHighlight.copy(alpha = 0.85f)
                    else      -> TextSecondary.copy(alpha = 0.45f)
                },
                fontSize   = TextSizeSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            )
            // Show which name field is active for filtering
            if (isActive) {
                Text(
                    if (byLastName) "LN" else "FN",
                    color      = if (byLastName) IncomingAmber else TextSecondary.copy(alpha = 0.55f),
                    fontSize   = TextSizeTiny,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ─── Contact row ──────────────────────────────────────────────────────────────

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
                color      = TextPrimary,
                fontSize   = TextSizeMedium,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                buildPhoneLabel(contact),
                color    = TextSecondary,
                fontSize = TextSizeSmall,
            )
        }
        Spacer(Modifier.width(18.dp))
    }
}

@Composable
private fun ContactAvatar(name: String) {
    val color   = remember(name) { avatarColor(name) }
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

fun navigateUp(current: Int, size: Int): Int   = (current - 1 + size) % size
fun navigateDown(current: Int, size: Int): Int = (current + 1) % size
