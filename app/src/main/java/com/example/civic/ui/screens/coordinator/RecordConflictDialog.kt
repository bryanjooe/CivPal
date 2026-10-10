package com.example.civic.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ConflictType

@Composable
internal fun RecordConflictDialog(onDismiss: () -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConflictType.ROAD_DIGGING_OVERLAP) }
    var location by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    var resolution by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record Schedule Conflict") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(CivicDataRepository.managedProjects.joinToString("\n") { "${it.id}: ${it.title}" })
            OutlinedTextField(a, { a = it }, label = { Text("Project A ID") })
            OutlinedTextField(b, { b = it }, label = { Text("Project B ID") })
            ConflictType.entries.forEach { value ->
                Row { RadioButton(type == value, { type = value }); Text(value.labelEn) }
            }
            OutlinedTextField(location, { location = it }, label = { Text("Conflict Location") })
            OutlinedTextField(dates, { dates = it }, label = { Text("Conflicting Dates") })
            OutlinedTextField(resolution, { resolution = it }, label = { Text("Proposed Resolution (optional)") })
        }
    }, confirmButton = { Button(onClick = { CivicDataRepository.launchOperation {
        CivicDataRepository.recordConflict(a, b, type, location, dates, resolution.ifBlank { null }); onDismiss()
    } }, enabled = a.isNotBlank() && b.isNotBlank() && a != b && location.isNotBlank() && dates.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
