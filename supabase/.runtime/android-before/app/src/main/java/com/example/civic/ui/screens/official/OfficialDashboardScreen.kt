package com.example.civic.ui.screens.official

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.civic.data.models.ComplaintStatus
import com.example.civic.data.models.ConflictStatus
import com.example.civic.data.models.HandoverStatus
import com.example.civic.data.models.ProjectStatus
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun OfficialDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val currentUser = CivicDataRepository.currentUser
    val currentRole = currentUser?.role ?: UserRole.OFFICER

    val projects = CivicDataRepository.projects
    val complaints = CivicDataRepository.complaints
    val conflicts = CivicDataRepository.conflicts
    val handovers = CivicDataRepository.handovers

    val pendingComplaintsCount = complaints.count { it.status == ComplaintStatus.PENDING || it.status == ComplaintStatus.UNDER_REVIEW }
    val unresolvedConflictsCount = conflicts.count { it.status == ConflictStatus.UNRESOLVED || it.status == ConflictStatus.UNDER_DISCUSSION }
    val pendingHandoversCount = handovers.count { it.status == HandoverStatus.INITIATED || it.status == HandoverStatus.CLARIFICATION_REQUESTED }
    val delayedProjectsCount = projects.count { it.status == ProjectStatus.DELAYED }
    val totalAssignedProjects = projects.size

    val totalPendingActions = pendingComplaintsCount + unresolvedConflictsCount + pendingHandoversCount + delayedProjectsCount

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அதிகாரப்பூர்வ தளம்" else "Official Workspace",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) currentRole.labelTa else currentRole.labelEn
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
            // User Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "வணக்கம், ${currentUser?.name ?: "Official"}!",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUser?.officeJurisdiction ?: currentUser?.department ?: "Government of Tamil Nadu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (isTa) currentRole.labelTa else currentRole.labelEn,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Smart Action Inbox Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.ActionInbox.route) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = totalPendingActions.toString(),
                                modifier = Modifier.padding(2.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "ஸ்மார்ட் நடவடிக்கை பெட்டி" else "Smart Action Inbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (isTa) "$totalPendingActions நடவடிக்கைகளுக்கான எச்சரிக்கைகள் நிலுவையில் உள்ளன" else "$totalPendingActions pending action items requiring attention",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Inbox",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Overview Metrics Grid
            Text(
                text = if (isTa) "பணி நிலை கண்ணோட்டம்" else "Workspace Summary & Action Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "ஒதுக்கப்பட்டவை" else "Assigned Projects",
                    value = totalAssignedProjects.toString(),
                    icon = Icons.Default.Folder,
                    containerColor = CivicMutedBlueLight,
                    contentColor = CivicNavy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val route = when (currentRole) {
                            UserRole.OFFICER -> Screen.OfficerMyProjects.route
                            UserRole.COORDINATOR -> Screen.DepartmentProjects.route
                            UserRole.CONTRACTOR -> Screen.ContractorAssignedProjects.route
                            else -> Screen.PublicProjects.route
                        }
                        onNavigate(route)
                    }
                )
                MetricCard(
                    title = if (isTa) "தாமதமான திட்டங்கள்" else "Overdue / Delayed",
                    value = delayedProjectsCount.toString(),
                    icon = Icons.Default.Warning,
                    containerColor = CivicWarningContainer,
                    contentColor = CivicWarningOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ActionInbox.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "துறை முரண்பாடுகள்" else "Dept Conflicts",
                    value = unresolvedConflictsCount.toString(),
                    icon = Icons.Default.AccountTree,
                    containerColor = CivicSurfaceVariant,
                    contentColor = CivicTextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ConflictDetection.route) }
                )
                MetricCard(
                    title = if (isTa) "புகார்கள் திறனாய்வு" else "Complaints Review",
                    value = pendingComplaintsCount.toString(),
                    icon = Icons.Default.ReportProblem,
                    containerColor = CivicErrorContainer,
                    contentColor = CivicErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ActionInbox.route) }
                )
            }

            // Role-Specific Quick Actions
            Text(
                text = if (isTa) "விரைவு நடவடிக்கைகள்" else "Role Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (currentRole) {
                UserRole.OFFICER -> {
                    Button(
                        onClick = { onNavigate(Screen.ProjectQRGenerator.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTa) "திட்ட QR உருவாக்கு" else "Generate Project QR")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.RegisterProject.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "புதிய திட்டம்" else "Register Project", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigate(Screen.ProgressVerification.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முன்னேற்றம் சரிபார்" else "Verify Progress", fontSize = 12.sp)
                        }
                    }
                }
                UserRole.COORDINATOR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.ConflictDetection.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முரண்பாடு அறி" else "Detect Conflicts", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigate(Screen.SharedSchedule.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "காலஅட்டவணை" else "Shared Schedule", fontSize = 12.sp)
                        }
                    }
                }
                UserRole.CONTRACTOR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.SubmitProgress.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முன்னேற்றம் சமர்ப்பி" else "Submit Progress", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigate(Screen.DeadlineRevision.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "காலக்கெடு திருத்தம்" else "Request Extension", fontSize = 12.sp)
                        }
                    }
                }
                UserRole.ADMINISTRATOR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.UserManagement.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "பயனர் நிர்வாகம்" else "Users Management", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigate(Screen.AuditLog.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "தணிக்கைப் பதிவு" else "Audit Log", fontSize = 12.sp)
                        }
                    }
                }
                UserRole.CITIZEN -> {}
            }

            // Assigned Projects List Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTa) "ஒதுக்கப்பட்ட திட்டங்களின் பட்டியல்" else "Assigned Public Works Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = {
                    val target = when (currentRole) {
                        UserRole.OFFICER -> Screen.OfficerMyProjects.route
                        UserRole.COORDINATOR -> Screen.DepartmentProjects.route
                        UserRole.CONTRACTOR -> Screen.ContractorAssignedProjects.route
                        else -> Screen.PublicProjects.route
                    }
                    onNavigate(target)
                }) {
                    Text(if (isTa) "அனைத்தும்" else "View All")
                }
            }

            projects.take(3).forEach { project ->
                ProjectCard(
                    project = project,
                    onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) }
                )
            }
        }
    }
}
