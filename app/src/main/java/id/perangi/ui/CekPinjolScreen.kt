package id.perangi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import id.perangi.data.OjkData
import id.perangi.data.PinjolCheck

@Composable
fun CekPinjolScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PinjolCheck?>(null) }

    LaunchedEffect(Unit) { OjkData.load(ctx) }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Cek Pinjol",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Ketik nama aplikasi pinjol untuk cek statusnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Nama pinjol") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { result = OjkData.check(query) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cek Sekarang")
        }
        result?.let { r ->
            val (emoji, label) = when (r.status) {
                PinjolCheck.Status.LEGAL -> "✅" to "LEGAL (terdaftar OJK)"
                PinjolCheck.Status.ILEGAL -> "🚫" to "ILEGAL"
                PinjolCheck.Status.UNKNOWN -> "❓" to "TIDAK DIKENAL"
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("$emoji $label", fontWeight = FontWeight.Bold)
                    if (r.note.isNotEmpty()) {
                        Text(
                            r.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            OjkData.warning(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            "Butuh bantuan atau mau lapor? Hubungi OJK di 157 (Senin–Jumat).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
