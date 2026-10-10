package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ComplaintCategory
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ConfirmationDialog
import com.example.civic.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintSubmissionScreen(
    prefilledProjectId: String? = null,
    onNavigateBack: () -> Unit,
    onComplaintSubmitted: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.projects

    var selectedCategory by remember { mutableStateOf(ComplaintCategory.POTHOLE) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedProject by remember {
        mutableStateOf(projects.find { it.id == prefilledProjectId } ?: projects.firstOrNull())
    }
    var projectExpanded by remember { mutableStateOf(false) }

    var locationInput by remember { mutableStateOf(selectedProject?.location ?: "") }
    var districtInput by remember { mutableStateOf(selectedProject?.district ?: "Chennai") }
    var descriptionInput by remember { mutableStateOf("") }
    var attachedPhotoName by remember { mutableStateOf<String?>(null) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var submittedComplaintId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "புகார் பதிவிடவும்" else "Submit Public Complaint",
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
            Text(
                text = if (isTa) "பொதுப்பணி தொடர்பான புகாரை சமர்ப்பிக்கவும்:" else "File a complaint or query regarding public works:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = if (isTa) selectedCategory.labelTa else selectedCategory.labelEn,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isTa) "புகார் வகை" else "Complaint Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    ComplaintCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(if (isTa) category.labelTa else category.labelEn) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Related Project Selector
            ExposedDropdownMenuBox(
                expanded = projectExpanded,
                onExpandedChange = { projectExpanded = !projectExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProject?.title ?: "Select Project (Optional)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isTa) "தொடர்புடைய திட்டம்" else "Related Project (Optional)") },
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
                                locationInput = proj.location
                                districtInput = proj.district
                                projectExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text(if (isTa) "இடம் / தெரு பெயர்" else "Location / Landmark") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text(if (isTa) "புகார் விவரம்" else "Description of Issue") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            // Photo Attachment Mock
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
                        Column {
                            Text(
                                text = attachedPhotoName ?: (if (isTa) "புகைப்படம் இணைக்கவும் (சான்று)" else "Attach Site Photo"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "JPEG, PNG up to 5MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { attachedPhotoName = "site_photo_proof_${(100..999).random()}.jpg" }
                    ) {
                        Text(if (attachedPhotoName == null) "Select" else "Change")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (descriptionInput.isNotBlank() && locationInput.isNotBlank()) {
                        val created = CivicDataRepository.submitComplaint(
                            category = selectedCategory,
                            projectId = selectedProject?.id,
                            projectTitle = selectedProject?.title,
                            location = locationInput,
                            district = districtInput,
                            description = descriptionInput,
                            photoUri = attachedPhotoName,
                            citizenName = "S. Karthik"
                        )
                        submittedComplaintId = created.id
                        showConfirmDialog = true
                    }
                },
                enabled = descriptionInput.isNotBlank() && locationInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "புகாரைச் சமர்ப்பி" else "Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isTa) "ரத்து செய்" else "Cancel")
            }
        }
    }

    if (showConfirmDialog && submittedComplaintId != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (isTa) "புகார் பதிவு செய்யப்பட்டது!" else "Complaint Submitted Successfully!") },
            text = {
                Column {
                    Text(if (isTa) "உங்கள் புகார் பதிவு எண்:" else "Your tracking Complaint ID:")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = submittedComplaintId!!,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (isTa) "அதிகாரிகள் உங்கள் புகாரை பரிசீலனை செய்வர்." else "The assigned department officer will review your complaint shortly.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    onComplaintSubmitted()
                }) {
                    Text("Track Complaints")
                }
            }
        )
    }
}
