package com.example.civic.ui.screens.officer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Complaint
import com.example.civic.data.models.ComplaintStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialComplaintReviewDialog(
    complaint: Complaint,
    onDismiss: () -> Unit,
    onResponseSubmitted: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var responseText by remember { mutableStateOf("") }
    var selectedDecision by remember { mutableStateOf(ComplaintStatus.RESOLVED) }
    var decisionExpanded by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val isOverdue = complaint.status == ComplaintStatus.ESCALATED_OVERDUE || now > complaint.deadlineTimestampMillis

    val decisions = listOf(
        ComplaintStatus.RESOLVED,
        ComplaintStatus.ACTION_PROPOSED,
        ComplaintStatus.REJECTED,
        ComplaintStatus.AWAITING_CITIZEN_INFO
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isTa) "புகார் பரிசீலனை & பதில்: ${complaint.id}" else "Official Complaint Review: ${complaint.id}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isOverdue) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "OVERDUE NOTICE: This complaint has exceeded its 7-day deadline. Your response will be recorded in history as a late response.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text("Category: ${complaint.category.labelEn}", fontWeight = FontWeight.SemiBold)
                Text("Location: ${complaint.location}", style = MaterialTheme.typography.bodySmall)
                Text("Citizen Description: ${complaint.description}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(4.dp))

                // Decision Selector
                ExposedDropdownMenuBox(
                    expanded = decisionExpanded,
                    onExpandedChange = { decisionExpanded = !decisionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDecision.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Decision Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = decisionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = decisionExpanded,
                        onDismissRequest = { decisionExpanded = false }
                    ) {
                        decisions.forEach { dec ->
                            DropdownMenuItem(
                                text = { Text(dec.labelEn) },
                                onClick = {
                                    selectedDecision = dec
                                    decisionExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("Written Explanation & Response") },
                    placeholder = { Text("Enter detailed official response explanation...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (responseText.isNotBlank()) {
                        val currentUser = CivicDataRepository.currentUser
                        val officerName = currentUser?.name ?: "Er. R. Kanimozhi"

                        if (selectedDecision == ComplaintStatus.AWAITING_CITIZEN_INFO) {
                            CivicDataRepository.requestCitizenInformation(
                                complaintId = complaint.id,
                                requestText = responseText,
                                officerName = officerName
                            )
                        } else {
                            CivicDataRepository.respondToComplaint(
                                complaintId = complaint.id,
                                decision = selectedDecision,
                                responseText = responseText,
                                officerName = officerName
                            )
                        }
                        onResponseSubmitted()
                    }
                },
                enabled = responseText.isNotBlank()
            ) {
                Text(if (isTa) "பதில் சமர்ப்பி" else "Submit Official Response")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isTa) "ரத்து செய்" else "Cancel")
            }
        }
    )
}
