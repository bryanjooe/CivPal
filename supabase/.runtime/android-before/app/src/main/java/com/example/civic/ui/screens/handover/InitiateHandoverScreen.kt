package com.example.civic.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun InitiateHandoverScreen(
    onNavigateBack: () -> Unit,
    onHandoverSubmitted: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    var outgoingName by remember { mutableStateOf("Er. S. Sundaram") }
    var outgoingEmail by remember { mutableStateOf("officer.sundaram@pwd.tn.gov.in") }

    var incomingName by remember { mutableStateOf("Er. R. Kanimozhi") }
    var incomingEmail by remember { mutableStateOf("officer.kanimozhi@pwd.tn.gov.in") }

    var department by remember { mutableStateOf("Public Works Department") }
    var jurisdiction by remember { mutableStateOf("Highways & PWD Chennai Metro Division") }
    var transferDate by remember { mutableStateOf("01 Apr 2025") }
    var reason by remember { mutableStateOf("Routine Departmental Transfer") }
    var notes by remember { mutableStateOf("Project TN-PWD-2025-01 is 68% complete. Box culverts laid on Anna Salai.") }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "பணி ஒப்படைப்பு தொடங்கு" else "Initiate Officer Handover",
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
                text = if (isTa) "பணி மாற்ற ஒப்படைப்புப் படிவம்:" else "Officer Reassignment & Project Handover Form:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = outgoingName,
                onValueChange = { outgoingName = it },
                label = { Text("Outgoing Officer Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = incomingName,
                onValueChange = { incomingName = it },
                label = { Text("Incoming Officer Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = jurisdiction,
                onValueChange = { jurisdiction = it },
                label = { Text("Office Jurisdiction") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = transferDate,
                    onValueChange = { transferDate = it },
                    label = { Text("Effective Transfer Date") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Reassignment") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Pending Tasks & Handover Notes") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // Auto Generate Button
            OutlinedButton(
                onClick = {
                    notes = "Auto-generated Handover Summary: 2 Active Projects attached to $jurisdiction. Pending Verifications: PR-101. Unresolved Complaints: CMP-2026-102."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isTa) "தானியங்கி ஒப்படைப்புச் சுருக்கம் உருவாக்கு" else "Generate Auto Handover Summary")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showPreviewDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }

                Button(
                    onClick = {
                        if (outgoingName.isNotBlank() && incomingName.isNotBlank()) {
                            CivicDataRepository.initiateHandover(
                                outgoingName = outgoingName,
                                outgoingEmail = outgoingEmail,
                                incomingName = incomingName,
                                incomingEmail = incomingEmail,
                                department = department,
                                jurisdiction = jurisdiction,
                                transferDate = transferDate,
                                reason = reason,
                                notes = notes
                            )
                            showSuccessDialog = true
                        }
                    },
                    enabled = outgoingName.isNotBlank() && incomingName.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit")
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Handover Summary Preview") },
            text = {
                Column {
                    Text("Outgoing Officer: $outgoingName", fontWeight = FontWeight.Bold)
                    Text("Incoming Officer: $incomingName", fontWeight = FontWeight.Bold)
                    Text("Office: $jurisdiction")
                    Text("Transfer Date: $transferDate")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Notes: $notes")
                }
            },
            confirmButton = { Button(onClick = { showPreviewDialog = false }) { Text("Close Preview") } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Handover Initiated Successfully!") },
            text = { Text("The handover record has been generated. The incoming officer $incomingName can now view and acknowledge the handover.") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onHandoverSubmitted()
                }) {
                    Text("Go to Handover Dashboard")
                }
            }
        )
    }
}
