package com.example.civic.ui.screens.officer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Project
import com.example.civic.ui.components.CivicSearchBar
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen

@Composable
fun OfficerMyProjectsScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var searchQuery by remember { mutableStateOf("") }
    var selectedProjectForDelay by remember { mutableStateOf<Project?>(null) }

    val projects = CivicDataRepository.projects.filter {
        searchQuery.isBlank() || (
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        )
    }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "எனது திட்டங்கள் நிர்வாகம்" else "Officer Managed Projects",
                onOpenDrawer = onOpenDrawer
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.RegisterProject.route) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (isTa) "புதிய திட்டம்" else "Register Project") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CivicSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (isTa) "திட்டம் தேடவும்..." else "Search assigned projects..."
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(projects) { project ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ProjectCard(
                                project = project,
                                onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { selectedProjectForDelay = project },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTa) "தாமதம் பதிவு" else "Record Delay")
                                }

                                Button(
                                    onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) }
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTa) "வரலாறு" else "View Details")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProjectForDelay?.let { project ->
        RecordDelayDialog(
            project = project,
            onDismiss = { selectedProjectForDelay = null },
            onDelayRecorded = { selectedProjectForDelay = null }
        )
    }
}
