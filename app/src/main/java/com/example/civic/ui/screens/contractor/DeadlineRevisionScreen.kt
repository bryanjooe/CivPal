package com.example.civic.ui.screens.contractor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineRevisionScreen(
    onNavigateBack: () -> Unit,
    onRequestSubmitted: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.assignedProjects

    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var projectExpanded by remember { mutableStateOf(false) }

    var proposedDateInput by remember { mutableStateOf("15 Aug 2025") }
    var reasonInput by remember { mutableStateOf("") }
    var attachedDocName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachedDocName = uri?.toString() }
    var savedRecordId by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "காலக்கெடு திருத்தக் கோரிக்கை" else "Deadline Revision Request",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CivicDataRepository.deadlineRequests.forEach { request ->
                Text("${request.projectId}: ${request.proposedDate} — ${request.status}")
                request.notes?.let { Text(it) }
                request.documentPath?.let { com.example.civic.ui.components.CivicStoredDocument(it) }
            }
            // Important Notice Rule Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isTa) "முக்கிய அறிவிப்பு: சமர்ப்பிக்கப்படும் கோரிக்கை அங்கீகரிக்கப்பட்ட காலக்கெடுவை உடனடியாக மாற்றாது. அரசு அதிகாரி பரிசீலித்து ஒப்புதல் அளித்த பின்னரே மாறும்." else "Important Rule: Submitting a request does NOT automatically change the approved project deadline. The deadline remains unchanged until formal officer approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Project Selector
            ExposedDropdownMenuBox(
                expanded = projectExpanded,
                onExpandedChange = { projectExpanded = !projectExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProject?.title ?: "Select Project",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Contract Project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = projectExpanded,
                    onDismissRequest = { projectExpanded = false }
                ) {
                    projects.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text("${proj.id} - ${proj.title}") },
                            onClick = {
                                selectedProject = proj
                                projectExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = selectedProject?.revisedCompletionDate ?: "30 May 2025",
                onValueChange = {},
                readOnly = true,
                label = { Text("Current Approved Deadline") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = proposedDateInput,
                onValueChange = { proposedDateInput = it },
                label = { Text("Proposed New Deadline") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reasonInput,
                onValueChange = { reasonInput = it },
                label = { Text("Reason for Delay Request") },
                placeholder = { Text("Describe unforeseen field conditions or delays...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // Authorized file attachment
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = attachedDocName?.let { "Selected attachment" } ?: "Attach Supporting Justification Document (PDF)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = { picker.launch("application/pdf") }) {
                        Text("Attach")
                    }
                }
            }

            Button(
                onClick = {
                    if (reasonInput.isNotBlank()) {
                        CivicDataRepository.launchOperation {
                            CivicDataRepository.validateAttachment(context, attachedDocName, pdf = true)
                            val createdId = savedRecordId ?: CivicDataRepository.requestDeadline(
                                checkNotNull(selectedProject) { "Select a project." }.id, proposedDateInput, reasonInput)
                            savedRecordId = createdId
                            CivicDataRepository.uploadAttachment(context, attachedDocName, "deadline-documents", createdId)
                            showSuccessDialog = true
                        }
                    }
                },
                enabled = reasonInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "கோரிக்கையைச் சமர்ப்பி" else "Submit Revision Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Request Submitted to Officer") },
            text = { Text("Your deadline revision request for $proposedDateInput has been logged and sent to the authorized officer. The official approved project deadline remains unchanged until officer review.") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onRequestSubmitted()
                }) {
                    Text("Understood")
                }
            }
        )
    }
}
