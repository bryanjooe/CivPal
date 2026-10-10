package com.example.civic.ui.screens.admin

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

@Composable
fun DepartmentManagementScreen(
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val depts = listOf(
        "Public Works Department (PWD)" to "Highways, Buildings, Water Bodies",
        "TWAD Board" to "Tamil Nadu Water Supply & Drainage Board",
        "Highways Department" to "State Highways, Major District Roads",
        "Municipal Administration & Water Supply" to "Corporations, Municipalities, Panchayats",
        "TANGEDCO" to "Electricity Board Power Line Relocation"
    )

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அரசுத் துறைகள் நிர்வாகம்" else "Department Management",
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
                text = if (isTa) "பதிவுசெய்யப்பட்ட தமிழ்நாட்டு அரசுத் துறைகள்:" else "Registered Tamil Nadu Government Public Work Departments:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(depts) { (name, scope) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Scope: $scope", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
