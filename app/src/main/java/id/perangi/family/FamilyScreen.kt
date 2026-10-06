package id.perangi.family

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.perangi.data.DeviceId
import id.perangi.data.Supabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Layar Keluarga (Fase 3b): pairing ortu-anak via kode 6 digit +
 * dashboard status anak (baca heartbeat).
 */
data class ChildStatus(
    val shortHash: String,
    val protectionOn: Boolean,
    val fresh: Boolean,
    val blockedToday: Int,
)

private fun familyPrefs(ctx: Context) =
    ctx.getSharedPreferences("perangi_family", Context.MODE_PRIVATE)

private fun parseIsoMs(iso: String): Long = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso.take(19))?.time ?: 0L
} catch (_: Exception) {
    0L
}

@Composable
fun FamilyScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var myCode by remember { mutableStateOf(familyPrefs(ctx).getString("my_code", "") ?: "") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var children by remember { mutableStateOf(listOf<ChildStatus>()) }
    var loadingKids by remember { mutableStateOf(false) }

    fun io(work: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { busy = true }
            try {
                work()
            } finally {
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    fun loadChildren() {
        loadingKids = true
        scope.launch(Dispatchers.IO) {
            val myHash = DeviceId.hash(ctx)
            val list = mutableListOf<ChildStatus>()
            try {
                val pairsJson = Supabase.get(
                    "perangi_pairs",
                    "parent_hash=eq.$myHash&select=child_hash"
                ) ?: "[]"
                val kids = JSONArray(pairsJson)
                for (i in 0 until kids.length()) {
                    val childHash = kids.getJSONObject(i).optString("child_hash")
                    val hbJson = Supabase.get(
                        "perangi_heartbeats",
                        "device_hash=eq.$childHash&select=protection_on,blocked_today,updated_at"
                    ) ?: "[]"
                    val hb = JSONArray(hbJson)
                    if (hb.length() == 0) {
                        list.add(ChildStatus(childHash.take(8), false, false, 0))
                    } else {
                        val o = hb.getJSONObject(0)
                        val updated = parseIsoMs(o.optString("updated_at"))
                        val fresh = System.currentTimeMillis() - updated < 30 * 60 * 1000L
                        list.add(
                            ChildStatus(
                                childHash.take(8),
                                o.optBoolean("protection_on", false),
                                fresh,
                                o.optInt("blocked_today", 0)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
            withContext(Dispatchers.Main) {
                children = list
                loadingKids = false
            }
        }
    }

    LazyColumn(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Keluarga",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Hubungkan HP anak dan pantau status proteksinya.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }

        // ── Mode anak: tampilkan kode ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👶 HP Anak — Kode Keluarga", fontWeight = FontWeight.Bold)
                    Text(
                        "Tunjukkan kode ini ke orang tuamu untuk dihubungkan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (myCode.isNotEmpty()) {
                        Text(
                            myCode.chunked(3).joinToString(" "),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            io {
                                val prefs = familyPrefs(ctx)
                                val existing = prefs.getString("my_code", "") ?: ""
                                if (existing.isNotEmpty()) {
                                    withContext(Dispatchers.Main) { myCode = existing }
                                    return@io
                                }
                                val childHash = DeviceId.hash(ctx)
                                var code = ""
                                for (attempt in 1..5) {
                                    val candidate = (100000..999999).random().toString()
                                    val st = Supabase.post(
                                        "perangi_pairs",
                                        JSONObject().apply {
                                            put("code", candidate)
                                            put("child_hash", childHash)
                                        }
                                    )
                                    if (st in 200..299) {
                                        code = candidate
                                        break
                                    }
                                    if (st != 409) {
                                        withContext(Dispatchers.Main) {
                                            msg = "Gagal membuat kode, coba lagi."
                                        }
                                        return@io
                                    }
                                    // 409 = kode kebetulan dipakai orang lain, coba kode baru
                                }
                                if (code.isNotEmpty()) {
                                    prefs.edit().putString("my_code", code).apply()
                                    withContext(Dispatchers.Main) { myCode = code }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        msg = "Gagal membuat kode, coba lagi."
                                    }
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text(if (myCode.isEmpty()) "Tampilkan Kode Saya" else "Kode Sudah Aktif") }
                }
            }
        }

        // ── Mode ortu: masukkan kode ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👨‍👩‍👧 HP Ortu — Hubungkan Anak", fontWeight = FontWeight.Bold)
                    Text(
                        "Masukkan 6 digit kode dari HP anak.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pairCode,
                        onValueChange = {
                            pairCode = it.filter(Char::isDigit).take(6); msg = ""
                        },
                        label = { Text("Kode anak") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            io {
                                val found = Supabase.get(
                                    "perangi_pairs",
                                    "code=eq.$pairCode&select=child_hash"
                                ) ?: "[]"
                                if (JSONArray(found).length() == 0) {
                                    withContext(Dispatchers.Main) {
                                        msg = "Kode tidak ditemukan. Cek lagi 6 digitnya."
                                    }
                                    return@io
                                }
                                val st = Supabase.patch(
                                    "perangi_pairs",
                                    "code=eq.$pairCode",
                                    JSONObject().apply {
                                        put("parent_hash", DeviceId.hash(ctx))
                                    }
                                )
                                withContext(Dispatchers.Main) {
                                    msg = if (st in 200..299) {
                                        pairCode = ""
                                        loadChildren()
                                        "✅ Terhubung! HP anak masuk daftar pantauan."
                                    } else {
                                        "Gagal menghubungkan, coba lagi."
                                    }
                                }
                            }
                        },
                        enabled = !busy && pairCode.length == 6
                    ) { Text("Hubungkan") }
                    if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Dashboard anak ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👀 Anak Terhubung", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { loadChildren() }, enabled = !loadingKids) {
                    Text(if (loadingKids) "..." else "Muat ulang")
                }
            }
        }
        if (children.isEmpty()) {
            item {
                Text(
                    "Belum ada HP anak terhubung.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        items(children) { c ->
            val ok = c.fresh && c.protectionOn
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            (if (ok) "🟢 " else "🔴 ") + "Anak • ${c.shortHash}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (!c.fresh) "Tidak ada kabar > 30 mnt (mati/uninstall?)"
                            else if (!c.protectionOn) "Proteksi MATI — segera cek!"
                            else "Aman • ${c.blockedToday} diblokir hari ini",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
