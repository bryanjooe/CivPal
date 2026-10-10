package com.example.civic.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.theme.*

data class BottomNavItem(
    val titleEn: String,
    val titleTa: String,
    val screen: Screen,
    val icon: ImageVector
)

@Composable
fun CivicBottomNavigation(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    val items = listOf(
        BottomNavItem("Home", "முகப்பு", Screen.CitizenDashboard, Icons.Default.Home),
        BottomNavItem("Projects", "திட்டங்கள்", Screen.PublicProjects, Icons.Default.List),
        BottomNavItem("Scan", "ஸ்கேன்", Screen.ProjectQRScanner, Icons.Default.QrCodeScanner),
        BottomNavItem("Map", "வரைபடம்", Screen.ProjectMap, Icons.Default.Map),
        BottomNavItem("Complaints", "புகார்கள்", Screen.MyComplaints, Icons.Default.ReportProblem)
    )

    NavigationBar(
        containerColor = CivicSurfaceWhite,
        contentColor = CivicPrimaryBlue,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            if (item.screen == Screen.ProjectQRScanner) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FloatingActionButton(
                        onClick = { onNavigate(item.screen) },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        containerColor = CivicPrimaryBlue,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = if (isTa) item.titleTa else item.titleEn,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (isTa) item.titleTa else item.titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = CivicPrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            } else {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.screen) },
                    icon = { Icon(item.icon, contentDescription = if (isTa) item.titleTa else item.titleEn) },
                    label = { Text(if (isTa) item.titleTa else item.titleEn, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CivicPrimaryBlue,
                        selectedTextColor = CivicPrimaryBlue,
                        unselectedIconColor = CivicTextSecondaryGray,
                        unselectedTextColor = CivicTextSecondaryGray,
                        indicatorColor = CivicPrimaryBlue.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}
