package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.clickable
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
import com.example.civic.data.models.ProjectStatus
import com.example.civic.ui.components.CivicSearchBar
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.EmptyStateView
import com.example.civic.ui.components.MetricCard
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

@Composable
fun CitizenDashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var searchQuery by remember { mutableStateOf("") }

    val projects = CivicDataRepository.projects
    val ongoingCount = projects.count { it.status == ProjectStatus.ONGOING }
    val plannedCount = projects.count { it.status == ProjectStatus.PLANNED }
    val delayedCount = projects.count { it.status == ProjectStatus.DELAYED }
    val completedCount = projects.count { it.status == ProjectStatus.COMPLETED }

    val filteredProjects = if (searchQuery.isBlank()) projects else {
        projects.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true) ||
            it.district.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "குடிமகன் முகப்பு" else "Citizen Portal",
                onOpenDrawer = onOpenDrawer,
                roleBadge = if (isTa) "குடிமகன்" else "Citizen"
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
            // Welcome Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTa) "வணக்கம், R. அன்பரசன்!" else "Welcome, Citizen!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTa) "தமிழ்நாட்டின் உங்கள் பகுதிக்கான அரசு பொதுப்பணி திட்டங்களை கண்காணிக்கவும்." else "Track public works, report issues, and monitor project timelines across Tamil Nadu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Search Bar
            CivicSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (isTa) "திட்டம், இடம் அல்லது மாவட்டம் தேடவும்..." else "Search projects by title, district or ID..."
            )

            if (searchQuery.isNotBlank()) {
                // Search Mode: Hide statistics and display matching projects
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTa) "தேடல் முடிவுகள் (${filteredProjects.size})" else "Search Results (${filteredProjects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (filteredProjects.isEmpty()) {
                    EmptyStateView(
                        title = if (isTa) "திட்டங்கள் ஏதுமில்லை" else "No Projects Found",
                        subtitle = if (isTa) "உங்கள் தேடலுக்கு எந்த திட்டமும் கிடைக்கவில்லை." else "No projects match your search query."
                    )
                } else {
                    filteredProjects.forEach { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) },
                            onToggleFollow = { CivicDataRepository.toggleProjectFollow(project.id) }
                        )
                    }
                }
            } else {
                // Default Mode: Show overview statistics, quick actions, notifications & nearby projects
                // Metrics Grid
                Text(
                    text = if (isTa) "திட்டங்களின் நிலை" else "Project Overview Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (isTa) "நடைபெறுகிறது" else "Ongoing",
                        value = ongoingCount.toString(),
                        icon = Icons.Default.Engineering,
                        containerColor = CivicMutedBlueLight,
                        contentColor = CivicNavy,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                    MetricCard(
                        title = if (isTa) "தாமதமாகிறது" else "Delayed",
                        value = delayedCount.toString(),
                        icon = Icons.Default.Warning,
                        containerColor = CivicWarningContainer,
                        contentColor = CivicWarningOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (isTa) "திட்டமிடப்பட்டது" else "Planned",
                        value = plannedCount.toString(),
                        icon = Icons.Default.Schedule,
                        containerColor = CivicSurfaceVariant,
                        contentColor = CivicTextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                    MetricCard(
                        title = if (isTa) "நிறைவடைந்தது" else "Completed",
                        value = completedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        containerColor = CivicAccentGreenContainer,
                        contentColor = CivicNavyDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                }

                // Quick Actions
                Text(
                    text = if (isTa) "விரைவுச் செயல்பாடுகள்" else "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate(Screen.ComplaintSubmission.createRoute(null)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTa) "புகார் அளி" else "Report Issue", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onNavigate(Screen.ProjectMap.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTa) "வரைபடம்" else "Map View", fontSize = 13.sp)
                    }
                }

                // Notification Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "புதிய அறிவிப்பு: அண்ணா சாலை மழைநீர் வடிகால்" else "Latest Update: Anna Salai Drain",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTa) "தாமதத்திற்கான பொதுக் காரணம் வெளியிடப்பட்டுள்ளது." else "Official delay explanation published by Er. R. Kanimozhi (PWD).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Featured / Nearby Projects
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTa) "அண்மையிலுள்ள திட்டங்கள்" else "Nearby Public Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate(Screen.PublicProjects.route) }) {
                        Text(if (isTa) "அனைத்தும் காண்க" else "View All")
                    }
                }

                filteredProjects.take(3).forEach { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) },
                        onToggleFollow = { CivicDataRepository.toggleProjectFollow(project.id) }
                    )
                }
            }
        }
    }
}
