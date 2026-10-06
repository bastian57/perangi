package id.perangi.ui

import android.app.Activity
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import id.perangi.util.Logger
import id.perangi.vpn.PerangiVpnService

/**
 * Onboarding 3 langkah (pelajaran Fase 0): sambutan → izin VPN →
 * panduan mematikan Secure DNS (wajib agar blokir tidak diakali).
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn(ctx)
            step = 2
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (step) {
            0 -> {
                Text("🛡️", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Selamat datang di PERANGI",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aplikasi ini memblokir situs judi online, pinjol ilegal, dan iklan judi " +
                        "langsung di HP-mu — tanpa root. Semua penyaringan diproses lokal di HP ini."
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lanjut")
                }
            }
            1 -> {
                Text(
                    "Langkah 1: Aktifkan Proteksi",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android akan meminta izin VPN. Ini VPN LOKAL — datamu tidak dikirim " +
                        "ke server mana pun."
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val intent = VpnService.prepare(ctx)
                        if (intent != null) launcher.launch(intent)
                        else {
                            startVpn(ctx)
                            step = 2
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aktifkan VPN")
                }
            }
            2 -> {
                Text(
                    "Langkah 2: Matikan Secure DNS",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Agar blokir tidak bisa diakali browser, matikan DNS aman:\n\n" +
                        "Chrome: ⋮ → Setelan → Privasi dan keamanan → " +
                        "Gunakan DNS aman → NONAKTIF\n\n" +
                        "HP: Setelan → Jaringan → DNS Pribadi → Nonaktif"
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        Logger.d("UI", "Onboarding selesai")
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesai — Mulai Lindungi HP")
                }
            }
        }
    }
}

private fun startVpn(ctx: android.content.Context) {
    ctx.startForegroundService(
        Intent(ctx, PerangiVpnService::class.java).setAction(PerangiVpnService.ACTION_START)
    )
    ctx.getSharedPreferences("perangi", android.content.Context.MODE_PRIVATE)
        .edit().putBoolean("vpn_enabled", true).apply()
    VpnState.running = true
    Logger.d("UI", "VPN dimulai dari onboarding")
}
