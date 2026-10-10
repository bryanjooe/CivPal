package com.example.civic.ui.screens.citizen

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
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.StatusBadge
import com.example.civic.ui.navigation.Screen

@Composable
fun ProjectDetailsScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToComplaint: (String) -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val project = CivicDataRepository.projects.find { it.id == projectId }

    if (project == null) {
        Scaffold(
            topBar = { CivicTopAppBar(title = "Project Details", canNavigateBack = true, onNavigateBack = onNavigateBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Project not found: $projectId")
            }
        }
        return
    }

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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header Card
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
                        StatusBadge(status = project.status)

                        Button(
                            onClick = { CivicDataRepository.toggleProjectFollow(project.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (project.isFollowed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (project.isFollowed) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (project.isFollowed) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (project.isFollowed) (if (isTa) "பின்பற்றப்படுகிறது" else "Following") else (if (isTa) "பின்பற்று" else "Follow"))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Permanent ID: ${project.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Key Attributes Grid
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isTa) "முக்கிய தகவல்கள்" else "Key Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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

            // Progress & Dates Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTa) "திட்ட முன்னேற்றம் & தேதிகள்" else "Progress & Timeline Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Overall Progress", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${project.progressPercentage}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { project.progressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = if (project.progressPercentage >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(project.startDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Original Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(project.originalCompletionDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Revised Completion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(project.revisedCompletionDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Published Delays Explanation Section
            if (project.delayHistory.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTa) "வெளியிடப்பட்ட தாமத விளக்கம்" else "Official Published Delay Explanation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        project.delayHistory.forEach { delay ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Reason: ${delay.reason}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = delay.publishedExplanation, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Recorded on ${delay.dateRecorded} by ${delay.recordedBy} | Revised completion: ${delay.revisedCompletionDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Milestones Timeline
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTa) "திட்ட மைல்கற்கள்" else "Project Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                                tint = if (m.isCompleted) MaterialTheme.colorScheme.tertiary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = m.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (m.isCompleted) "Completed on ${m.completionDate}" else "Target: ${m.targetDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Button(
                onClick = { onNavigateToComplaint(project.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "இத்திட்டம் குறித்து புகார் அளிக்குக" else "Report an Issue on this Project", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
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
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
