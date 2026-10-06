package id.perangi.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SuspiciousApp(
    val packageName: String,
    val label: String,
    val keyword: String,
    val isSystem: Boolean
)

private val APP_KEYWORDS = listOf(
    "togel", "gacor", "judol", "judi", "maxwin",
    "slotgacor", "rtpslot", "polaslot", "pinjol"
)

/**
 * Scan aplikasi terinstal: judol/pinjol ilegal versi APK tidak lewat DNS,
 * jadi tidak bisa diblokir filter — satu-satunya jalan: deteksi & uninstall.
 * Hasil bersifat "mencurigakan, periksa manual", bukan vonis.
 */
fun scanSuspiciousApps(ctx: Context): List<SuspiciousApp> {
    val pm = ctx.packageManager
    val pkgs = try {
        pm.getInstalledPackages(0)
    } catch (_: Exception) {
        return emptyList()
    }
    return pkgs.mapNotNull { pi ->
        val pkg = pi.packageName
        if (pkg == ctx.packageName) return@mapNotNull null
        val ai = pi.applicationInfo ?: return@mapNotNull null
        val label = try {
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg
        }
        val hay = "$pkg $label".lowercase()
        val hit = APP_KEYWORDS.firstOrNull { hay.contains(it) } ?: return@mapNotNull null
        val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        SuspiciousApp(pkg, label, hit, isSystem)
    }.sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
}

@Composable
fun AppScanScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<List<SuspiciousApp>?>(null) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        result = null
        result = withContext(Dispatchers.IO) { scanSuspiciousApps(ctx) }
    }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Scan Aplikasi",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Aplikasi judi/pinjol ilegal versi APK tidak lewat DNS sehingga " +
                "tidak bisa diblokir filter. Kalau ketemu, uninstall manual.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        val list = result
        if (list == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(
                "Memindai...",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else if (list.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "✅ Bersih — tidak ada aplikasi mencurigakan.",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                "⚠️ ${list.size} aplikasi mencurigakan — periksa satu per satu:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { app ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, fontWeight = FontWeight.Medium)
                                Text(
                                    "${app.packageName}\nKata kunci: \"${app.keyword}\"" +
                                        if (app.isSystem) " • aplikasi sistem" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${app.packageName}")
                                    )
                                )
                            }) {
                                Text("Periksa")
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { tick++ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Ulang")
        }
    }
}
