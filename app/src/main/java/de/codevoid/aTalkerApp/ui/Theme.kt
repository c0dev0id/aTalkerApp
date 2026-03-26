package de.codevoid.aTalkerApp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// Overlay uses a dark semi-transparent background so the nav app remains visible underneath
val OverlayBackground = Color(0xE6000000)   // 90 % black
val FocusHighlight = Color(0xFF00BFFF)      // deep sky blue for d-pad focus ring
val AcceptGreen = Color(0xFF4CAF50)
val RejectRed = Color(0xFFF44336)
val TextPrimary = Color.White
val TextSecondary = Color(0xFFBBBBBB)

// All text sizes are intentionally large for glanceable reading on a tablet at arm's length
val TextSizeHuge = 48.sp
val TextSizeLarge = 36.sp
val TextSizeMedium = 28.sp
val TextSizeSmall = 22.sp

private val Colors = darkColorScheme(
    primary = FocusHighlight,
    background = OverlayBackground,
    surface = Color(0xFF1A1A1A),
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun OverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
