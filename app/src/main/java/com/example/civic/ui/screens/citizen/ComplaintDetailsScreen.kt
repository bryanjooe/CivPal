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
import com.example.civic.data.models.ComplaintStatus
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ComplaintStatusBadge
import com.example.civic.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ComplaintDetailsScreen(
    complaintId: String,
    onNavigateBack: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val complaint = CivicDataRepository.complaints.find { it.id == complaintId }
    var reply by remember(complaintId) { mutableStateOf("") }

    if (complaint == null) {
        Scaffold(
            topBar = { CivicTopAppBar(title = "Complaint Details", canNavigateBack = true, onNavigateBack = onNavigateBack) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CivicPaleBlueBg)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Complaint not found", color = CivicTextSecondaryGray, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val now = System.currentTimeMillis()
    val isOverdue = complaint.status == ComplaintStatus.ESCALATED_OVERDUE || now > complaint.deadlineTimestampMillis

    // 7-Day Window Calculations
    val totalWindowMillis = 7 * 24 * 60 * 60 * 1000L
    val elapsedMillis = (now - complaint.submissionTimestampMillis).coerceIn(0L, totalWindowMillis)
    val progressFraction = if (isOverdue) 1.0f else (elapsedMillis.toFloat() / totalWindowMillis.toFloat()).coerceIn(0f, 1f)

    val deadlineDateStr = remember(complaint.deadlineTimestampMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(complaint.deadlineTimestampMillis))
    }

    val daysOverdue = if (isOverdue) maxOf(1L, (now - complaint.deadlineTimestampMillis) / (24 * 60 * 60 * 1000L)) else 0L

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "புகார் விவரங்கள்" else "Complaint Details",
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
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = complaint.id,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CivicPrimaryBlue,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        ComplaintStatusBadge(status = complaint.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTa) complaint.category.labelTa else complaint.category.labelEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CivicPrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${complaint.location}, ${complaint.district}", style = MaterialTheme.typography.bodyMedium, color = CivicTextSecondaryGray)
                    }
                }
            }

            // REFINED 7-DAY RESPONSE DEADLINE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isOverdue) CivicErrorRedRes.copy(alpha = 0.3f) else CivicSubtleBorder
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isOverdue) CivicErrorRedRes else CivicPrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTa) "7-நாள் பதிலளிப்பு காலக்கெடு" else "Mandatory 7-Day Response Deadline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CivicTextPrimaryDark
                            )
                        }

                        Surface(
                            color = if (isOverdue) CivicErrorContainerRes else CivicSecondaryBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isOverdue) "Overdue ($daysOverdue days)" else "${(progressFraction * 100).toInt()}% elapsed",
                                color = if (isOverdue) CivicErrorRedRes else CivicDarkBlue,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (isOverdue) CivicErrorRedRes else CivicPrimaryBlue,
                        trackColor = CivicSecondaryBg,
                        drawStopIndicator = {}
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Submitted: ${complaint.submittedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicTextSecondaryGray
                        )

                        Text(
                            text = "Deadline: $deadlineDateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicTextSecondaryGray
                        )
                    }

                    if (isOverdue) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CivicSubtleBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isTa)
                                "இப்புகாரிற்கான அதிகாரப்பூர்வ 7-நாள் பதிலளிப்பு காலக்கெடு மீறப்பட்டுள்ளது. இப்புகார் முன்னுரிமை நடவடிக்கைகளுக்காக மேலதிகாரிகளுக்கு உயர்த்தப்பட்டுள்ளது. அதன் தற்போதைய நிலையை இங்கு தொடர்ந்து கண்காணிக்கலாம்."
                            else
                                "The official response deadline for this complaint has passed. The complaint has been escalated for priority attention. You can continue to track its status and any official updates here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CivicTextSecondaryGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Related Specifications Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isTa) "தொடர்புடைய திட்டம் & துறை விவரங்கள்" else "Related Project & Department Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    if (complaint.projectTitle != null) {
                        DetailItem(icon = Icons.Default.Folder, label = "Related Project", value = complaint.projectTitle)
                    }
                    if (complaint.projectId != null) {
                        DetailItem(icon = Icons.Default.Badge, label = "Project Permanent ID", value = complaint.projectId)
                    }
                    DetailItem(icon = Icons.Default.Business, label = "Assigned Department", value = complaint.assignedDepartment)
                    DetailItem(icon = Icons.Default.Person, label = "Assigned Officer", value = complaint.assignedOfficerName ?: "Unassigned — Escalation Required")
                    DetailItem(icon = Icons.Default.Schedule, label = "Submission Date", value = complaint.submittedDate)
                    DetailItem(icon = Icons.Default.Update, label = "Last Update Date", value = complaint.lastUpdatedDate)
                }
            }

            // Description & Attachments
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "புகார் விளக்கம்" else "Issue Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = complaint.description, style = MaterialTheme.typography.bodyMedium, color = CivicTextSecondaryGray)

                    if (complaint.photoUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = CivicSecondaryBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = CivicPrimaryBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Attached Photo Proof: ${complaint.photoUri}", style = MaterialTheme.typography.bodyMedium, color = CivicTextPrimaryDark)
                            }
                        }
                        com.example.civic.ui.components.CivicStoredImage("complaint-photos", complaint.photoUri)
                    }
                }
            }

            // Citizen Reply Section
            if (complaint.status == ComplaintStatus.AWAITING_CITIZEN_INFO &&
                CivicDataRepository.currentUser?.role == com.example.civic.data.models.UserRole.CITIZEN) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text(if (isTa) "கூடுதல் தகவல்" else "Additional information") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { CivicDataRepository.launchOperation { CivicDataRepository.replyComplaint(complaint.id, reply); reply = "" } },
                    enabled = reply.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isTa) "பதில் அனுப்பு" else "Send reply", fontWeight = FontWeight.Bold)
                }
            }

            // Chronological Event History Timeline
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "காலவரிசை நிகழ்வு தணிக்கைப் பதிவு" else "Chronological Event Audit Trail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (complaint.eventHistory.isEmpty()) {
                        Text(
                            text = "Initial submission event logged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CivicTextSecondaryGray
                        )
                    } else {
                        complaint.eventHistory.forEach { evt ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (evt.eventType.contains("Escalated")) Icons.Default.Warning else Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (evt.eventType.contains("Escalated")) CivicErrorRedRes else CivicPrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = evt.eventType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                                        Text(text = evt.dateString, style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = evt.description, style = MaterialTheme.typography.bodySmall, color = CivicTextSecondaryGray)
                                    Text(text = "By: ${evt.performedBy}", style = MaterialTheme.typography.labelSmall, color = CivicDarkBlue)
                                }
                            }
                            HorizontalDivider(color = CivicSubtleBorder)
                        }
                    }
                }
            }

            // Official Responses List
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "அதிகாரி பதில்கள்" else "Officer Recorded Responses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (complaint.responses.isEmpty()) {
                        Text(
                            text = if (isTa) "இன்னும் பதில்கள் பதிவு செய்யப்படவில்லை. அதிகாரிகள் ஆய்வு செய்கின்றனர்." else "No official response recorded yet. Assigned officer is reviewing the complaint.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CivicTextSecondaryGray
                        )
                    } else {
                        complaint.responses.forEach { resp ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CivicSecondaryBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = resp.respondedBy, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                                        Text(text = resp.date, style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = resp.message, style = MaterialTheme.typography.bodyMedium, color = CivicTextPrimaryDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
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
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = CivicTextSecondaryGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CivicTextPrimaryDark)
    }
}
