package com.example.civic.ui.screens.contractor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
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
fun SubmitProgressScreen(
    onNavigateBack: () -> Unit,
    onProgressSubmitted: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.assignedProjects

    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var projectExpanded by remember { mutableStateOf(false) }

    var selectedMilestone by remember { mutableStateOf(selectedProject?.milestones?.firstOrNull()?.title ?: "Milestone 1") }
    var milestoneExpanded by remember { mutableStateOf(false) }

    var completionPercentage by remember { mutableFloatStateOf(75f) }
    var workCompletedInput by remember { mutableStateOf("") }
    var remainingWorkInput by remember { mutableStateOf("") }
    var obstaclesInput by remember { mutableStateOf("") }
    var attachedPhotoName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachedPhotoName = uri?.toString() }
    var savedRecordId by remember { mutableStateOf<String?>(null) }

    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "பணி முன்னேற்றம் சமர்ப்பி" else "Submit Progress Report",
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
            Text(
                text = if (isTa) "அதிகாரி சரிபார்ப்பிற்காக பணி முன்னேற்றம் சமர்ப்பிக்கவும்:" else "Submit verified milestone progress to officer in charge:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Project Selector
            ExposedDropdownMenuBox(
                expanded = projectExpanded,
                onExpandedChange = { projectExpanded = !projectExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProject?.title ?: "Select Project",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Project") },
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
                                selectedMilestone = proj.milestones.firstOrNull()?.title ?: "Milestone 1"
                                projectExpanded = false
                            }
                        )
                    }
                }
            }

            // Milestone Selector
            ExposedDropdownMenuBox(
                expanded = milestoneExpanded,
                onExpandedChange = { milestoneExpanded = !milestoneExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMilestone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Milestone Target") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = milestoneExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = milestoneExpanded,
                    onDismissRequest = { milestoneExpanded = false }
                ) {
                    selectedProject?.milestones?.forEach { ms ->
                        DropdownMenuItem(
                            text = { Text("${ms.title} (Target: ${ms.targetDate})") },
                            onClick = {
                                selectedMilestone = ms.title
                                milestoneExpanded = false
                            }
                        )
                    }
                }
            }

            // Completion Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Claimed Work Progress", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${completionPercentage.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = completionPercentage,
                        onValueChange = { completionPercentage = it },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                }
            }

            OutlinedTextField(
                value = workCompletedInput,
                onValueChange = { workCompletedInput = it },
                label = { Text("Work Completed Description") },
                placeholder = { Text("e.g., Completed 1.5 km box culvert laying and concrete curing") },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                maxLines = 3
            )

            OutlinedTextField(
                value = remainingWorkInput,
                onValueChange = { remainingWorkInput = it },
                label = { Text("Remaining Scope of Work") },
                placeholder = { Text("e.g., Bituminous road overlay and pavement link") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = obstaclesInput,
                onValueChange = { obstaclesInput = it },
                label = { Text("Obstacles or Delays Encountered (Optional)") },
                modifier = Modifier.fillMaxWidth()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = attachedPhotoName?.let { "Selected attachment" } ?: "Attach Site Verification Photo", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = { picker.launch("image/*") }) {
                        Text("Attach")
                    }
                }
            }

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                    if (selectedProject != null && workCompletedInput.isNotBlank()) {
                        CivicDataRepository.validateAttachment(context, attachedPhotoName)
                        val createdId = savedRecordId ?: CivicDataRepository.submitProgressReport(
                            projectId = selectedProject!!.id,
                            milestoneId = checkNotNull(selectedProject?.milestones?.find { it.title == selectedMilestone }) { "Select a valid milestone." }.id,
                            submittedBy = "L&T Contractor Supervisor",
                            completionPct = completionPercentage.toInt(),
                            workDone = workCompletedInput,
                            remainingWork = remainingWorkInput.ifBlank { "As scheduled" },
                            obstacles = obstaclesInput.ifBlank { null }
                        )
                        savedRecordId = createdId
                        CivicDataRepository.uploadAttachment(context, attachedPhotoName, "progress-evidence", createdId)
                        showConfirmDialog = true
                    }
                
                    }
                },
                enabled = selectedProject != null && workCompletedInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "அதிகாரி சரிபார்ப்பிற்கு சமர்ப்பி" else "Submit for Verification", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Progress Report Submitted!") },
            text = { Text("Your progress report has been submitted to the authorized government officer for site verification.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    onProgressSubmitted()
                }) {
                    Text("OK")
                }
            }
        )
    }
}
