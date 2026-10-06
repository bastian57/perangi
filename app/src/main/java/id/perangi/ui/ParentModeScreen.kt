package id.perangi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.perangi.data.ParentMode
import id.perangi.util.Logger

@Composable
fun ParentModeScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(ParentMode.isEnabled(ctx)) }
    var hasPin by remember { mutableStateOf(ParentMode.hasPin(ctx)) }
    // null = tidak ada dialog; "disable" / "change" = mode dialog PIN
    var pinGateFor by remember { mutableStateOf<String?>(null) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    pinGateFor?.let { mode ->
        PinDialog(
            title = if (mode == "disable") "Masukkan PIN untuk menonaktifkan"
            else "Masukkan PIN lama",
            onDismiss = { pinGateFor = null },
            onVerified = {
                pinGateFor = null
                if (mode == "disable") {
                    ParentMode.setEnabled(ctx, false)
                    enabled = false
                    Logger.d("UI", "Mode Orang Tua dinonaktifkan")
                } else {
                    // ganti PIN: tampilkan form buat PIN baru
                    hasPin = false
                    pin1 = ""; pin2 = ""; msg = ""
                }
            }
        )
    }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Mode Orang Tua",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Kunci pengaturan dengan PIN. Mematikan proteksi, mengubah whitelist, " +
                "dan menonaktifkan mode ini butuh PIN.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )

        if (!hasPin) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Buat PIN (4-6 digit)", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { pin1 = it.filter(Char::isDigit).take(6) },
                        label = { Text("PIN baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { pin2 = it.filter(Char::isDigit).take(6) },
                        label = { Text("Ulangi PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (msg.isNotEmpty()) {
                        Text(msg, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = {
                        msg = when {
                            pin1.length < 4 -> "PIN minimal 4 digit."
                            pin1 != pin2 -> "PIN tidak sama. Ulangi."
                            else -> {
                                ParentMode.setPin(ctx, pin1)
                                ParentMode.setEnabled(ctx, true)
                                hasPin = true
                                enabled = true
                                pin1 = ""; pin2 = ""
                                Logger.d("UI", "Mode Orang Tua diaktifkan")
                                "Mode Orang Tua aktif."
                            }
                        }
                    }) { Text("Simpan & Aktifkan") }
                }
            }
        } else {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mode Orang Tua",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (on) {
                                ParentMode.setEnabled(ctx, true)
                                enabled = true
                            } else {
                                pinGateFor = "disable"
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { pinGateFor = "change" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ganti PIN")
            }
        }
    }
}
