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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.data.Contact
import kotlin.math.abs

// Filter groups. "YZ*" catches Y, Z, and anything not covered by the others
// (non-letters, empty names, numeric first chars, special characters).
private val filterGroups  = listOf("ABC", "DEF", "GHI", "JKL", "MNO", "PQR", "STU", "VWX", "YZ*")
private val whitespaceRegex = "\\s+".toRegex()

// Letters explicitly covered by groups 0–7 (A through X). Anything outside
// this set — including Y and Z — falls into the last "YZ*" group.
private val coveredLetters = ('A'..'X').toHashSet()

private fun nameForFilter(contact: Contact, byLastName: Boolean): String {
    val words = contact.displayName.trim().split(whitespaceRegex)
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
        if (filtered.isEmpty()) return@LaunchedEffect
        val idx = selectedIndex.coerceAtMost(filtered.lastIndex)
        // Only scroll if the item is not already visible; snap instantly to avoid
        // conflicting with ongoing touch-scroll gestures.
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == idx }) listState.scrollToItem(idx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onDpadKeyDown { event ->
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

            // ── Filter column + contact list ──────────────────────────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                FilterColumn(
                    filterIndex  = filterIndex,
                    panelFocused = filterFocused,
                    onSelect     = { idx -> filterIndex = idx; filterFocused = true },
                )

                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    itemsIndexed(filtered, key = { _, c -> c.id }) { index, contact ->
                        ContactRow(
                            contact    = contact,
                            byLastName = byLastName,
                            focused    = !filterFocused && index == selectedIndex,
                            onClick    = { onCall(contact) },
                        )
                    }
                }
            }
        }

    }
}

// ─── Filter column ────────────────────────────────────────────────────────────

@Composable
private fun FilterColumn(
    filterIndex: Int,
    panelFocused: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight()
            .padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        filterGroups.forEachIndexed { idx, label ->
            FilterButton(
                label    = label,
                isActive = idx == filterIndex,
                isFocused = panelFocused && idx == filterIndex,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick  = { onSelect(idx) },
            )
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    isActive: Boolean,
    isFocused: Boolean,
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
    }
}

// ─── Contact row ──────────────────────────────────────────────────────────────

private fun displayName(contact: Contact, byLastName: Boolean): String {
    if (!byLastName) return contact.displayName
    val words = contact.displayName.trim().split(whitespaceRegex)
    if (words.size < 2) return contact.displayName
    return "${words.last()}, ${words.dropLast(1).joinToString(" ")}"
}

@Composable
private fun ContactRow(contact: Contact, byLastName: Boolean, focused: Boolean, onClick: () -> Unit) {
    val rowShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .then(
                if (focused) Modifier
                    .clip(rowShape)
                    .background(RowSelected)
                    .border(1.dp, FocusHighlight.copy(alpha = 0.5f), rowShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent strip — lights up on focus; invisible for unfocused rows
        Box(
            Modifier
                .width(12.dp)
                .height(52.dp)
                .then(if (focused) Modifier.background(FocusHighlight) else Modifier),
        )
        Spacer(Modifier.width(12.dp))
        ContactAvatar(name = contact.displayName)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                displayName(contact, byLastName),
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
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun ContactAvatar(name: String) {
    val color   = remember(name) { avatarColor(name) }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
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
