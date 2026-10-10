package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
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
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.StatusBadge
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun ProjectMapScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val projects = CivicDataRepository.projects
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    val selectedProject = projects.find { it.id == selectedProjectId }

    val initialLocation = selectedProject?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(13.0827, 80.2707) // Default to Chennai, Tamil Nadu

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 11f)
    }

    LaunchedEffect(selectedProjectId) {
        selectedProject?.let { proj ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(proj.latitude, proj.longitude), 12f
            )
        }
    }

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
            // Google Map View Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true,
                        compassEnabled = true
                    )
                ) {
                    projects.forEach { project ->
                        Marker(
                            state = MarkerState(position = LatLng(project.latitude, project.longitude)),
                            title = project.title,
                            snippet = "${project.district} • ${if (isTa) project.status.labelTa else project.status.labelEn}",
                            onClick = {
                                selectedProjectId = project.id
                                false
                            }
                        )
                    }
                }

                // Title Overlay Banner
                Surface(
                    color = CivicSurfaceWhite,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = CivicPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTa) "தமிழ்நாடு அரசு பொதுப்பணி கூகிள் வரைபடம்" else "Tamil Nadu Public Works Google Map",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CivicTextPrimaryDark
                        )
                    }
                }
            }

            // Bottom Selected Project Preview Sheet
            selectedProject?.let { proj ->
                Surface(
                    color = CivicSurfaceWhite,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(status = proj.status)
                            Text(
                                text = proj.district,
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicPrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = proj.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CivicTextPrimaryDark
                        )

                        Text(
                            text = proj.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = CivicTextSecondaryGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigate(Screen.ProjectDetails.createRoute(proj.id)) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTa) "திட்ட விவரங்களைக் காண்க" else "View Project Details", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
