package com.example.civic.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.HandoverStatus
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.navigation.Screen

@Composable
fun IncomingOfficerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProjects: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val handover = CivicDataRepository.handovers.firstOrNull() ?: return

    var showAckDialog by remember { mutableStateOf(false) }
    var showClarifyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "பெறப்பட்ட பணிப்பொறுப்பு" else "Incoming Officer Handover View",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handover Status Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Handover Record: ${handover.id}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = handover.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Outgoing Officer: ${handover.outgoingOfficerName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Incoming Officer: ${handover.incomingOfficerName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Office: ${handover.officeJurisdiction}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Inherited Pending Tasks & Decisions
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTa) "மரபுரிமையாகப் பெறப்பட்ட நிலுவைப் பணிகள்" else "Inherited Pending Tasks & Decisions History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    handover.pendingTasks.forEach { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Project: ${task.relatedProjectId} | Priority: ${task.priority}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            Text(text = "Due: ${task.dueDate}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Handover Notes
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTa) "முந்தைய அலுவலரின் குறிப்புகள்" else "Previous Officer Handover Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = handover.handoverNotes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Continuity Enforcement Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Project Continuity Rules Enforced:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "• Permanent Project IDs (TN-PWD-2025-01) remain unchanged.", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Historical decisions & previous officer delay records are preserved.", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Active responsibility transfers to ${handover.incomingOfficerName} upon acknowledgment.", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Action Buttons
            Button(
                onClick = {
                    CivicDataRepository.acknowledgeHandover(handover.id, handover.incomingOfficerName)
                    showAckDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "ஒப்படைப்பை ஏற்றுக்கொள்ளவும் (Acknowledge)" else "Acknowledge Handover", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showClarifyDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clarification", fontSize = 12.sp)
                }

                Button(
                    onClick = onNavigateToProjects,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Projects", fontSize = 12.sp)
                }
            }
        }
    }

    if (showAckDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Handover Acknowledged Successfully!") },
            text = { Text("Handover ${handover.id} has been acknowledged. Active officer responsibility for projects under ${handover.officeJurisdiction} has been transferred to ${handover.incomingOfficerName}. All permanent project IDs and historical records remain intact.") },
            confirmButton = {
                Button(onClick = {
                    showAckDialog = false
                    onNavigateToProjects()
                }) {
                    Text("View Managed Projects")
                }
            }
        )
    }

    if (showClarifyDialog) {
        AlertDialog(
            onDismissRequest = { showClarifyDialog = false },
            title = { Text("Request Clarification") },
            text = { Text("Clarification request sent to outgoing officer ${handover.outgoingOfficerName}.") },
            confirmButton = {
                Button(onClick = { showClarifyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
