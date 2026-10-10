package com.example.civic.ui.screens.officer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Project

@Composable
fun RecordDelayDialog(
    project: Project,
    onDismiss: () -> Unit,
    onDelayRecorded: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var reasonInput by remember { mutableStateOf("") }
    var revisedDateInput by remember { mutableStateOf("30 Aug 2025") }
    var explanationInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isTa) "தாமதத்தைப் பதிவு செய்க: ${project.id}" else "Record Official Delay: ${project.id}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Project: ${project.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Internal Reason for Delay") },
                    placeholder = { Text("e.g., Unseasonal rain, utility relocation") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = revisedDateInput,
                    onValueChange = { revisedDateInput = it },
                    label = { Text("Revised Expected Completion Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = explanationInput,
                    onValueChange = { explanationInput = it },
                    label = { Text("Published Public Explanation for Citizens") },
                    placeholder = { Text("Explanation visible on public portal") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reasonInput.isNotBlank() && explanationInput.isNotBlank()) {
                        CivicDataRepository.recordDelay(
                            projectId = project.id,
                            reason = reasonInput,
                            revisedDate = revisedDateInput,
                            explanation = explanationInput,
                            officerName = CivicDataRepository.currentUser?.name ?: "Er. Officer"
                        )
                        onDelayRecorded()
                    }
                },
                enabled = reasonInput.isNotBlank() && explanationInput.isNotBlank()
            ) {
                Text(if (isTa) "பதிவு செய்" else "Publish & Update Status")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isTa) "ரத்து செய்" else "Cancel")
            }
        }
    )
}
