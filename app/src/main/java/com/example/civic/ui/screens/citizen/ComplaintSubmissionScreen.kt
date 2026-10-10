package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

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
    var municipalityExpanded by remember { mutableStateOf(false) }

    var locationInput by remember { mutableStateOf(selectedProject?.location ?: "") }
    var districtInput by remember { mutableStateOf(selectedProject?.district ?: "Chennai") }
    var descriptionInput by remember { mutableStateOf("") }
    var attachedPhotoName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachedPhotoName = uri?.toString() }
    var savedRecordId by remember { mutableStateOf<String?>(null) }

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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isTa) "பொதுப்பணி தொடர்பான புகாரை சமர்ப்பிக்கவும்:" else "File a complaint or query regarding public works:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CivicSurfaceWhite,
                        focusedContainerColor = CivicSurfaceWhite,
                        unfocusedBorderColor = CivicSubtleBorder
                    )
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CivicSurfaceWhite,
                        focusedContainerColor = CivicSurfaceWhite,
                        unfocusedBorderColor = CivicSubtleBorder
                    )
                )
                ExposedDropdownMenu(
                    expanded = projectExpanded,
                    onDismissRequest = { projectExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isTa) "திட்டம் இல்லாத புகார்" else "No related project") },
                        onClick = { selectedProject = null; districtInput = ""; projectExpanded = false }
                    )
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

            if (selectedProject == null) {
                ExposedDropdownMenuBox(expanded = municipalityExpanded,
                    onExpandedChange = { municipalityExpanded = !municipalityExpanded }) {
                    OutlinedTextField(
                        value = districtInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isTa) "நகராட்சி" else "Municipality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(municipalityExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = CivicSurfaceWhite,
                            focusedContainerColor = CivicSurfaceWhite,
                            unfocusedBorderColor = CivicSubtleBorder
                        )
                    )
                    ExposedDropdownMenu(expanded = municipalityExpanded, onDismissRequest = { municipalityExpanded = false }) {
                        CivicDataRepository.jurisdictions.filter { it.active && it.kind == "MUNICIPALITY" }.forEach { municipality ->
                            DropdownMenuItem(text = { Text(municipality.name) }, onClick = {
                                districtInput = municipality.name; municipalityExpanded = false
                            })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text(if (isTa) "இடம் / தெரு பெயர்" else "Location / Landmark") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CivicSurfaceWhite,
                    focusedContainerColor = CivicSurfaceWhite,
                    unfocusedBorderColor = CivicSubtleBorder
                )
            )

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text(if (isTa) "புகார் விவரம்" else "Description of Issue") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CivicSurfaceWhite,
                    focusedContainerColor = CivicSurfaceWhite,
                    unfocusedBorderColor = CivicSubtleBorder
                )
            )

            // File attachment card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CivicPrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = CivicPrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = attachedPhotoName?.let { "Selected attachment" } ?: (if (isTa) "புகைப்படம் இணைக்கவும் (சான்று)" else "Attach Site Photo"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CivicTextPrimaryDark
                            )
                            Text(
                                text = "JPEG, PNG, WebP up to 10MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTextSecondaryGray
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CivicSubtleBorder)
                    ) {
                        Text(if (attachedPhotoName == null) "Select" else "Change", color = CivicPrimaryBlue, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                        if (descriptionInput.isNotBlank() && locationInput.isNotBlank()) {
                            CivicDataRepository.validateAttachment(context, attachedPhotoName)
                            val createdId = savedRecordId ?: CivicDataRepository.submitComplaint(
                                category = selectedCategory,
                                projectId = selectedProject?.id,
                                projectTitle = selectedProject?.title,
                                location = locationInput,
                                district = districtInput,
                                description = descriptionInput,
                                photoUri = attachedPhotoName,
                                citizenName = CivicDataRepository.currentUser?.name.orEmpty()
                            ).id
                            savedRecordId = createdId
                            CivicDataRepository.uploadAttachment(context, attachedPhotoName, "complaint-photos", createdId)
                            submittedComplaintId = createdId
                            showConfirmDialog = true
                        }
                    }
                },
                enabled = descriptionInput.isNotBlank() && locationInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "புகாரைச் சமர்ப்பி" else "Submit Complaint", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Text(if (isTa) "ரத்து செய்" else "Cancel", color = CivicTextSecondaryGray)
            }
        }
    }

    if (showConfirmDialog && submittedComplaintId != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (isTa) "புகார் பதிவு செய்யப்பட்டது!" else "Complaint Submitted Successfully!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(if (isTa) "உங்கள் புகார் பதிவு எண்:" else "Your tracking Complaint ID:", color = CivicTextSecondaryGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = submittedComplaintId!!,
                        style = MaterialTheme.typography.titleLarge,
                        color = CivicPrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (isTa) "அதிகாரிகள் உங்கள் புகாரை பரிசீலனை செய்வர்." else "The assigned department officer will review your complaint shortly.", color = CivicTextSecondaryGray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onComplaintSubmitted()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                ) {
                    Text("Track Complaints", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CivicSurfaceWhite
        )
    }
}
