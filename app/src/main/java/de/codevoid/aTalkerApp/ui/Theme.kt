package de.codevoid.aTalkerApp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// ── Overlay backdrop ──────────────────────────────────────────────────────────
val OverlayBackground = Color(0xE6000000)       // 90 % black

// ── Card surfaces ─────────────────────────────────────────────────────────────
val CardSurface         = Color(0xFF0D1117)      // deepest dark
val CardSurfaceElevated = Color(0xFF162032)      // raised / header tint

// ── State accent colors ───────────────────────────────────────────────────────
val IncomingAmber = Color(0xFFF59E0B)            // "attention" — ringing
val AcceptGreen   = Color(0xFF22C55E)            // accept / active call
val RejectRed     = Color(0xFFEF4444)            // decline / end call

// ── Interactive ───────────────────────────────────────────────────────────────
val FocusHighlight = Color(0xFF38BDF8)           // D-pad focus ring / accent
val FocusGlow      = Color(0x1A38BDF8)           // 10 % blue fill under focused rows

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color.White
val TextSecondary = Color(0xFF94A3B8)            // slate-400: subdued but readable

// ── Rows ─────────────────────────────────────────────────────────────────────
val RowSelected = Color(0xFF0F1E2F)             // focused contact-row background

// ── Text sizes (large — glanceable at arm's length) ──────────────────────────
val TextSizeHuge   = 48.sp
val TextSizeLarge  = 36.sp
val TextSizeMedium = 28.sp
val TextSizeSmall  = 22.sp
val TextSizeTiny   = 14.sp

private val Colors = darkColorScheme(
    primary      = FocusHighlight,
    background   = OverlayBackground,
    surface      = CardSurface,
    onPrimary    = Color.Black,
    onBackground = TextPrimary,
    onSurface    = TextPrimary,
)

@Composable
fun OverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
