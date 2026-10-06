package id.perangi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@Composable
fun KalkulatorScreen(modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Rugi Judol") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Bunga Pinjol") })
        }
        if (tab == 0) KalkulatorJudol(Modifier.padding(20.dp))
        else KalkulatorPinjol(Modifier.padding(20.dp))
    }
}

private fun rupiah(n: Double): String =
    "Rp" + NumberFormat.getNumberInstance(Locale("id")).format(n.toLong())

@Composable
private fun KalkulatorJudol(modifier: Modifier = Modifier) {
    var harian by remember { mutableStateOf("50000") }
    val perHari = harian.toDoubleOrNull() ?: 0.0
    val perBulan = perHari * 30
    val perTahun = perHari * 365
    val limaTahun = perTahun * 5
    // Nilai jika ditabung dengan return 6% per tahun (anuitas).
    val tabung5thn = if (perTahun > 0) perTahun * ((1.06.pow(5) - 1) / 0.06) else 0.0

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Kalkulator Rugi Judol",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text("Berapa yang kamu pertaruhkan setiap hari?")
        OutlinedTextField(
            value = harian,
            onValueChange = { harian = it.filter(Char::isDigit).take(12) },
            label = { Text("Taruhan per hari (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        HasilRow("Diputar per bulan", rupiah(perBulan))
        HasilRow("Diputar per tahun", rupiah(perTahun))
        HasilRow("Diputar 5 tahun", rupiah(limaTahun))
        HasilRow("Kalau ditabung 5 thn @6%", rupiah(tabung5thn))
        Text(
            "Catatan jujur: ini total uang yang kamu PERTARUHKAN, bukan ramalan. " +
                "Secara matematis pemain selalu kalah jangka panjang karena bandar " +
                "mengambil margin (house edge) di setiap taruhan. Uang yang 'diputar' " +
                "di atas = uang yang pelan-pelan pindah ke kantong bandar.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun KalkulatorPinjol(modifier: Modifier = Modifier) {
    var pokok by remember { mutableStateOf("1000000") }
    var bunga by remember { mutableStateOf("2") }
    var hari by remember { mutableStateOf("30") }
    val p = pokok.toDoubleOrNull() ?: 0.0
    val b = bunga.toDoubleOrNull() ?: 0.0
    val h = hari.toDoubleOrNull() ?: 0.0
    val total = if (p > 0 && h > 0) p * (1 + b / 100).pow(h) else 0.0

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Kalkulator Bunga Pinjol",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text("Pinjol ilegal sering mematok bunga HARIAN. Lihat sendiri ngerinya.")
        OutlinedTextField(
            value = pokok,
            onValueChange = { pokok = it.filter(Char::isDigit).take(12) },
            label = { Text("Pokok pinjaman (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bunga,
            onValueChange = { bunga = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
            label = { Text("Bunga per hari (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hari,
            onValueChange = { hari = it.filter(Char::isDigit).take(4) },
            label = { Text("Tenor (hari)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        HasilRow("Total harus dibayar", rupiah(total))
        HasilRow("Bunga saja", rupiah(total - p))
        Text(
            "Pinjam Rp1 juta berbunga 2%/hari jadi Rp1,8 juta dalam sebulan. " +
                "Inilah kenapa pinjol ilegal menghancurkan hidup.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HasilRow(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
