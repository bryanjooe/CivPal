package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Project
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.components.StatusBadge
import com.example.civic.ui.navigation.Screen

@Composable
fun ProjectMapScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.projects
    var selectedProject by remember { mutableStateOf<Project?>(projects.firstOrNull()) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "திட்டங்களின் வரைபடம்" else "Public Projects Map",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE2E8F0)), // Map light background
                contentAlignment = Alignment.Center
            ) {
                // Fictional Tamil Nadu Map Canvas representation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines simulating map grid
                    val gridStep = 80f
                    var x = 0f
                    while (x < width) {
                        drawLine(Color(0xFFCBD5E1), Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                        x += gridStep
                    }
                    var y = 0f
                    while (y < height) {
                        drawLine(Color(0xFFCBD5E1), Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                        y += gridStep
                    }
                }

                // Title Overlay Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTa) "தமிழ்நாடு அரசு பொதுப்பணி வரைபடம்" else "Tamil Nadu Public Works GIS Map",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Map Markers overlay
                val markerPositions = listOf(
                    Offset(0.7f, 0.25f) to projects.getOrNull(0), // Chennai
                    Offset(0.25f, 0.55f) to projects.getOrNull(1), // Coimbatore
                    Offset(0.45f, 0.75f) to projects.getOrNull(2), // Madurai
                    Offset(0.52f, 0.50f) to projects.getOrNull(3), // Trichy
                    Offset(0.38f, 0.40f) to projects.getOrNull(4)  // Salem
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight

                    markerPositions.forEach { (relPos, proj) ->
                        if (proj != null) {
                            val isSelected = selectedProject?.id == proj.id
                            IconButton(
                                onClick = { selectedProject = proj },
                                modifier = Modifier
                                    .offset(x = w * relPos.x - 20.dp, y = h * relPos.y - 20.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = proj.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(if (isSelected) 36.dp else 28.dp)
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.9f),
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = proj.district,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Selected Project Preview Sheet
            selectedProject?.let { proj ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(status = proj.status)
                            Text(
                                text = proj.district,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = proj.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = proj.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTa) "திட்ட விவரங்களைக் காண்க" else "View Project Details")
                        }
                    }
                }
            }
        }
    }
}
