package app.gallager.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GallagerBackground = Color(0xFF020617)
val GallagerSurface = Color(0xFF0F172A)
val GallagerSurfaceRaised = Color(0xFF1E293B)
val GallagerBorder = Color(0xFF334155)
val GallagerAccent = Color(0xFF22C55E)
val GallagerText = Color(0xFFF8FAFC)
val GallagerMuted = Color(0xFF94A3B8)
val GallagerDanger = Color(0xFFEF4444)
val GallagerWarning = Color(0xFFF59E0B)

private val colors = darkColorScheme(
    primary = GallagerAccent,
    onPrimary = Color(0xFF052E16),
    secondary = Color(0xFF38BDF8),
    background = GallagerBackground,
    onBackground = GallagerText,
    surface = GallagerSurface,
    onSurface = GallagerText,
    surfaceVariant = GallagerSurfaceRaised,
    onSurfaceVariant = GallagerMuted,
    outline = GallagerBorder,
    error = GallagerDanger,
)

@Composable
fun GallagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
