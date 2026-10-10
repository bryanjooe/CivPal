package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.civic.ui.theme.*

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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CivicPrimaryBlue,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = CivicDataRepository.currentUser?.name ?: "Guest Citizen",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CivicTextPrimaryDark
                        )
                        Text(
                            text = CivicDataRepository.currentUser?.email.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CivicTextSecondaryGray
                        )
                        if (!CivicDataRepository.currentUser?.officeJurisdiction.isNullOrBlank()) {
                            Text(
                                text = CivicDataRepository.currentUser?.officeJurisdiction.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicPrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Language Selector Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = CivicPrimaryBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isTa) "செயலி மொழி (Language)" else "Application Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CivicTextPrimaryDark
                            )
                        }

                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = CivicDataRepository.currentLanguage == AppLanguage.ENGLISH,
                                onClick = { CivicDataRepository.setLanguage(AppLanguage.ENGLISH) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                label = { Text("English") }
                            )
                            SegmentedButton(
                                selected = CivicDataRepository.currentLanguage == AppLanguage.TAMIL,
                                onClick = { CivicDataRepository.setLanguage(AppLanguage.TAMIL) },
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
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
            )

            if (followedProjects.isEmpty()) {
                Text(
                    text = if (isTa) "பின்பற்றப்படும் திட்டங்கள் எதுவும் இல்லை." else "No projects followed yet. Tap the pin icon on any project card to save it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CivicTextSecondaryGray
                )
            } else {
                followedProjects.forEach { proj ->
                    ProjectCard(
                        project = proj,
                        onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) },
                        onToggleFollow = { CivicDataRepository.launchOperation { CivicDataRepository.toggleProjectFollow(proj.id) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = CivicErrorRedRes),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "வெளியேறு" else "Logout from CivPal", fontWeight = FontWeight.Bold)
            }
        }
    }
}
