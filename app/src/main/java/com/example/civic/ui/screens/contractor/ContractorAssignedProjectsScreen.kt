package com.example.civic.ui.screens.contractor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen

@Composable
fun ContractorAssignedProjectsScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.assignedProjects

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஒப்படைக்கப்பட்ட பணிகள்" else "Assigned Project Contracts",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = if (isTa) "உங்கள் உரிமத்தின்கீழ் ஒதுக்கீடு செய்யப்பட்ட திட்டங்கள்:" else "Contracts allocated under license CON-LT-9081:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project
                    )
                }
            }
        }
    }
}
