package id.perangi.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.perangi.blocklist.BlocklistUpdateWorker
import id.perangi.data.StatsRepository
import id.perangi.util.Logger
import id.perangi.vpn.Blocklist
import id.perangi.vpn.PerangiVpnService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val stats = remember { StatsRepository(ctx) }
    var tick by remember { mutableStateOf(0) }
    val running = VpnState.running

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) startVpnService(ctx)
    }

    LaunchedEffect(Unit) {
        BlocklistUpdateWorker.schedule(ctx)
        Logger.d("UI", "Dashboard dibuka")
    }

    // baca ulang setiap ada perubahan status / tombol refresh
    val recent = remember(tick, running) { stats.recentBlocked() }
    val logs = remember(tick, running) { Logger.snapshot().takeLast(40).reversed() }
    val total = remember(tick, running) { stats.totalBlocked() }
    val domains = remember(tick, running) { Blocklist.size() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PERANGI", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "Perang Melawan Judol & Pinjol Ilegal",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (running) "🛡️ Proteksi AKTIF" else "⚠️ Proteksi MATI",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Situs diblokir: $total")
                    Text("Domain di daftar: $domains")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val intent = VpnService.prepare(ctx)
                        if (intent != null) launcher.launch(intent) else startVpnService(ctx)
                    }) {
                        Text(if (running) "Aktifkan Ulang" else "Aktifkan Proteksi")
                    }
                }
            }
        }
        item {
            Text("Terakhir diblokir", fontWeight = FontWeight.Bold)
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    "Belum ada — bagus, berarti belum ada yang coba-coba.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            items(recent) { (ts, domain) ->
                val time = remember(ts) {
                    SimpleDateFormat("dd MMM HH:mm", Locale("id")).format(Date(ts))
                }
                Text("🚫 $domain — $time", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            OutlinedButton(onClick = { tick++ }) { Text("Muat ulang") }
        }
        item {
            Text("Log", fontWeight = FontWeight.Bold)
        }
        if (logs.isEmpty()) {
            item { Text("Belum ada log.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(logs) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun startVpnService(ctx: Context) {
    ctx.startForegroundService(
        Intent(ctx, PerangiVpnService::class.java).setAction(PerangiVpnService.ACTION_START)
    )
    ctx.getSharedPreferences("perangi", Context.MODE_PRIVATE)
        .edit().putBoolean("vpn_enabled", true).apply()
    VpnState.running = true
    Logger.d("UI", "VPN dimulai dari dashboard")
}
