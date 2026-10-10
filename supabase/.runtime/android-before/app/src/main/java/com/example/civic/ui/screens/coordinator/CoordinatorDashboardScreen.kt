package com.example.civic.ui.screens.coordinator

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
import com.example.civic.data.models.ConflictStatus
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun CoordinatorDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val conflicts = CivicDataRepository.conflicts
    val unresolvedConflicts = conflicts.count { it.status == ConflictStatus.UNRESOLVED || it.status == ConflictStatus.UNDER_DISCUSSION }
    val resolvedConflicts = conflicts.count { it.status == ConflictStatus.RESOLVED }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஒருங்கிணைப்பாளர் முகப்பு" else "Inter-Dept Coordinator",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) "ஒருங்கிணைப்பாளர்" else "Coordinator"
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
                        text = "Dr. S. Ramanathan",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Inter-Departmental Works Coordination Cell, TN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Conflict Alerts Banner
            if (unresolvedConflicts > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicWarningContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CivicWarningOrange)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTa) "$unresolvedConflicts துறைசார் முரண்பாடுகள் கண்டறியப்பட்டுள்ளன!" else "$unresolvedConflicts Inter-Departmental Work Conflicts Detected!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicWarningOrange
                            )
                            Text(
                                text = if (isTa) "தார் சாலை அமைத்தல் மற்றும் குடிநீர் குழாய் பணி மோதல்களைத் தீர்க்கவும்." else "Road resurfacing vs underground pipe laying overlaps detected in Chennai & Coimbatore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicNavy
                            )
                        }
                        Button(
                            onClick = { onNavigate(Screen.ConflictDetection.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicWarningOrange)
                        ) {
                            Text(if (isTa) "தீர்" else "Resolve", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Metrics Grid
            Text(
                text = if (isTa) "ஒருங்கிணைப்பு அளவீடுகள்" else "Coordination Overview Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "செயலில் உள்ள முரண்பாடுகள்" else "Active Conflicts",
                    value = unresolvedConflicts.toString(),
                    icon = Icons.Default.Warning,
                    containerColor = CivicWarningContainer,
                    contentColor = CivicWarningOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ConflictDetection.route) }
                )
                MetricCard(
                    title = if (isTa) "தீர்க்கப்பட்டவை" else "Resolved Agreements",
                    value = resolvedConflicts.toString(),
                    icon = Icons.Default.CheckCircle,
                    containerColor = CivicAccentGreenContainer,
                    contentColor = CivicNavyDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ResolutionHistory.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "பகிரப்பட்ட திட்டங்கள்" else "Shared Schedule",
                    value = CivicDataRepository.projects.size.toString(),
                    icon = Icons.Default.DateRange,
                    containerColor = CivicMutedBlueLight,
                    contentColor = CivicNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SharedSchedule.route) }
                )
                MetricCard(
                    title = if (isTa) "துறை அறிக்கைகள்" else "Reports & Analytics",
                    value = "5 Depts",
                    icon = Icons.Default.Assessment,
                    containerColor = CivicSurfaceVariant,
                    contentColor = CivicTextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CoordinatorReports.route) }
                )
            }

            // Quick Navigation List
            Text(
                text = if (isTa) "ஒருங்கிணைப்பு பிரிவுகள்" else "Coordination Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = { onNavigate(Screen.ConflictDetection.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "துறைசார் முரண்பாடு கண்டறிதல்" else "Inter-Department Conflict Detection")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.SharedSchedule.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "பகிரப்பட்ட காலஅட்டவணை" else "Shared Inter-Departmental Schedule")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.DepartmentProjects.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "அனைத்து துறை திட்டங்கள்" else "All Department Projects Catalog")
            }
        }
    }
}
