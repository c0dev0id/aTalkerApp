package de.codevoid.aTalkerApp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.codevoid.aTalkerApp.R

// ── Apotek font (shared with aR2Launcher) ─────────────────────────────────────
private val ApotekFamily = FontFamily(
    Font(R.font.apotek_comp_medium, FontWeight.Normal),
    Font(R.font.apotek_bold,        FontWeight.Bold),
)

// ── Overlay backdrop ──────────────────────────────────────────────────────────
val OverlayBackground = Color(0xE6000000)       // 90 % black

// ── Card surfaces (aR2Launcher surface scale) ─────────────────────────────────
val CardSurface         = Color(0xFF1A1A1A)      // deepest dark
val CardSurfaceElevated = Color(0xFF2B2B2B)      // raised / header tint
val CardSurfaceCard     = Color(0xFF3C3C3C)      // dialog / floating card

// ── State accent colors ───────────────────────────────────────────────────────
val IncomingAmber = Color(0xFFF59E0B)            // amber — ringing urgency
val AcceptGreen   = Color(0xFF22C55E)            // accept / active call
val RejectRed     = Color(0xFFEF4444)            // decline / end call

// ── Interactive (aR2Launcher orange) ─────────────────────────────────────────
val FocusHighlight = Color(0xFFF57C00)           // orange — focus / active accent
val FocusGlow      = Color(0x1AF57C00)           // 10 % orange fill

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color.White
val TextSecondary = Color(0xFFB0B0B0)            // light gray

// ── Rows ─────────────────────────────────────────────────────────────────────
val RowSelected = Color(0xFF2B2B2B)              // surface_dark

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

private val defaultTypography = Typography()
private val AppTypography = Typography(
    displayLarge   = defaultTypography.displayLarge.copy(fontFamily   = ApotekFamily),
    displayMedium  = defaultTypography.displayMedium.copy(fontFamily  = ApotekFamily),
    displaySmall   = defaultTypography.displaySmall.copy(fontFamily   = ApotekFamily),
    headlineLarge  = defaultTypography.headlineLarge.copy(fontFamily  = ApotekFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = ApotekFamily),
    headlineSmall  = defaultTypography.headlineSmall.copy(fontFamily  = ApotekFamily),
    titleLarge     = defaultTypography.titleLarge.copy(fontFamily     = ApotekFamily),
    titleMedium    = defaultTypography.titleMedium.copy(fontFamily    = ApotekFamily),
    titleSmall     = defaultTypography.titleSmall.copy(fontFamily     = ApotekFamily),
    bodyLarge      = defaultTypography.bodyLarge.copy(fontFamily      = ApotekFamily),
    bodyMedium     = defaultTypography.bodyMedium.copy(fontFamily     = ApotekFamily),
    bodySmall      = defaultTypography.bodySmall.copy(fontFamily      = ApotekFamily),
    labelLarge     = defaultTypography.labelLarge.copy(fontFamily     = ApotekFamily),
    labelMedium    = defaultTypography.labelMedium.copy(fontFamily    = ApotekFamily),
    labelSmall     = defaultTypography.labelSmall.copy(fontFamily     = ApotekFamily),
)

@Composable
fun OverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
