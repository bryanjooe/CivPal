package com.example.civic.ui.screens.officer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ProgressReport
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ConfirmationDialog
import com.example.civic.ui.components.EmptyStateView

@Composable
fun ProgressVerificationScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.projects

    // Collect all pending progress reports
    val pendingReports = projects.flatMap { p ->
        p.progressReports.filter { it.status == "Pending Verification" }.map { p to it }
    }

    var selectedActionReport by remember { mutableStateOf<Pair<Pair<String, String>, Boolean>?>(null) } // ((projectId, reportId), approve)
    var officerNotesInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "முன்னேற்ற சரிபார்ப்பு" else "Progress Verification Queue",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = if (isTa) "ஒப்பந்ததாரர் சமர்ப்பித்த முன்னேற்ற அறிக்கைகள்:" else "Submitted Contractor Reports Awaiting Officer Verification:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (pendingReports.isEmpty()) {
                EmptyStateView(
                    title = if (isTa) "சரிபார்ப்பு நிலுவையில் இல்லை" else "No Pending Verifications",
                    subtitle = if (isTa) "அனைத்து ஒப்பந்ததாரர் அறிக்கைகளும் சரிபார்க்கப்பட்டுள்ளன." else "All submitted contractor progress reports have been verified."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(pendingReports) { (project, report) ->
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
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = report.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = report.submissionDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Project: ${project.title} (${project.id})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Submitted by: ${report.submittedBy}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Claimed Progress: ${report.completionPercentage}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(text = "Work Completed: ${report.workCompleted}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Remaining Work: ${report.remainingWork}", style = MaterialTheme.typography.bodyMedium)

                                if (!report.obstacles.isNull_or_blank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Obstacles: ${report.obstacles}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { selectedActionReport = (project.id to report.id) to true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTa) "ஒப்புதல்" else "Approve", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedActionReport = (project.id to report.id) to false },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTa) "நிராகரி" else "Reject", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedActionReport?.let { (ids, isApprove) ->
        val (projId, repId) = ids
        AlertDialog(
            onDismissRequest = { selectedActionReport = null },
            title = { Text(if (isApprove) "Approve Progress Report" else "Request Correction / Reject") },
            text = {
                Column {
                    Text("Add verification notes or instructions for contractor:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = officerNotesInput,
                        onValueChange = { officerNotesInput = it },
                        label = { Text("Officer Verification Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CivicDataRepository.verifyProgressReport(
                            projectId = projId,
                            reportId = repId,
                            approve = isApprove,
                            officerNotes = officerNotesInput.ifBlank { "Verified by Officer" },
                            officerName = CivicDataRepository.currentUser?.name ?: "Er. Officer"
                        )
                        selectedActionReport = null
                        officerNotesInput = ""
                    }
                ) {
                    Text("Confirm Action")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedActionReport = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
