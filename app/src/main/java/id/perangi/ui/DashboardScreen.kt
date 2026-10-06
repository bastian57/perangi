package id.perangi.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import id.perangi.R
import id.perangi.blocklist.BlocklistUpdateWorker
import id.perangi.blocklist.ReportUploadWorker
import id.perangi.data.ParentMode
import id.perangi.data.ReportStore
import id.perangi.data.StatsRepository
import id.perangi.data.StreakStore
import id.perangi.family.HeartbeatWorker
import id.perangi.util.Logger
import id.perangi.vpn.Blocklist
import id.perangi.vpn.KeywordBlock
import id.perangi.vpn.PerangiVpnService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUOTES_TEMPUR = listOf(
    "Judol itu pajak orang putus asa",
    "Pinjol ilegal = lintah digital",
    "Sekali klik judol, selamanya dikejar bandar",
    "Berhenti sekarang lebih murah daripada berhenti nanti",
    "Uang panas judol tidak pernah membawa berkah",
    "Pinjol ilegal menagih dengan teror, bukan aturan"
)

@Composable
private fun MenuCard(
    label: String,
    img: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painterResource(img), label,
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, onNav: (Dest) -> Unit) {
    val ctx = LocalContext.current
    val stats = remember { StatsRepository(ctx) }
    var tick by remember { mutableStateOf(0) }
    val running = VpnState.running
    var showPinGate by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) startVpnService(ctx)
    }

    LaunchedEffect(Unit) {
        BlocklistUpdateWorker.schedule(ctx)
        // Fase 3b: heartbeat keluarga tiap 15 menit
        HeartbeatWorker.schedule(ctx)
        // Fase 3a: upload sisa laporan yang belum terkirim ke server
        if (ReportStore.pendingUpload(ctx).isNotEmpty()) {
            ReportUploadWorker.enqueue(ctx)
        }
        Logger.d("UI", "Dashboard dibuka")
    }

    if (showPinGate) {
        PinDialog(
            title = "PIN Mode Orang Tua",
            onDismiss = { showPinGate = false },
            onVerified = {
                showPinGate = false
                stopVpnService(ctx)
                tick++
            }
        )
    }

    val recent = remember(tick, running) { stats.recentBlocked() }
    val logs = remember(tick, running) { Logger.snapshot().takeLast(40).reversed() }
    val total = remember(tick, running) { stats.totalBlocked() }
    val domains = remember(tick, running) { Blocklist.size() }
    val streak = remember(tick, running) { StreakStore.days(ctx) }

    val marqueeText = remember(total, streak) {
        "🛡️ $total situs diblokir • 🔥 $streak hari terlindungi • " +
            QUOTES_TEMPUR.joinToString(" • ") { "💬 $it" } + " • "
    }

    fun setProteksi(on: Boolean) {
        if (on) {
            val intent = VpnService.prepare(ctx)
            if (intent != null) launcher.launch(intent) else startVpnService(ctx)
        } else {
            if (ParentMode.isEnabled(ctx)) showPinGate = true
            else stopVpnService(ctx)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PERANGI", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "Perang Melawan Judol & Pinjol Ilegal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        item {
            Image(
                painterResource(R.drawable.hero_banner), "PERANGI",
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    marqueeText,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        .padding(12.dp).fillMaxWidth(),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (running) "🛡️ TERLINDUNGI" else "⚠️ TIDAK TERLINDUNGI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (running) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("🔥 $streak hari terlindungi")
                        Text("Situs diblokir: $total")
                        Text("Domain di daftar: $domains")
                        Text("Pola kata: ${KeywordBlock.get().size}")
                    }
                    Switch(
                        checked = running,
                        onCheckedChange = { setProteksi(it) }
                    )
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuCard("Tes", R.drawable.img_tes,
                    onClick = { onNav(SubScreen.Kesehatan) }, modifier = Modifier.weight(1f))
                MenuCard("Ortu", R.drawable.img_ortu,
                    onClick = { onNav(SubScreen.OrangTua) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuCard("Lapor", R.drawable.img_lapor,
                    onClick = { onNav(SubScreen.Lapor) }, modifier = Modifier.weight(1f))
                MenuCard("Putih", R.drawable.img_putih,
                    onClick = { onNav(SubScreen.Whitelist) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuCard("Scan", R.drawable.img_scan,
                    onClick = { onNav(SubScreen.AppScan) }, modifier = Modifier.weight(1f))
                MenuCard("Keluarga", R.drawable.img_keluarga,
                    onClick = { onNav(SubScreen.Keluarga) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(
                onClick = { onNav(SubScreen.Tentang) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("⬇️ Cek Pembaruan & Tentang") }
        }
        item {
            Text("Terakhir diblokir", style = MaterialTheme.typography.titleMedium)
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    "Belum ada — bagus, berarti belum ada yang coba-coba.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            items(recent) { (ts, domain) ->
                val time = remember(ts) {
                    SimpleDateFormat("dd MMM HH:mm", Locale("id")).format(Date(ts))
                }
                Text(
                    "🚫 $domain — $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
        item {
            OutlinedButton(onClick = { tick++ }) { Text("Muat ulang") }
        }
        item {
            Text("Log", style = MaterialTheme.typography.titleMedium)
        }
        if (logs.isEmpty()) {
            item {
                Text(
                    "Belum ada log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            items(logs) { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
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

private fun stopVpnService(ctx: Context) {
    ctx.startForegroundService(
        Intent(ctx, PerangiVpnService::class.java).setAction(PerangiVpnService.ACTION_STOP)
    )
    ctx.getSharedPreferences("perangi", Context.MODE_PRIVATE)
        .edit().putBoolean("vpn_enabled", false).apply()
    Logger.d("UI", "VPN dihentikan dari dashboard")
}
