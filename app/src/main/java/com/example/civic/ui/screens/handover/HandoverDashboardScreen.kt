package com.example.civic.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun HandoverDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val handovers = CivicDataRepository.handovers

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அலுவலர் ஒப்படைப்பு & தொடர்ச்சி" else "Officer Handover Module",
                onOpenDrawer = onOpenDrawer
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(com.example.civic.ui.navigation.Screen.InitiateHandover.route) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White) },
                text = { Text(if (isTa) "புதிய ஒப்படைப்பு தொடங்கு" else "Initiate Handover", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                containerColor = com.example.civic.ui.theme.CivicPrimaryBlue,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Guarantee Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isTa) "திட்டத் தொடர்ச்சி விதி: அலுவலர்கள் மாறினாலும் திட்டக் குறியீடு (ID), வரலாறு மற்றும் நிலுவைப் பணிகள் நிரந்தர அலுவலகப் பதிவேட்டில் பாதுகாக்கப்படும்." else "Project Continuity Guarantee: Permanent project IDs, decision history, and pending tasks are strictly preserved on officer transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isTa) "அலுவலர் பணி ஒப்படைப்புகள்:" else "Officer Reassignments & Handover Records:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(handovers) { handover ->
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
                                Text(
                                    text = handover.id,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    color = if (handover.status == HandoverStatus.ACKNOWLEDGED) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = if (isTa) handover.status.labelTa else handover.status.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${handover.department} | ${handover.officeJurisdiction}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Outgoing Officer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = handover.outgoingOfficerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(text = "Incoming Officer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = handover.incomingOfficerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Assigned Projects: ${handover.assignedProjectsCount}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Pending Tasks: ${handover.pendingTasksCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onNavigate(Screen.IncomingOfficer.route) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isTa) "ஒப்படைப்பு விவரங்களைக் காண்க / ஏற்றுக்கொள்" else "View & Acknowledge Handover")
                            }
                        }
                    }
                }
            }
        }
    }
}
