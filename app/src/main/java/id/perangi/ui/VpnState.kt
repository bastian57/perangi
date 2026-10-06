package id.perangi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Status VPN yang bisa diobservasi Compose (satu proses dengan service).
 * Diupdate oleh PerangiVpnService saat start/stop.
 */
object VpnState {
    var running by mutableStateOf(false)
}
