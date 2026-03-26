package de.codevoid.aTalkerApp.ui

import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.codevoid.aTalkerApp.data.CallLogEntry
import de.codevoid.aTalkerApp.data.CallLogRepository
import de.codevoid.aTalkerApp.data.Contact
import de.codevoid.aTalkerApp.data.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OverlayTab(val label: String) {
    History("History"),
    Contacts("Contacts"),
    Dialpad("Dialpad"),
}

private val TABS = OverlayTab.values().toList()

/**
 * Three-tab overlay container: History | Contacts | Dialpad.
 *
 * Data is loaded once and survives tab switches.
 * Right-click anywhere → onClose.
 * F6 / F7 (DMD remote SWITCH_IN / SWITCH_OUT) → prev / next tab.
 */
@Composable
fun TabbedOverlay(
    initialTab: OverlayTab = OverlayTab.Contacts,
    onDial: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(initialTab) }

    // Load data once — survives tab switches.
    var contacts       by remember { mutableStateOf(emptyList<Contact>()) }
    var contactsReady  by remember { mutableStateOf(false) }
    var history        by remember { mutableStateOf(emptyList<CallLogEntry>()) }

    LaunchedEffect(Unit) {
        contacts      = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
        contactsReady = true
        history       = withContext(Dispatchers.IO) { CallLogRepository.load(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            // Tab switching via DMD remote SWITCH_IN (F6=136) / SWITCH_OUT (F7=137)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.source == InputDevice.SOURCE_KEYBOARD) return@onKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.F6 -> { selectedTab = TABS[(selectedTab.ordinal - 1 + TABS.size) % TABS.size]; true }
                    Key.F7 -> { selectedTab = TABS[(selectedTab.ordinal + 1) % TABS.size]; true }
                    else   -> false
                }
            }
            // Right-click anywhere → close (mouse secondary button = Escape)
            .pointerInput(onClose) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed) {
                            onClose()
                        }
                    }
                }
            },
    ) {
        // ── Tab bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CardSurfaceElevated),
        ) {
            TABS.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) RowSelected else Color.Transparent)
                        .clickable { selectedTab = tab },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        tab.label,
                        color      = if (isSelected) FocusHighlight else TextSecondary,
                        fontSize   = TextSizeMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                    // Active underline
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(if (isSelected) 40.dp else 0.dp)
                            .height(3.dp)
                            .background(FocusHighlight, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // ── Tab content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                OverlayTab.History ->
                    HistoryScreen(
                        entries = history,
                        onDial  = onDial,
                        onClose = onClose,
                    )
                OverlayTab.Contacts ->
                    if (!contactsReady) {
                        LoadingScreen()
                    } else {
                        ContactsScreen(
                            contacts  = contacts,
                            onCall    = { contact -> onDial(contact.phoneNumber) },
                            onDialpad = { selectedTab = OverlayTab.Dialpad },
                            onClose   = onClose,
                        )
                    }
                OverlayTab.Dialpad ->
                    DialpadScreen(
                        onDial     = onDial,
                        onContacts = { selectedTab = OverlayTab.Contacts },
                        onClose    = onClose,
                    )
            }
        }

        // ── Dismiss bar ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CardSurfaceElevated)
                .border(1.dp, RejectRed.copy(alpha = 0.25f), RoundedCornerShape(0.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Dismiss",
                color      = RejectRed,
                fontSize   = TextSizeMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(OverlayBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loading contacts…", color = TextSecondary, fontSize = TextSizeLarge)
    }
}
