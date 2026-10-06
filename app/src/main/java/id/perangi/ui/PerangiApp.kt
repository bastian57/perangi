package id.perangi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import id.perangi.ui.theme.PerangiTheme

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Beranda", Icons.Filled.Dashboard),
    Kalkulator("Kalkulator", Icons.Filled.Calculate),
    CekPinjol("Cek Pinjol", Icons.Filled.VerifiedUser),
}

@Composable
fun PerangiApp() {
    PerangiTheme {
        var screen by remember { mutableStateOf(Screen.Dashboard) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Screen.values().forEach { s ->
                        NavigationBarItem(
                            selected = screen == s,
                            onClick = { screen = s },
                            icon = { Icon(s.icon, contentDescription = s.title) },
                            label = { Text(s.title) }
                        )
                    }
                }
            }
        ) { pad ->
            val mod = Modifier.padding(pad)
            when (screen) {
                Screen.Dashboard -> DashboardScreen(mod)
                Screen.Kalkulator -> KalkulatorScreen(mod)
                Screen.CekPinjol -> CekPinjolScreen(mod)
            }
        }
    }
}
