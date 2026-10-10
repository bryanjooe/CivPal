package com.example.civic.ui.screens.officer

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
import com.example.civic.data.models.ProjectStatus
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun OfficerDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val currentUser = CivicDataRepository.currentUser
    val projects = CivicDataRepository.projects

    val officerName = currentUser?.name ?: "Er. R. Kanimozhi"
    val officeJurisdiction = currentUser?.officeJurisdiction ?: "Highways & PWD Chennai Metro Division"

    val assignedProjects = CivicDataRepository.assignedProjects

    val ongoingCount = assignedProjects.count { it.status == ProjectStatus.ONGOING }
    val delayedCount = assignedProjects.count { it.status == ProjectStatus.DELAYED }
    val pendingVerifications = assignedProjects.sumOf { p -> p.progressReports.count { it.status == "Pending Verification" } }
    val unresolvedComplaints = CivicDataRepository.complaints.count { it.status.name != "RESOLVED" }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அலுவலர் பணிப்பலகை" else "Officer Portal",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) "அரசு அலுவலர்" else "Govt Officer"
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
                        text = "Er. R. Kanimozhi",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = officeJurisdiction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Metrics Grid
            Text(
                text = if (isTa) "ஒப்படைக்கப்பட்ட பணிகள் நிலை" else "Assigned Office Status Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "ஒப்படைத்த திட்டங்கள்" else "Assigned Projects",
                    value = assignedProjects.size.toString(),
                    icon = Icons.Default.Folder,
                    containerColor = CivicMutedBlueLight,
                    contentColor = CivicNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.OfficerMyProjects.route) }
                )
                MetricCard(
                    title = if (isTa) "சரிபார்ப்பு நிலுவையில்" else "Pending Verifications",
                    value = pendingVerifications.toString(),
                    icon = Icons.Default.Verified,
                    containerColor = CivicWarningContainer,
                    contentColor = CivicWarningOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ProgressVerification.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "தாமதமான திட்டங்கள்" else "Delayed Projects",
                    value = delayedCount.toString(),
                    icon = Icons.Default.Warning,
                    containerColor = CivicErrorContainer,
                    contentColor = CivicErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.OfficerMyProjects.route) }
                )
                MetricCard(
                    title = if (isTa) "தீர்க்கப்படாத புகார்கள்" else "Unresolved Complaints",
                    value = unresolvedComplaints.toString(),
                    icon = Icons.Default.ReportProblem,
                    containerColor = CivicSurfaceVariant,
                    contentColor = CivicTextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.MyComplaints.route) }
                )
            }

            // Quick Actions
            Text(
                text = if (isTa) "அலுவலர் முதன்மை செயல்பாடுகள்" else "Officer Management Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.RegisterProject.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTa) "புதிய திட்டம்" else "Register Project", fontSize = 12.sp)
                }

                Button(
                    onClick = { onNavigate(Screen.HandoverDashboard.route) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTa) "ஒப்படைப்பு" else "Handover Module", fontSize = 12.sp)
                }
            }

            // Assigned Projects List Preview
            Text(
                text = if (isTa) "உங்கள் கட்டுப்பாட்டிலுள்ள திட்டங்கள்" else "Managed Projects List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            assignedProjects.forEach { proj ->
                ProjectCard(
                    project = proj,
                    onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) }
                )
            }
        }
    }
}
