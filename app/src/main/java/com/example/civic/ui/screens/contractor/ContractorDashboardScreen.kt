package com.example.civic.ui.screens.contractor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun ContractorDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.assignedProjects

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஒப்பந்ததாரர் பணிப்பலகை" else "Contractor Portal",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) "ஒப்பந்ததாரர்" else "Contractor"
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
            // Welcome Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "L&T Infrastructure Ltd",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Class-I Registered Civil Contractor | License: CON-LT-9081",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Metrics Grid
            Text(
                text = if (isTa) "ஒப்பந்தப்பணிகள் நிலை" else "Active Contracts Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "ஒப்படைத்த பணிகள்" else "Assigned Works",
                    value = projects.size.toString(),
                    icon = Icons.Default.Assignment,
                    containerColor = CivicMutedBlueLight,
                    contentColor = CivicNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ContractorAssignedProjects.route) }
                )
                MetricCard(
                    title = if (isTa) "சரிபார்ப்பு நிலுவையில்" else "Pending Officer Verification",
                    value = projects.sumOf { it.progressReports.count { r -> r.status == "Pending Verification" } }.toString(),
                    icon = Icons.Default.PendingActions,
                    containerColor = CivicWarningContainer,
                    contentColor = CivicWarningOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SubmitProgress.route) }
                )
            }

            // Actions
            Text(
                text = if (isTa) "ஒப்பந்ததாரர் செயல்பாடுகள்" else "Contractor Action Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.SubmitProgress.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTa) "முன்னேற்றம் சமர்ப்பி" else "Submit Progress", fontSize = 12.sp)
                }

                Button(
                    onClick = { onNavigate(Screen.DeadlineRevision.route) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTa) "காலக்கெடு கோரிக்கை" else "Revision Request", fontSize = 12.sp)
                }
            }

            // Assigned Projects List
            Text(
                text = if (isTa) "ஒப்பந்தங்கள் பட்டியல்" else "Assigned Project Contracts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            projects.take(2).forEach { proj ->
                ProjectCard(
                    project = proj,
                    onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) }
                )
            }
        }
    }
}
