package id.perangi.ui

import android.content.Context
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Tombol Darurat: untuk yang lagi "sakau" pengen main.
 * Senjata psikologis — ingatkan ruginya + arahkan ke bantuan.
 */
@Composable
fun SosScreen(modifier: Modifier = Modifier, onOpenKalkulator: () -> Unit) {
    val ctx = LocalContext.current
    val harian = remember {
        ctx.getSharedPreferences("perangi", Context.MODE_PRIVATE)
            .getString("sos_harian", "50000") ?: "50000"
    }
    val perHari = harian.toDoubleOrNull() ?: 0.0
    val limaTahun = remember(perHari) {
        "Rp" + NumberFormat.getNumberInstance(Locale("id")).format((perHari * 365 * 5).toLong())
    }
    val harianFmt = remember(harian) {
        "Rp" + NumberFormat.getNumberInstance(Locale("id")).format((harian.toDoubleOrNull() ?: 0.0).toLong())
    }

    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🆘", fontSize = 64.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "TAHAN.",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Rasa pengen main itu cuma sementara.\nRuginya permanen.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
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
                    "Ingat hitunganmu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$harianFmt/hari",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "= $limaTahun hangus dalam 5 tahun",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Coba ini sekarang:\n" +
                "1. Tarik napas dalam 60 detik.\n" +
                "2. Telepon/chat orang yang kamu percaya.\n" +
                "3. Hapus aplikasinya kalau masih ada.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpenKalkulator,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Lihat Hitunganku Lagi")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { /* TODO(Fase 3): hotline konseling */ },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Aku Butuh Bantuan (segera)")
        }
    }
}
