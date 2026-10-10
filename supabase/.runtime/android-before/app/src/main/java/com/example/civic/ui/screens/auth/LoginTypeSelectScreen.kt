package com.example.civic.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun LoginTypeSelectScreen(
    onNavigateBack: () -> Unit,
    onSelectRole: (UserRole) -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அதிகாரப்பூர்வ உள்நுழைவு" else "Official Sign In",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTa) "டெமோ உள்நுழைவு முறை" else "Demo Authentication Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isTa) "அதிகாரப்பூர்வ தளத்தை அனுபவிக்க கீழே உள்ள உங்கள் பணிக்கான கணக்கை தேர்வு செய்யவும்."
                            else "Select an official role below to access the unified Official Workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Text(
                text = if (isTa) "அதிகாரப்பூர்வ பணிக்கான கணக்கு தேர்வு:" else "Select your official role to proceed:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            RoleCard(
                role = UserRole.OFFICER,
                titleEn = "Government / Municipal Officer",
                titleTa = "அரசு / நகராட்சி அலுவலர்",
                descEn = "Register projects, verify contractor progress, record delays, and manage handovers.",
                descTa = "திட்டங்களைப் பதிவு செய்யவும், முன்னேற்றத்தைச் சரிபார்க்கவும்.",
                icon = Icons.Default.Badge,
                onClick = { onSelectRole(UserRole.OFFICER) }
            )

            RoleCard(
                role = UserRole.COORDINATOR,
                titleEn = "Department Coordinator",
                titleTa = "துறை ஒருங்கிணைப்பாளர்",
                descEn = "Cross-departmental schedule tracking, inter-work conflict resolution & reports.",
                descTa = "துறைசார் திட்ட முரண்பாடுகளைக் கண்டறிந்து தீர்க்கவும்.",
                icon = Icons.Default.AccountTree,
                onClick = { onSelectRole(UserRole.COORDINATOR) }
            )

            RoleCard(
                role = UserRole.CONTRACTOR,
                titleEn = "Authorized Contractor",
                titleTa = "ஒப்பந்ததாரர் உள்நுழைவு",
                descEn = "Submit milestone progress reports, site photos, and deadline revision requests.",
                descTa = "பணி முன்னேற்ற அறிக்கைகள் மற்றும் புகைப்படங்களைச் சமர்ப்பிக்கவும்.",
                icon = Icons.Default.Engineering,
                onClick = { onSelectRole(UserRole.CONTRACTOR) }
            )

            RoleCard(
                role = UserRole.ADMINISTRATOR,
                titleEn = "System Administrator",
                titleTa = "அமைப்பு நிர்வாகி",
                descEn = "Manage users, departments, office jurisdictions, and audit log monitoring.",
                descTa = "பயனர்கள், துறைகள் மற்றும் தணிக்கைப் பதிவுகளை நிர்வகிக்கவும்.",
                icon = Icons.Default.AdminPanelSettings,
                onClick = { onSelectRole(UserRole.ADMINISTRATOR) }
            )
        }
    }
}

@Composable
private fun RoleCard(
    role: UserRole,
    titleEn: String,
    titleTa: String,
    descEn: String,
    descTa: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTa) titleTa else titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isTa) descTa else descEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
