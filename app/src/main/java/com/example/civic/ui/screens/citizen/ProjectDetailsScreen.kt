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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.StatusBadge
import com.example.civic.ui.theme.*

@Composable
fun ProjectDetailsScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToComplaint: (String) -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    LaunchedEffect(projectId) {
        CivicDataRepository.launchOperation { CivicDataRepository.findProject(projectId) }
    }
    val project = CivicDataRepository.projects.find { it.id == projectId }

    if (project == null) {
        Scaffold(
            topBar = { CivicTopAppBar(title = "Project Details", canNavigateBack = true, onNavigateBack = onNavigateBack) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CivicPaleBlueBg)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (CivicDataRepository.loading) "Loading project details…" else "Project not found or not accessible: $projectId",
                    color = CivicTextSecondaryGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "திட்ட விவரங்கள்" else "Project Details",
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
            // Title Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = project.status)

                        Button(
                            onClick = {
                                CivicDataRepository.launchOperation { CivicDataRepository.toggleProjectFollow(project.id) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (project.isFollowed) CivicSecondaryBg else CivicPrimaryBlue,
                                contentColor = if (project.isFollowed) CivicDarkBlue else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (project.isFollowed) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (project.isFollowed) (if (isTa) "பின்பற்றப்படுகிறது" else "Following") else (if (isTa) "பின்பற்று" else "Follow"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Permanent ID: ${project.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CivicPrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CivicTextSecondaryGray
                    )
                }
            }

            // Key Attributes Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isTa) "முக்கிய தகவல்கள்" else "Key Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    DetailRow(icon = Icons.Default.Business, label = "Department", value = project.department)
                    DetailRow(icon = Icons.Default.LocationOn, label = "Location & District", value = "${project.location}, ${project.district}")
                    DetailRow(icon = Icons.Default.LocationCity, label = "Municipal Body", value = project.municipalBody)
                    DetailRow(icon = Icons.Default.AccountBalance, label = "Responsible Office", value = project.responsibleOffice)
                    DetailRow(icon = Icons.Default.Badge, label = "Officer in Charge", value = project.currentOfficerInCharge)
                    DetailRow(icon = Icons.Default.Engineering, label = "Contractor", value = "${project.contractorName} (${project.contractorId})")
                    DetailRow(icon = Icons.Default.AttachMoney, label = "Budget & Funding", value = "₹${project.estimatedBudgetInLakhs} Lakhs (${project.fundingSource})")
                }
            }

            // Progress & Timeline Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "திட்ட முன்னேற்றம் & தேதிகள்" else "Progress & Timeline Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Overall Progress", style = MaterialTheme.typography.bodyMedium, color = CivicTextSecondaryGray)
                        Text(text = "${project.progressPercentage}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CivicPrimaryBlue)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { project.progressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (project.progressPercentage >= 100) CivicSuccessGreen else CivicPrimaryBlue,
                        trackColor = CivicSecondaryBg,
                        drawStopIndicator = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
                            Text(project.startDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CivicTextPrimaryDark)
                        }
                        Column {
                            Text("Original Target", style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
                            Text(project.originalCompletionDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CivicTextPrimaryDark)
                        }
                        Column {
                            Text("Revised Completion", style = MaterialTheme.typography.labelSmall, color = CivicPrimaryBlue)
                            Text(project.revisedCompletionDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CivicPrimaryBlue)
                        }
                    }
                }
            }

            // Published Delay Explanations
            if (project.delayHistory.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicWarningContainer),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CivicWarningAmber.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CivicWarningAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTa) "வெளியிடப்பட்ட தாமத விளக்கம்" else "Official Published Delay Explanation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicWarningAmber
                            )
                        }

                        project.delayHistory.forEach { delay ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Reason: ${delay.reason}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                            Text(text = delay.publishedExplanation, style = MaterialTheme.typography.bodyMedium, color = CivicTextPrimaryDark)
                            Text(
                                text = "Recorded on ${delay.dateRecorded} by ${delay.recordedBy} | Revised completion: ${delay.revisedCompletionDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTextSecondaryGray
                            )
                        }
                    }
                }
            }

            // Milestones Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "திட்ட மைல்கற்கள்" else "Project Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    project.milestones.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (m.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (m.isCompleted) CivicSuccessGreen else CivicTextSecondaryGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = m.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CivicTextPrimaryDark)
                                Text(
                                    text = if (m.isCompleted) "Completed on ${m.completionDate}" else "Target: ${m.targetDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CivicTextSecondaryGray
                                )
                            }
                        }
                    }
                }
            }

            // Progress Reports & Evidence
            project.progressReports.forEach { report ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CivicSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${report.completionPercentage}% — ${report.status}", fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                        Text(report.workCompleted, color = CivicTextSecondaryGray)
                        report.photoUri?.let { com.example.civic.ui.components.CivicStoredImage("progress-evidence", it) }
                        report.officerNotes?.let { Text(it, color = CivicDarkBlue, fontSize = 13.sp) }
                    }
                }
            }

            // Complaint Action Button
            Button(
                onClick = {
                    if (CivicDataRepository.isGuest) {
                        showLoginRequiredDialog = true
                    } else {
                        onNavigateToComplaint(project.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CivicErrorRedRes, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "இத்திட்டம் குறித்து புகார் அளிக்குக" else "Report an Issue on this Project", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showLoginRequiredDialog = false },
            title = { Text(if (isTa) "குடிமகன் உள்நுழைவு தேவை" else "Citizen Login Required", fontWeight = FontWeight.Bold) },
            text = { Text(if (isTa) "புகாரைப் பதிவு செய்ய உங்கள் குடிமகன் கணக்கில் உள்நுழையவும்." else "Please sign in with your citizen account to submit a complaint on this project.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginRequiredDialog = false
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                ) {
                    Text(if (isTa) "உள்நுழைக" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLoginRequiredDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CivicSubtleBorder)
                ) {
                    Text(if (isTa) "ரத்து செய்" else "Cancel", color = CivicTextSecondaryGray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CivicSurfaceWhite
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = CivicPrimaryBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = CivicPrimaryBlue, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CivicTextPrimaryDark)
        }
    }
}
