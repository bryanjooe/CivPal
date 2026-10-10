package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen

@Composable
fun CitizenProfileScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val followedProjects = CivicDataRepository.projects.filter { it.isFollowed }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "சுயவிவரம் & அமைப்புகள்" else "Profile & Settings",
                onOpenDrawer = onOpenDrawer
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
            // Profile Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "R. Anbarasan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "citizen.anbarasan@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "District: Chennai | Ward 118",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Language Selector Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isTa) "செயலி மொழி (Language)" else "Application Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = CivicDataRepository.currentLanguage == AppLanguage.ENGLISH,
                                onClick = { CivicDataRepository.currentLanguage = AppLanguage.ENGLISH },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                label = { Text("English") }
                            )
                            SegmentedButton(
                                selected = CivicDataRepository.currentLanguage == AppLanguage.TAMIL,
                                onClick = { CivicDataRepository.currentLanguage = AppLanguage.TAMIL },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                label = { Text("தமிழ்") }
                            )
                        }
                    }
                }
            }

            // Followed Projects Section
            Text(
                text = if (isTa) "பின்பற்றப்படும் திட்டங்கள் (${followedProjects.size})" else "Followed / Saved Projects (${followedProjects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (followedProjects.isEmpty()) {
                Text(
                    text = if (isTa) "பின்பற்றப்படும் திட்டங்கள் எதுவும் இல்லை." else "No projects followed yet. Tap the star icon on any project card to follow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                followedProjects.forEach { proj ->
                    ProjectCard(
                        project = proj,
                        onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) },
                        onToggleFollow = { CivicDataRepository.toggleProjectFollow(proj.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "வெளியேறு" else "Logout from CivicTech", fontWeight = FontWeight.Bold)
            }
        }
    }
}
