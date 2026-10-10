package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ComplaintStatus
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ComplaintCard
import com.example.civic.ui.components.EmptyStateView
import com.example.civic.ui.components.FilterChipRow
import com.example.civic.ui.navigation.Screen

@Composable
fun MyComplaintsScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Pending", "Due Soon", "Overdue", "Responded")

    LaunchedEffect(Unit) {
        CivicDataRepository.evaluateComplaintDeadlines()
    }

    val now = System.currentTimeMillis()
    val allComplaints = CivicDataRepository.complaints

    val filteredComplaints = allComplaints.filter { c ->
        val diffMillis = c.deadlineTimestampMillis - now
        val isOverdue = c.status == ComplaintStatus.ESCALATED_OVERDUE || now > c.deadlineTimestampMillis
        val isDueSoon = !isOverdue && diffMillis > 0 && diffMillis < (2 * 24 * 60 * 60 * 1000L) // Less than 2 days remaining

        when (selectedFilter) {
            "Pending" -> c.status == ComplaintStatus.PENDING || c.status == ComplaintStatus.UNDER_REVIEW
            "Due Soon" -> isDueSoon
            "Overdue" -> isOverdue
            "Responded" -> c.status == ComplaintStatus.RESOLVED || c.status == ComplaintStatus.REJECTED || c.status == ComplaintStatus.ACTION_PROPOSED || c.status == ComplaintStatus.AWAITING_CITIZEN_INFO
            else -> true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "எனது புகார்கள்" else "My Complaints & Trackers",
                onOpenDrawer = onOpenDrawer
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.ComplaintSubmission.createRoute(null)) },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Complaint") },
                text = { Text(if (isTa) "புதிய புகார்" else "New Complaint") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            // Filter Chips
            FilterChipRow(
                options = filterOptions,
                selectedOption = selectedFilter,
                onOptionSelected = { selectedFilter = it }
            )

            Text(
                text = if (isTa) "சமர்ப்பிக்கப்பட்ட புகார்கள் (${filteredComplaints.size}):" else "Submitted Complaints & Status Trackers (${filteredComplaints.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (filteredComplaints.isEmpty()) {
                EmptyStateView(
                    title = if (isTa) "புகார்கள் ஏதுமில்லை" else "No Complaints Found",
                    subtitle = if (isTa) "தேர்ந்தெடுக்கப்பட்ட வடிகட்டிக்கு புகார்கள் இல்லை." else "There are no complaints matching the selected filter."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredComplaints) { complaint ->
                        ComplaintCard(
                            complaint = complaint,
                            onClick = { onNavigate(Screen.ComplaintDetails.createRoute(complaint.id)) }
                        )
                    }
                }
            }
        }
    }
}
