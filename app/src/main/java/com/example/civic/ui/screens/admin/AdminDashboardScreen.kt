package com.example.civic.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val usersCount = CivicDataRepository.users.size
    val projectsCount = CivicDataRepository.projects.size
    val auditLogsCount = CivicDataRepository.auditLogs.size

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "நிர்வாகி முகப்பு" else "System Administrator",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) "நிர்வாகி" else "Admin"
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
            // Welcome Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "System Admin (Tamil Nadu HQ)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tamil Nadu E-Governance Agency & CivPal Portal Management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Metrics
            Text(
                text = if (isTa) "அமைப்பு புள்ளிவிவரங்கள்" else "System Performance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "பயனர்கள்" else "Registered Users",
                    value = usersCount.toString(),
                    icon = Icons.Default.People,
                    containerColor = CivicMutedBlueLight,
                    contentColor = CivicNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.UserManagement.route) }
                )
                MetricCard(
                    title = if (isTa) "திட்டங்கள்" else "Total Projects",
                    value = projectsCount.toString(),
                    icon = Icons.Default.Folder,
                    containerColor = CivicAccentGreenContainer,
                    contentColor = CivicNavyDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.PublicProjects.route) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = if (isTa) "தணிக்கைப் பதிவுகள்" else "Audit Log Entries",
                    value = auditLogsCount.toString(),
                    icon = Icons.Default.ReceiptLong,
                    containerColor = CivicSurfaceVariant,
                    contentColor = CivicTextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AuditLog.route) }
                )
                MetricCard(
                    title = if (isTa) "துறைகள்" else "Departments",
                    value = CivicDataRepository.departments.count { it.active }.toString(),
                    icon = Icons.Default.Business,
                    containerColor = CivicWarningContainer,
                    contentColor = CivicWarningOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.DepartmentManagement.route) }
                )
            }

            // Admin Actions List
            Text(
                text = if (isTa) "நிர்வாகக் கட்டுப்பாடுகள்" else "Administrator Control Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = { onNavigate(Screen.UserManagement.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.People, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "பயனர் நிர்வாகம்" else "User Management & Role Assignments")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.HandoverDashboard.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "அலுவலர் ஒப்படைப்பு தொடர்ச்சி" else "Officer Handover Continuity Module")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.DepartmentManagement.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Business, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "அரசுத் துறை நிர்வாகம்" else "Department & Jurisdiction Setup")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.AuditLog.route) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "அமைப்பு தணிக்கைப் பதிவு" else "System Audit Log")
            }
        }
    }
}
