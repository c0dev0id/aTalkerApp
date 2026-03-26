package de.codevoid.aTalkerApp.ui

import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import de.codevoid.aTalkerApp.data.CallLogEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    entries: List<CallLogEntry>,
    onDial: (String) -> Unit,
    onClose: () -> Unit,
) {
    var selectedIdx by remember { mutableIntStateOf(0) }
    val listState    = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(selectedIdx) {
        if (entries.isNotEmpty())
            listState.animateScrollToItem(selectedIdx.coerceAtMost(entries.lastIndex))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground)
            .focusRequester(focusRequester)
            .onDpadKeyDown { event ->
                when (event.key) {
                    Key.DirectionUp                       -> { if (entries.isNotEmpty()) selectedIdx = navigateUp(selectedIdx, entries.size); true }
                    Key.DirectionDown, Key.DirectionRight -> { if (entries.isNotEmpty()) selectedIdx = navigateDown(selectedIdx, entries.size); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { if (entries.isNotEmpty()) onDial(entries[selectedIdx].number); true }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
    ) {
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent calls", color = TextSecondary, fontSize = TextSizeMedium)
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(entries) { index, entry ->
                    HistoryRow(
                        entry   = entry,
                        focused = index == selectedIdx,
                        onClick = { onDial(entry.number) },
                    )
                }
            }
        }

        Text(
            "↑↓ Navigate   CONFIRM Call back   BACK Close",
            color    = TextSecondary.copy(alpha = 0.7f),
            fontSize = TextSizeSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun HistoryRow(entry: CallLogEntry, focused: Boolean, onClick: () -> Unit) {
    val accentColor = callTypeColor(entry.callType)
    val rowShape    = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(rowShape)
            .background(if (focused) RowSelected else Color.Transparent)
            .border(
                1.dp,
                if (focused) FocusHighlight.copy(alpha = 0.5f) else Color.Transparent,
                rowShape,
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent strip — color signals call direction/outcome
        Box(
            Modifier
                .width(12.dp)
                .height(52.dp)
                .background(if (focused) accentColor else accentColor.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(10.dp))

        // Call-type badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                callTypeSymbol(entry.callType),
                color      = accentColor,
                fontSize   = TextSizeMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                entry.displayName,
                color      = TextPrimary,
                fontSize   = TextSizeMedium,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            )
            if (entry.displayName != entry.number) {
                Text(entry.number, color = TextSecondary, fontSize = TextSizeSmall)
            }
        }

        Text(
            formatCallDate(entry.date),
            color    = TextSecondary,
            fontSize = TextSizeSmall,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

private fun callTypeColor(type: Int) = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> AcceptGreen
    CallLog.Calls.MISSED_TYPE   -> RejectRed
    else                        -> FocusHighlight   // incoming
}

private fun callTypeSymbol(type: Int) = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> "↑"
    CallLog.Calls.MISSED_TYPE   -> "✗"
    else                        -> "↓"   // incoming
}

private fun formatCallDate(date: Long): String {
    val now     = System.currentTimeMillis()
    val todayMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        date >= todayMs                      -> SimpleDateFormat("HH:mm",       Locale.getDefault()).format(date)
        date >= todayMs - 6 * 86_400_000L   -> SimpleDateFormat("EEE HH:mm",   Locale.getDefault()).format(date)
        else                                 -> SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(date)
    }
}
