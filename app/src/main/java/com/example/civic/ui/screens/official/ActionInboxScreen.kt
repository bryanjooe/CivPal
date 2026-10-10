package com.example.civic.ui.screens.official

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Complaint
import com.example.civic.data.models.ComplaintStatus
import com.example.civic.data.models.ConflictStatus
import com.example.civic.data.models.HandoverStatus
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicSearchBar
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.EmptyStateView
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.screens.officer.OfficialComplaintReviewDialog
import com.example.civic.ui.theme.*

enum class InboxFilter(val labelEn: String, val labelTa: String) {
    ALL("All Actions", "அனைத்தும்"),
    OVERDUE_TASKS("Overdue / Escalated", "காலக்கெடு மீறப்பட்டவை"),
    CITIZEN_COMPLAINTS("Citizen Complaints", "புகார்கள்"),
    SCHEDULE_CONFLICTS("Schedule Conflicts", "முரண்பாடுகள்"),
    PROGRESS_SUBMISSIONS("Progress Verifications", "முன்னேற்றம்"),
    OFFICER_HANDOVERS("Officer Handovers", "ஒப்படைப்பு")
}

data class ActionInboxItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val relatedId: String,
    val date: String,
    val priority: String,
    val isOverdue: Boolean = false,
    val filterType: InboxFilter,
    val icon: ImageVector,
    val targetRoute: String,
    val allowedRoles: List<UserRole>,
    val complaintRef: Complaint? = null
)

