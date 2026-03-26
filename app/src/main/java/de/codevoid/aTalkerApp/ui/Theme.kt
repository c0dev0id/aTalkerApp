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

// ── Text sizes — tuned for 1280×800 landscape (≈ 853×533 dp at hdpi) ────────
val TextSizeHuge   = 36.sp
val TextSizeLarge  = 26.sp
val TextSizeMedium = 20.sp
val TextSizeSmall  = 16.sp
val TextSizeTiny   = 12.sp

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
