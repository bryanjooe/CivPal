package com.example.civic

import android.os.Bundle
import android.content.Intent
import com.example.civic.data.backend.CivicSupabase
import com.example.civic.data.CivicDataRepository
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.example.civic.ui.navigation.CivicNavHost
import com.example.civic.ui.theme.CivicNavy
import com.example.civic.ui.theme.CivicSurface
import com.example.civic.ui.theme.CivicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiveAuthLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CivicNavy.toArgb()),
            navigationBarStyle = SystemBarStyle.light(CivicSurface.toArgb(), CivicSurface.toArgb())
        )
        setContent {
            CivicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CivicNavHost()
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveAuthLink(intent)
    }
    private fun receiveAuthLink(intent: Intent) {
        val uri = intent.data ?: return
        if (!CivicSupabase.configured || uri.scheme != "civic" || uri.host != "auth-callback") return
        CivicSupabase.client.handleDeeplinks(intent,
            onSessionSuccess = {
                val fragment = android.net.Uri.parse("https://callback/?${uri.fragment.orEmpty()}")
                CivicDataRepository.recoverPassword = fragment.getQueryParameter("type") in listOf("recovery", "invite")
                CivicDataRepository.launchOperation { CivicDataRepository.refresh() }
            })
    }
}
