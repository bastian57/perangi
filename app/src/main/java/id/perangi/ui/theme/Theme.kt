package id.perangi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedPrimary = Color(0xFFC62828)
private val RedDark = Color(0xFF8E0000)
private val AmberAccent = Color(0xFFFF8F00)

@Composable
fun PerangiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RedPrimary,
            onPrimary = Color.White,
            secondary = RedDark,
            tertiary = AmberAccent
        ),
        content = content
    )
}
