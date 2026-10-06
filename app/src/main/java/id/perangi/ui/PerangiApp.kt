package id.perangi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import id.perangi.ui.theme.PerangiTheme

sealed interface Dest

enum class MainTab(val title: String, val icon: ImageVector) : Dest {
    Beranda("Beranda", Icons.Filled.Dashboard),
    Kalkulator("Kalkulator", Icons.Filled.Calculate),
    Darurat("Darurat", Icons.Filled.Sos),
    CekPinjol("Cek Pinjol", Icons.Filled.VerifiedUser),
}

enum class SubScreen(val title: String) : Dest {
    Kesehatan("Tes Kesehatan"),
    OrangTua("Mode Orang Tua"),
    Lapor("Laporkan Situs"),
    Whitelist("Whitelist"),
    AppScan("Scan Aplikasi"),
    Tentang("Tentang & Pembaruan"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerangiApp() {
    PerangiTheme {
        var dest by remember { mutableStateOf<Dest>(MainTab.Beranda) }
        val isMain = dest is MainTab
        BackHandler(enabled = !isMain) { dest = MainTab.Beranda }
        Scaffold(
            topBar = {
                if (!isMain) {
                    TopAppBar(
                        title = { Text((dest as SubScreen).title) },
                        navigationIcon = {
                            IconButton(onClick = { dest = MainTab.Beranda }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isMain) {
                    NavigationBar {
                        MainTab.values().forEach { t ->
                            NavigationBarItem(
                                selected = dest == t,
                                onClick = { dest = t },
                                icon = { Icon(t.icon, contentDescription = t.title) },
                                label = { Text(t.title) }
                            )
                        }
                    }
                }
            }
        ) { pad ->
            val mod = Modifier.padding(pad)
            when (val d = dest) {
                MainTab.Beranda -> DashboardScreen(mod, onNav = { dest = it })
                MainTab.Kalkulator -> KalkulatorScreen(mod)
                MainTab.Darurat -> SosScreen(mod, onOpenKalkulator = { dest = MainTab.Kalkulator })
                MainTab.CekPinjol -> CekPinjolScreen(mod)
                SubScreen.Kesehatan -> HealthScreen(mod)
                SubScreen.OrangTua -> ParentModeScreen(mod)
                SubScreen.Lapor -> ReportScreen(mod)
                SubScreen.Whitelist -> WhitelistScreen(mod)
                SubScreen.AppScan -> AppScanScreen(mod)
                SubScreen.Tentang -> AboutScreen(mod)
            }
        }
    }
}
