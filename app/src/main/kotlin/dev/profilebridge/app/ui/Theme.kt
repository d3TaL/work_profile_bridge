package dev.profilebridge.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(primary = Color(0xFF1E5AA8), primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E), secondary = Color(0xFF00897B))
private val Dark = darkColorScheme(primary = Color(0xFFA9C7FF), primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = Color(0xFFD6E3FF), secondary = Color(0xFF4DB6AC))

/** One app-wide theme: Material You colours on Android 12+, light and dark. */
@Composable
fun BridgeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}
