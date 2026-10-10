package com.example.civic.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ScheduleConflict
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.EmptyStateView
import com.example.civic.ui.theme.CivicWarningContainer
import com.example.civic.ui.theme.CivicWarningOrange

@Composable
fun ConflictDetectionScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val conflicts = CivicDataRepository.conflicts
    var selectedConflictForResolution by remember { mutableStateOf<ScheduleConflict?>(null) }
    var resolutionInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "முரண்பாடுகள் கண்டறிதல்" else "Conflict Detection System",
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
                text = if (isTa) "கண்டறியப்பட்ட துறைசார் திட்ட மோதல்கள்:" else "Detected Schedule & Utility Overlap Conflicts:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (conflicts.isEmpty()) {
                EmptyStateView(
                    title = if (isTa) "முரண்பாடுகள் ஏதுமில்லை" else "No Schedule Conflicts",
                    subtitle = if (isTa) "அனைத்து துறை திட்ட அட்டவணைகளும் சீராக உள்ளன." else "No inter-departmental schedule overlaps detected across registered projects."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(conflicts) { conflict ->
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
                                        color = CivicWarningContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = conflict.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CivicWarningOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (isTa) conflict.status.labelTa else conflict.status.labelEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isTa) conflict.conflictType.labelTa else conflict.conflictType.labelEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Location: ${conflict.location}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Conflicting Dates: ${conflict.conflictingDates}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "1. ${conflict.departmentA}: ${conflict.projectATitle} (${conflict.projectAId})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "2. ${conflict.departmentB}: ${conflict.projectBTitle} (${conflict.projectBId})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (conflict.proposedResolution != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Proposed Agreement: ${conflict.proposedResolution}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedConflictForResolution = conflict },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTa) "காலஅட்டவணை மாற்று" else "Propose New Schedule", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { selectedConflictForResolution = conflict },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTa) "தீர்வு பதிவு" else "Record Resolution", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedConflictForResolution?.let { conflict ->
        AlertDialog(
            onDismissRequest = { selectedConflictForResolution = null },
            title = { Text("Resolve Conflict ${conflict.id}") },
            text = {
                Column {
                    Text("Enter joint resolution agreement between ${conflict.departmentA} and ${conflict.departmentB}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resolutionInput,
                        onValueChange = { resolutionInput = it },
                        label = { Text("Resolution Agreement Details") },
                        placeholder = { Text("e.g. TWAD agrees to defer trench excavation until PWD completes culverts") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resolutionInput.isNotBlank()) {
                            CivicDataRepository.resolveConflict(
                                conflictId = conflict.id,
                                resolutionText = resolutionInput,
                                coordinatorName = CivicDataRepository.currentUser?.name ?: "Dr. Coordinator"
                            )
                            selectedConflictForResolution = null
                            resolutionInput = ""
                        }
                    },
                    enabled = resolutionInput.isNotBlank()
                ) {
                    Text("Save Resolution")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedConflictForResolution = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
