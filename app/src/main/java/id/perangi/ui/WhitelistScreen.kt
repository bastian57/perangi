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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import id.perangi.data.ParentMode
import id.perangi.data.WhitelistStore
import id.perangi.util.Logger

@Composable
fun WhitelistScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var domain by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    // aksi tertunda yang butuh PIN: Pair("add"/"remove", domain)
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) }

    pending?.let { (action, d) ->
        PinDialog(
            title = "PIN Mode Orang Tua",
            onDismiss = { pending = null },
            onVerified = {
                if (action == "add") {
                    WhitelistStore.add(ctx, d)
                    Logger.d("UI", "Whitelist +$d")
                } else {
                    WhitelistStore.remove(ctx, d)
                    Logger.d("UI", "Whitelist -$d")
                }
                pending = null
                domain = ""
                refresh++
            }
        )
    }

    fun doAdd(d: String) {
        if (d.isBlank()) return
        if (ParentMode.isEnabled(ctx)) pending = "add" to d
        else {
            WhitelistStore.add(ctx, d)
            domain = ""
            refresh++
        }
    }

    fun doRemove(d: String) {
        if (ParentMode.isEnabled(ctx)) pending = "remove" to d
        else {
            WhitelistStore.remove(ctx, d)
            refresh++
        }
    }

    val list = remember(refresh) { WhitelistStore.all().sorted() }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Whitelist",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Domain di sini TIDAK diblokir (untuk false positive). " +
                (if (ParentMode.isEnabled(ctx)) "Mode Orang Tua aktif — butuh PIN." else ""),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Domain") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { doAdd(domain) }) { Text("Tambah") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) {
                item {
                    Text(
                        "Whitelist kosong.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(list) { d ->
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
                            Text(d, modifier = Modifier.weight(1f))
                            TextButton(onClick = { doRemove(d) }) {
                                Text(
                                    "Hapus",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
