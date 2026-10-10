package com.example.civic.ui.screens.officer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterProjectScreen(
    onNavigateBack: () -> Unit,
    onProjectRegistered: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(CivicDataRepository.currentUser?.department.orEmpty()) }
    var deptExpanded by remember { mutableStateOf(false) }

    var district by remember { mutableStateOf("Chennai") }
    var municipalBody by remember { mutableStateOf("Greater Chennai Corporation") }
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var responsibleOffice by remember { mutableStateOf(CivicDataRepository.currentUser?.officeJurisdiction.orEmpty()) }
    var contractorName by remember { mutableStateOf(CivicDataRepository.contractors.firstOrNull { it.active }?.name.orEmpty()) }
    var budgetLakhs by remember { mutableStateOf("350") }
    var fundingSource by remember { mutableStateOf("TN Urban Infrastructure Grant") }
    var startDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var completionDate by remember { mutableStateOf(java.time.LocalDate.now().plusMonths(6).toString()) }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val departments = CivicDataRepository.departments.filter { it.active }.map { it.name }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "புதிய திட்டம் பதிவு" else "Register New Project",
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
                text = if (isTa) "அரசு திட்டப் பதிவுப் படிவம்:" else "Government Public Work Registration Form:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Project Title") },
                placeholder = { Text("e.g. Stormwater Drain Resurfacing") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Project Scope & Description") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            ExposedDropdownMenuBox(
                expanded = deptExpanded,
                onExpandedChange = { deptExpanded = !deptExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDept,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Department") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = deptExpanded,
                    onDismissRequest = { deptExpanded = false }
                ) {
                    departments.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d) },
                            onClick = {
                                selectedDept = d
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = municipalBody,
                    onValueChange = { municipalBody = it },
                    label = { Text("Municipal Body") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location / Landmark") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = latitude, onValueChange = { latitude = it },
                    label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = longitude, onValueChange = { longitude = it },
                    label = { Text("Longitude") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = responsibleOffice, onValueChange = { responsibleOffice = it },
                label = { Text("Registered Responsible Office") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = contractorName,
                onValueChange = { contractorName = it },
                label = { Text("Assigned Contractor") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = budgetLakhs,
                    onValueChange = { budgetLakhs = it },
                    label = { Text("Budget (₹ Lakhs)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fundingSource,
                    onValueChange = { fundingSource = it },
                    label = { Text("Funding Scheme") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = completionDate,
                    onValueChange = { completionDate = it },
                    label = { Text("Completion Date") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showPreviewDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }

                Button(
                    onClick = {
                    CivicDataRepository.launchOperation {
                        if (title.isNotBlank() && location.isNotBlank()) {
                            CivicDataRepository.registerProject(
                                title = title,
                                description = description,
                                department = selectedDept,
                                district = district,
                                municipalBody = municipalBody,
                                location = location,
                                contractorName = contractorName,
                                responsibleOffice = responsibleOffice,
                                officerInCharge = CivicDataRepository.currentUser?.name ?: "Er. Officer",
                                budgetLakhs = budgetLakhs.toDoubleOrNull() ?: 100.0,
                                fundingSource = fundingSource,
                                startDate = startDate,
                                completionDate = completionDate,
                                latitude = requireNotNull(latitude.toDoubleOrNull()) { "Enter a valid latitude." },
                                longitude = requireNotNull(longitude.toDoubleOrNull()) { "Enter a valid longitude." }
                            )
                            showSuccessDialog = true
                        }
                    
                    }
                },
                    enabled = title.isNotBlank() && location.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit")
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Project Preview") },
            text = {
                Column {
                    Text("Title: $title", fontWeight = FontWeight.Bold)
                    Text("Dept: $selectedDept")
                    Text("Location: $location, $district")
                    Text("Budget: ₹$budgetLakhs Lakhs ($fundingSource)")
                    Text("Dates: $startDate to $completionDate")
                }
            },
            confirmButton = { Button(onClick = { showPreviewDialog = false }) { Text("Close Preview") } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Project Registered Successfully!") },
            text = { Text("The public work project has been added to the portal and permanent project ID generated.") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onProjectRegistered()
                }) {
                    Text("View Projects")
                }
            }
        )
    }
}
