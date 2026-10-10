package com.example.civic.ui.screens.official

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.civic.data.models.ProjectStatus
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
    val priority: String, // High, Medium, Low
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

    // Run deadline evaluation on launch
    LaunchedEffect(Unit) {
        CivicDataRepository.evaluateComplaintDeadlines()
    }

    // Synthesize Action Items from Repository
    val allInboxItems = remember(CivicDataRepository.projects.size, CivicDataRepository.complaints.size, CivicDataRepository.conflicts.size, CivicDataRepository.handovers.size) {
        val list = mutableListOf<ActionInboxItem>()

        // 1. Citizen Complaints Needing Action (Overdue + Pending)
        CivicDataRepository.complaints.forEach { c ->
            val isOverdue = c.status == ComplaintStatus.ESCALATED_OVERDUE
            val isUnresolved = c.status != ComplaintStatus.RESOLVED && c.status != ComplaintStatus.REJECTED

            if (isUnresolved) {
                val assignedInfo = if (c.assignedOfficerName != null) "Assigned: ${c.assignedOfficerName}" else "Unassigned — Escalation Required"
                list.add(
                    ActionInboxItem(
                        id = "ACT-CMP-${c.id}",
                        title = if (isOverdue) "OVERDUE ESCALATED: ${c.category.labelEn}" else "Citizen Complaint: ${c.category.labelEn}",
                        subtitle = "This complaint has exceeded its 7-day response deadline. Immediate action is required. Location: ${c.location} | $assignedInfo",
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

    // Role-filtered inbox items & SORT OVERDUE TASKS ON TOP
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
            .sortedByDescending { it.isOverdue } // OVERDUE TASKS SORTED ON TOP!
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            CivicSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (isTa) "நடவடிக்கை, ID அல்லது தலைப்பை தேடுக..." else "Search action items by ID, title, or project..."
            )

            // Category Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(InboxFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(if (isTa) filter.labelTa else filter.labelEn, fontSize = 12.sp) }
                    )
                }
            }

            Text(
                text = if (isTa) "செயல்பாடுகள் (${filteredItems.size}) - காலக்கெடு மீறப்பட்டவை மேலே" else "Action Items (${filteredItems.size}) — Overdue Sorted First",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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

    val priorityColor = if (item.isOverdue) CivicErrorRed else when (item.priority) {
        "High" -> CivicErrorRed
        "Medium" -> CivicWarningOrange
        else -> CivicNavy
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOverdue) CivicErrorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (item.isOverdue) CivicErrorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.isOverdue) Icons.Default.Warning else item.icon,
                                contentDescription = null,
                                tint = if (item.isOverdue) CivicErrorRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "ID: ${item.relatedId}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = if (item.isOverdue) "OVERDUE — 7-Day Deadline Passed" else "${item.priority} Priority",
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
                color = if (item.isOverdue) CivicErrorRed else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isTa) "பரிசீலனை & நடவடிக்கை" else "Review & Action",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
