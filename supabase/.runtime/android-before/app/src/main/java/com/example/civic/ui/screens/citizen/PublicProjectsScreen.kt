package com.example.civic.ui.screens.citizen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ProjectStatus
import com.example.civic.ui.components.CivicSearchBar
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.components.EmptyStateView
import com.example.civic.ui.components.ProjectCard
import com.example.civic.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProjectsScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All Status") }
    var selectedDeptFilter by remember { mutableStateOf("All Depts") }

    var statusExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }

    val statusFilters = listOf("All Status", "Ongoing", "Planned", "Delayed", "Completed")
    val deptFilters = listOf("All Depts", "Public Works Department", "TWAD Board", "Highways Department", "Municipal Administration")

    val allProjects = CivicDataRepository.projects

    // Filter projects & sort PINNED PROJECTS ON TOP
    val filteredProjects = allProjects.filter { p ->
        val matchesSearch = searchQuery.isBlank() || (
            p.title.contains(searchQuery, ignoreCase = true) ||
            p.location.contains(searchQuery, ignoreCase = true) ||
            p.district.contains(searchQuery, ignoreCase = true) ||
            p.id.contains(searchQuery, ignoreCase = true)
        )

        val matchesStatus = when (selectedStatusFilter) {
            "Ongoing" -> p.status == ProjectStatus.ONGOING
            "Planned" -> p.status == ProjectStatus.PLANNED
            "Delayed" -> p.status == ProjectStatus.DELAYED
            "Completed" -> p.status == ProjectStatus.COMPLETED
            else -> true
        }

        val matchesDept = if (selectedDeptFilter == "All Depts") true else p.department.contains(selectedDeptFilter, ignoreCase = true)

        matchesSearch && matchesStatus && matchesDept
    }.sortedByDescending { it.isFollowed } // Pinned projects appear on top!

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "பொதுத் திட்டங்கள்" else "Public Projects",
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
                placeholder = if (isTa) "திட்டம் பெயர் / ஐடி / இடம் தேடவும்..." else "Search by name, location or project ID..."
            )

            // COMBINED SINGLE ROW DROPDOWN FILTERS (Status & Department side-by-side in 1 row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedStatusFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Filter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusFilters.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    selectedStatusFilter = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Department Filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = deptExpanded,
                    onExpandedChange = { deptExpanded = !deptExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (selectedDeptFilter.length > 12) selectedDeptFilter.take(10) + "..." else selectedDeptFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = deptExpanded,
                        onDismissRequest = { deptExpanded = false }
                    ) {
                        deptFilters.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    selectedDeptFilter = dept
                                    deptExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isTa) "${filteredProjects.size} திட்டங்கள் கண்டறியப்பட்டன (இணைக்கப்பட்டவை மேலே)" else "Found ${filteredProjects.size} projects (Pinned on top)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            if (filteredProjects.isEmpty()) {
                EmptyStateView(
                    title = if (isTa) "திட்டங்கள் ஏதுமில்லை" else "No Projects Found",
                    subtitle = if (isTa) "உங்கள் தேடல் அல்லது வடிகட்டிகளை மாற்ற முயற்சிக்கவும்." else "Try adjusting your search terms or filters."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredProjects,
                        key = { it.id }
                    ) { project ->
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
}