@Composable
fun ActionInboxScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val currentUser = CivicDataRepository.currentUser
    val currentRole = currentUser?.role ?: UserRole.OFFICER

    var selectedFilter by remember { mutableStateOf(InboxFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var activeReviewComplaint by remember { mutableStateOf<Complaint?>(null) }

    LaunchedEffect(Unit) {
        CivicDataRepository.evaluateComplaintDeadlines()
    }

    val allInboxItems = remember(CivicDataRepository.projects.toList(), CivicDataRepository.complaints.toList(), CivicDataRepository.conflicts.toList(), CivicDataRepository.handovers.toList()) {
        val list = mutableListOf<ActionInboxItem>()

        // 1. Citizen Complaints Needing Action
        CivicDataRepository.complaints.forEach { c ->
            val isOverdue = c.status == ComplaintStatus.ESCALATED_OVERDUE
            val isUnresolved = c.status != ComplaintStatus.RESOLVED && c.status != ComplaintStatus.REJECTED

            if (isUnresolved) {
                val assignedInfo = if (c.assignedOfficerName != null) "Assigned: ${c.assignedOfficerName}" else "Unassigned — Escalation Required"
                list.add(
                    ActionInboxItem(
                        id = "ACT-CMP-${c.id}",
                        title = if (isOverdue) "OVERDUE ESCALATED: ${c.category.labelEn}" else "Citizen Complaint: ${c.category.labelEn}",
                        subtitle = "7-day response deadline. Location: ${c.location} | $assignedInfo",
                        relatedId = c.id,
                        date = c.submittedDate,
                        priority = if (isOverdue) "High" else "Normal",
                        isOverdue = isOverdue,
                        filterType = if (isOverdue) InboxFilter.OVERDUE_TASKS else InboxFilter.CITIZEN_COMPLAINTS,
                        icon = Icons.Default.ReportProblem,
                        targetRoute = Screen.ComplaintDetails.createRoute(c.id),
                        allowedRoles = listOf(UserRole.OFFICER, UserRole.ADMINISTRATOR),
                        complaintRef = c
                    )
                )
            }
        }

        // 2. Schedule Conflicts
        CivicDataRepository.conflicts.forEach { conf ->
            if (conf.status == ConflictStatus.UNRESOLVED || conf.status == ConflictStatus.UNDER_DISCUSSION) {
                list.add(
                    ActionInboxItem(
                        id = "ACT-CONF-${conf.id}",
                        title = "Schedule Conflict: ${conf.conflictType.labelEn}",
                        subtitle = "${conf.projectATitle} vs ${conf.projectBTitle}. Location: ${conf.location}",
                        relatedId = conf.id,
                        date = "Active",
                        priority = "High",
                        isOverdue = false,
                        filterType = InboxFilter.SCHEDULE_CONFLICTS,
                        icon = Icons.Default.Warning,
                        targetRoute = Screen.ConflictDetection.route,
                        allowedRoles = listOf(UserRole.COORDINATOR, UserRole.OFFICER, UserRole.ADMINISTRATOR)
                    )
                )
            }
        }

        // 3. Officer Handovers
        CivicDataRepository.handovers.forEach { ho ->
            if (ho.status == HandoverStatus.INITIATED || ho.status == HandoverStatus.CLARIFICATION_REQUESTED) {
                list.add(
                    ActionInboxItem(
                        id = "ACT-HO-${ho.id}",
                        title = "Pending Officer Handover (${ho.outgoingOfficerName} ➔ ${ho.incomingOfficerName})",
                        subtitle = "${ho.officeJurisdiction} | ${ho.reasonForReassignment}",
                        relatedId = ho.id,
                        date = ho.effectiveTransferDate,
                        priority = "High",
                        isOverdue = false,
                        filterType = InboxFilter.OFFICER_HANDOVERS,
                        icon = Icons.Default.SwapHoriz,
                        targetRoute = Screen.IncomingOfficer.route,
                        allowedRoles = listOf(UserRole.OFFICER, UserRole.ADMINISTRATOR)
                    )
                )
            }
        }

        // 4. Progress Verification Submissions
        CivicDataRepository.projects.forEach { proj ->
            proj.progressReports.filter { it.status == "Submitted" || it.status == "Pending Verification" }.forEach { pr ->
                list.add(
                    ActionInboxItem(
                        id = "ACT-PR-${pr.id}",
                        title = "Progress Verification: ${proj.title}",
                        subtitle = "Milestone ${pr.milestoneId} - ${pr.workCompleted}",
                        relatedId = proj.id,
                        date = pr.submissionDate,
                        priority = "Normal",
                        isOverdue = false,
                        filterType = InboxFilter.PROGRESS_SUBMISSIONS,
                        icon = Icons.Default.Verified,
                        targetRoute = Screen.ProgressVerification.route,
                        allowedRoles = listOf(UserRole.OFFICER, UserRole.CONTRACTOR, UserRole.ADMINISTRATOR)
                    )
                )
            }
        }

        list
    }

    val filteredItems = remember(allInboxItems, selectedFilter, searchQuery, currentRole) {
        allInboxItems
            .filter { it.allowedRoles.contains(currentRole) }
            .filter { item ->
                val matchesFilter = when (selectedFilter) {
                    InboxFilter.ALL -> true
                    else -> item.filterType == selectedFilter || (selectedFilter == InboxFilter.OVERDUE_TASKS && item.isOverdue)
                }
                val matchesQuery = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.subtitle.contains(searchQuery, ignoreCase = true) ||
                        item.relatedId.contains(searchQuery, ignoreCase = true)

                matchesFilter && matchesQuery
            }
            .sortedByDescending { it.isOverdue }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஸ்மார்ட் நடவடிக்கை பெட்டி" else "Smart Action Inbox",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CivicSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (isTa) "நடவடிக்கை, ID அல்லது தலைப்பை தேடுக..." else "Search action items by ID, title, or project..."
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(InboxFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(if (isTa) filter.labelTa else filter.labelEn, fontSize = 12.sp, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CivicPrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = CivicSurfaceWhite,
                            labelColor = CivicTextPrimaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = CivicSubtleBorder,
                            selectedBorderColor = CivicPrimaryBlue
                        )
                    )
                }
            }

            Text(
                text = if (isTa) "செயல்பாடுகள் (${filteredItems.size})" else "Action Items (${filteredItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
            )

            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    title = if (isTa) "நடவடிக்கைகள் எதுவும் இல்லை" else "No Action Items Found",
                    subtitle = if (isTa) "தேர்ந்தெடுக்கப்பட்ட வகைக்கு எந்த நிலுவை நடவடிக்கையும் இல்லை." else "There are no pending actions matching your filter criteria."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredItems) { item ->
                        ActionInboxCard(
                            item = item,
                            onClick = {
                                if (item.complaintRef != null) {
                                    activeReviewComplaint = item.complaintRef
                                } else {
                                    onNavigate(item.targetRoute)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    activeReviewComplaint?.let { cmp ->
        OfficialComplaintReviewDialog(
            complaint = cmp,
            onDismiss = { activeReviewComplaint = null },
            onResponseSubmitted = {
                activeReviewComplaint = null
                CivicDataRepository.evaluateComplaintDeadlines()
            }
        )
    }
}

@Composable
private fun ActionInboxCard(
    item: ActionInboxItem,
    onClick: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    val priorityColor = if (item.isOverdue) CivicErrorRedRes else when (item.priority) {
        "High" -> CivicErrorRedRes
        "Medium" -> CivicWarningAmber
        else -> CivicPrimaryBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOverdue) CivicErrorContainerRes else CivicSurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (item.isOverdue) CivicErrorRedRes.copy(alpha = 0.3f) else CivicSubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (item.isOverdue) CivicErrorContainerRes else CivicPrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.isOverdue) Icons.Default.Warning else item.icon,
                                contentDescription = null,
                                tint = if (item.isOverdue) CivicErrorRedRes else CivicPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        color = CivicSecondaryBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ID: ${item.relatedId}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            color = CivicDarkBlue
                        )
                    }
                }

                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (item.isOverdue) "OVERDUE" else "${item.priority} Priority",
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isOverdue) CivicErrorRedRes else CivicTextPrimaryDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CivicTextSecondaryGray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Submitted: ${item.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CivicTextSecondaryGray
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isTa) "பரிசீலனை & நடவடிக்கை" else "Review & Action",
                        style = MaterialTheme.typography.labelMedium,
                        color = CivicPrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Action",
                        tint = CivicPrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
