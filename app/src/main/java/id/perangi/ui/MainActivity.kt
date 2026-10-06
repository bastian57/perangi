package id.perangi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import id.perangi.blocklist.BlocklistUpdater
import id.perangi.ui.theme.PerangiTheme
import id.perangi.util.Logger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("perangi", MODE_PRIVATE)
        // Update blocklist di background setiap aplikasi dibuka
        // (jadwal berkala 6 jam dipasang oleh BlocklistUpdateWorker).
        Thread {
            BlocklistUpdater.update(this)
            Logger.d("UI", "MainActivity dibuka")
        }.start()
        setContent {
            PerangiTheme {
                var onboarded by remember {
                    mutableStateOf(prefs.getBoolean("onboarded", false))
                }
                if (onboarded) {
                    PerangiApp()
                } else {
                    OnboardingScreen(onFinish = {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        onboarded = true
                    })
                }
            }
        }
    }
}
