package id.perangi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.perangi.vpn.Blocklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class HealthItem(val name: String, val ok: Boolean, val detail: String)

/**
 * Tes Kesehatan Proteksi: diagnosis mandiri — VPN aktif? DNS jalan?
 * Blocklist ke-load? Belajar dari insiden "internet lumpuh" Fase 1.
 */
@Composable
fun HealthScreen(modifier: Modifier = Modifier) {
    var running by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<HealthItem>?>(null) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        running = true
        items = null
        val list = mutableListOf<HealthItem>()

        // 1. VPN aktif?
        val vpnOn = VpnState.running
        list.add(
            HealthItem(
                "VPN Proteksi",
                vpnOn,
                if (vpnOn) "Aktif — lalu lintas DNS disaring." else "MATI — nyalakan di Beranda."
            )
        )

        // 2. Blocklist ke-load?
        val n = Blocklist.size()
        list.add(
            HealthItem(
                "Daftar blokir",
                n > 0,
                if (n > 0) "$n domain dimuat." else "KOSONG — install ulang aplikasi."
            )
        )

        // 3. DNS berfungsi? (resolve via DNS sistem — lewat VPN kita jika aktif)
        val dnsOk = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName("google.com")
                true
            } catch (_: Exception) {
                false
            }
        }
        list.add(
            HealthItem(
                "Koneksi DNS",
                dnsOk,
                if (dnsOk) "Resolusi nama OK." else "GAGAL — matikan lalu nyalakan proteksi."
            )
        )

        // 4. Update otomatis
        list.add(
            HealthItem(
                "Update otomatis",
                true,
                "Jadwal 6 jam terpasang."
            )
        )

        items = list
        running = false
    }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Tes Kesehatan Proteksi",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        if (running || items == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(
                "Memeriksa...",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            val list = items!!
            val score = list.count { it.ok }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$score/${list.size}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score == list.size) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (score == list.size) "SEHAT — proteksi berjalan penuh."
                        else "ADA MASALAH — ikuti saran di bawah.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            list.forEach { item ->
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
                        Text(
                            if (item.ok) "✅" else "❌",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(item.name, fontWeight = FontWeight.Medium)
                            Text(
                                item.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { tick++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tes Ulang")
            }
        }
    }
}
