package com.example.civic.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun CoordinatorReportsScreen(
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "துறை அறிக்கைகள்" else "Coordination Analytics & Reports",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isTa) "துறைசார் பணி செயல்திறன் அறிக்கைகள்:" else "Inter-Departmental Performance Analytics:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ReportStatCard(title = "Public Works Dept (PWD)", total = 2, ongoing = 1, delayed = 0)
            ReportStatCard(title = "TWAD Board (Water Supply)", total = 1, ongoing = 0, delayed = 1)
            ReportStatCard(title = "Highways Department", total = 1, ongoing = 0, delayed = 0)
            ReportStatCard(title = "Municipal Administration", total = 2, ongoing = 1, delayed = 0)
        }
    }
}

@Composable
private fun ReportStatCard(title: String, total: Int, ongoing: Int, delayed: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Projects: $total", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Ongoing: $ongoing", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = "Delayed: $delayed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
