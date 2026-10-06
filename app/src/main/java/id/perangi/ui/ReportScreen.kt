package id.perangi.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.perangi.data.REPORT_CATEGORIES
import id.perangi.data.ReportStore
import id.perangi.blocklist.ReportUploadWorker
import id.perangi.util.Logger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var domain by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(REPORT_CATEGORIES[0]) }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    val reports = remember(refresh) { ReportStore.myReports(ctx) }
    val left = remember(refresh) { 5 - ReportStore.reportsToday(ctx) }

    LazyColumn(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Laporkan Situs",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Bantu lindungi orang lain. Laporanmu ditampung dan dikurasi.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
        item {
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it; msg = "" },
                label = { Text("Domain (contoh: situsjudol.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Text("Kategori", fontWeight = FontWeight.Medium)
        }
        items(REPORT_CATEGORIES) { cat ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = category == cat,
                    onClick = { category = cat }
                )
                Text(cat)
            }
        }
        item {
            if (msg.isNotEmpty()) {
                Text(
                    msg,
                    color = if (msgOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    val err = ReportStore.submit(ctx, domain, category)
                    if (err == null) {
                        msg = "Laporan terkirim. Terima kasih sudah peduli! 🙏"
                        msgOk = true
                        domain = ""
                        refresh++
                        ReportUploadWorker.enqueue(ctx)
                        Logger.d("UI", "Laporan dikirim: $category")
                    } else {
                        msg = err
                        msgOk = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kirim Laporan (sisa $left hari ini)")
            }
        }
        item {
            Text("Laporanku", fontWeight = FontWeight.Bold)
        }
        if (reports.isEmpty()) {
            item {
                Text(
                    "Belum ada laporan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            items(reports) { r ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.domain, fontWeight = FontWeight.Medium)
                        val time = remember(r.time) {
                            SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id"))
                                .format(Date(r.time))
                        }
                        Text(
                            "${r.category} • $time • menunggu kurasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
