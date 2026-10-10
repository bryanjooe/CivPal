package com.example.civic.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun AdminSettingsScreen(
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoAuditEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அமைப்பு அமைப்புகள்" else "System Settings",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isTa) "அமைப்பின் முதன்மை அமைப்புகள்:" else "Portal Global Configuration:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SMS / Email Public Notifications")
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automatic Audit Log Tracking")
                        Switch(checked = autoAuditEnabled, onCheckedChange = { autoAuditEnabled = it })
                    }
                }
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isTa) "நிர்வாகியிலிருந்து வெளியேறு" else "Logout Administrator")
            }
        }
    }
}
