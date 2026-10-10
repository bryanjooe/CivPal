package com.example.civic.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicSearchBar
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.FilterChipRow
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen

@Composable
fun DepartmentProjectsScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All Depts") }

    val depts = listOf("All Depts", "Public Works Department", "TWAD Board", "Highways Department", "Municipal Administration")
    val projects = CivicDataRepository.managedProjects.filter { p ->
        val matchesDept = if (selectedDept == "All Depts") true else p.department.contains(selectedDept, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || p.title.contains(searchQuery, ignoreCase = true) || p.district.contains(searchQuery, ignoreCase = true)
        matchesDept && matchesSearch
    }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அனைத்து துறை திட்டங்கள்" else "Inter-Department Projects",
                onOpenDrawer = onOpenDrawer
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
                placeholder = if (isTa) "துறை / திட்டம் தேடவும்..." else "Search inter-department works..."
            )

            FilterChipRow(
                options = depts,
                selectedOption = selectedDept,
                onOptionSelected = { selectedDept = it }
            )

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
