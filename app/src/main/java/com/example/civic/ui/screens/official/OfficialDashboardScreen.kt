package com.example.civic.ui.screens.official

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val projects = CivicDataRepository.managedProjects
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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicPrimaryBlue),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
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
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = CivicPrimaryBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = totalPendingActions.toString(),
                                    color = CivicPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "ஸ்மார்ட் நடவடிக்கை பெட்டி" else "Smart Action Inbox",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CivicTextPrimaryDark
                            )
                            Text(
                                text = if (isTa) "$totalPendingActions நடவடிக்கைகளுக்கான எச்சரிக்கைகள் நிலுவையில் உள்ளன" else "$totalPendingActions pending action items requiring attention",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicTextSecondaryGray
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Inbox",
                        tint = CivicPrimaryBlue
                    )
                }
            }

            // Overview Metrics Grid
            Text(
                text = if (isTa) "பணி நிலை கண்ணோட்டம்" else "Workspace Summary & Action Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "ஒதுக்கப்பட்டவை" else "Assigned Projects",
                    value = totalAssignedProjects.toString(),
                    icon = Icons.Default.Folder,
                    containerColor = CivicPrimaryBlue.copy(alpha = 0.12f),
                    contentColor = CivicPrimaryBlue,
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
                    contentColor = CivicWarningAmber,
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
                    containerColor = CivicSecondaryBg,
                    contentColor = CivicTextSecondaryGray,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ConflictDetection.route) }
                )
                MetricCard(
                    title = if (isTa) "புகார்கள் திறனாய்வு" else "Complaints Review",
                    value = pendingComplaintsCount.toString(),
                    icon = Icons.Default.ReportProblem,
                    containerColor = CivicErrorContainerRes,
                    contentColor = CivicErrorRedRes,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ActionInbox.route) }
                )
            }

            // Role-Specific Quick Actions
            Text(
                text = if (isTa) "விரைவு நடவடிக்கைகள்" else "Role Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
            )

            when (currentRole) {
                UserRole.OFFICER -> {
                    Button(
                        onClick = { onNavigate(Screen.ProjectQRGenerator.route) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTa) "திட்ட QR உருவாக்கு" else "Generate Project QR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.RegisterProject.route) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "புதிய திட்டம்" else "Register Project", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigate(Screen.ProgressVerification.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicDarkBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முன்னேற்றம் சரிபார்" else "Verify Progress", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முரண்பாடு அறி" else "Detect Conflicts", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigate(Screen.SharedSchedule.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicDarkBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "காலஅட்டவணை" else "Shared Schedule", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "முன்னேற்றம் சமர்ப்பி" else "Submit Progress", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigate(Screen.DeadlineRevision.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicDarkBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "காலக்கெடு திருத்தம்" else "Request Extension", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "பயனர் நிர்வாகம்" else "Users Management", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigate(Screen.AuditLog.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicDarkBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTa) "தணிக்கைப் பதிவு" else "Audit Log", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                UserRole.CITIZEN -> {}
            }

            // Assigned Projects Preview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTa) "ஒதுக்கப்பட்ட திட்டங்களின் பட்டியல்" else "Assigned Public Works Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CivicTextPrimaryDark
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
                    Text(if (isTa) "அனைத்தும்" else "View All", color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
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
