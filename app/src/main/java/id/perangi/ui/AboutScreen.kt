package id.perangi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import id.perangi.data.ReleaseInfo
import id.perangi.data.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<ReleaseInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    val installed = remember { UpdateChecker.installedVersion(ctx) }
    val installedMs = remember { UpdateChecker.installedTime(ctx) }

    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        checking = true
        failed = false
        info = null
        info = withContext(Dispatchers.IO) { UpdateChecker.check() }
        if (info == null) failed = true
        checking = false
    }

    fun openUrl(url: String) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Tentang & Pembaruan",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🛡️ PERANGI", fontWeight = FontWeight.Bold)
                Text("Versi terpasang: $installed")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Perang Melawan Judol & Pinjol Ilegal.\n" +
                        "Filter DNS lokal — datamu tidak dikirim ke server mana pun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = { tick++ },
            modifier = Modifier.fillMaxWidth(),
            enabled = !checking
        ) {
            Text(if (checking) "Memeriksa..." else "⬇️ Cek Pembaruan")
        }
        if (checking) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (failed) {
            Text(
                "Gagal memeriksa — periksa koneksi internet lalu coba lagi.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        info?.let { r ->
            val hasNewer = r.publishedAtMs > installedMs && r.apkUrl.isNotEmpty()
            val date = remember(r.publishedAtMs) {
                if (r.publishedAtMs > 0) SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id"))
                    .format(Date(r.publishedAtMs)) else "-"
            }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (hasNewer) "🎉 Ada versi baru!" else "✅ Sudah versi terbaru",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${r.name} • $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (r.notes.isNotBlank()) {
                        Text(
                            r.notes.take(500),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (hasNewer) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { openUrl(r.apkUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Download APK Terbaru")
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { openUrl("https://github.com/bastian57/perangi") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kode Sumber di GitHub")
        }
        Text(
            "Dibuat dengan ♥ untuk Indonesia bebas judol & pinjol ilegal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
