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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicPrimaryBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val citizenName = CivicDataRepository.currentUser?.name ?: (if (isTa) "குடிமகன்" else "Citizen")
                    Text(
                        text = if (isTa) "வணக்கம், $citizenName!" else "Welcome, $citizenName!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTa) "தமிழ்நாட்டின் உங்கள் பகுதிக்கான அரசு பொதுப்பணி திட்டங்களை கண்காணிக்கவும்." else "Track public works, report issues, and monitor project timelines across Tamil Nadu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
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
                // Search Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTa) "தேடல் முடிவுகள் (${filteredProjects.size})" else "Search Results (${filteredProjects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
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
                            onToggleFollow = { CivicDataRepository.launchOperation { CivicDataRepository.toggleProjectFollow(project.id) } }
                        )
                    }
                }
            } else {
                // Default Overview Mode
                Text(
                    text = if (isTa) "திட்டங்களின் நிலை" else "Project Overview Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CivicTextPrimaryDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (isTa) "நடைபெறுகிறது" else "Ongoing",
                        value = ongoingCount.toString(),
                        icon = Icons.Default.Engineering,
                        containerColor = CivicPrimaryBlue.copy(alpha = 0.12f),
                        contentColor = CivicPrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                    MetricCard(
                        title = if (isTa) "தாமதமாகிறது" else "Delayed",
                        value = delayedCount.toString(),
                        icon = Icons.Default.Warning,
                        containerColor = CivicWarningContainer,
                        contentColor = CivicWarningAmber,
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
                        containerColor = CivicSecondaryBg,
                        contentColor = CivicTextSecondaryGray,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                    MetricCard(
                        title = if (isTa) "நிறைவடைந்தது" else "Completed",
                        value = completedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        containerColor = CivicSuccessContainer,
                        contentColor = CivicSuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.PublicProjects.route) }
                    )
                }

                // Latest Updates Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTa) "அண்மைக்கால புதுப்பிப்புகள்" else "Latest Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )
                }

                // Update 1
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CivicSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CivicSuccessContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CivicSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "அண்ணா சாலை மழைநீர் வடிகால்: மைல்கல் நிறைவு" else "Anna Salai Drain: Milestone Verified",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicTextPrimaryDark
                            )
                            Text(
                                text = if (isTa) "PWD அலுவலரால் 68% முன்னேற்றம் சரிபார்க்கப்பட்டது." else "Milestone 2 verified at 68% completion by PWD Officer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicTextSecondaryGray
                            )
                        }
                    }
                }

                // Update 2
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CivicSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CivicPrimaryBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = CivicPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "கோயம்புத்தூர் TWAD குடிநீர் திட்டம்: குழாய் பதிப்பு" else "Coimbatore Water Supply: Pipe Laying Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicTextPrimaryDark
                            )
                            Text(
                                text = if (isTa) "வார்டு 12-ல் குடிநீர் குழாய் பதிக்கும் பணிகள் நடைபெறுகின்றன." else "Main pipeline installation in progress for Ward 12 circle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicTextSecondaryGray
                            )
                        }
                    }
                }

                // Update 3
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CivicSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CivicWarningContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CivicWarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTa) "மதுரை மேம்பாலம்: காலக்கெடு நீட்டிப்பு" else "Madurai Bridge: Timeline Extension",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicTextPrimaryDark
                            )
                            Text(
                                text = if (isTa) "மின்சார வடங்கள் மாற்றத்திற்காக 30 நாட்கள் நீட்டிக்கப்பட்டுள்ளது." else "30-day utility clearance extension approved by Highways division.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicTextSecondaryGray
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
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )

                    TextButton(onClick = { onNavigate(Screen.PublicProjects.route) }) {
                        Text(if (isTa) "அனைத்தும் காண்க" else "View All", color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }

                filteredProjects.take(3).forEach { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onNavigate(Screen.ProjectDetails.createRoute(project.id)) },
                        onToggleFollow = { CivicDataRepository.launchOperation { CivicDataRepository.toggleProjectFollow(project.id) } }
                    )
                }
            }
        }
    }
}
