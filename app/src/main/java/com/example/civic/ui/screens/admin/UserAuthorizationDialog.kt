package com.example.civic.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.*

@Composable
internal fun UserAuthorizationDialog(user: User, onDismiss: () -> Unit) {
    val ids = CivicDataRepository.profileIds(user)
    var role by remember(user.id) { mutableStateOf(user.role) }
    var department by remember(user.id) { mutableStateOf(ids.first) }
    var office by remember(user.id) { mutableStateOf(ids.second) }
    var contractor by remember(user.id) { mutableStateOf(ids.third) }
    var scopeDepartment by remember(user.id) { mutableStateOf(ids.first) }
    var scopeJurisdiction by remember(user.id) { mutableStateOf(ids.second) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Authorization: ${user.name}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("User UUID: ${user.id}")
            Text("Changing role or disabling an account clears approved scopes. Grant scopes explicitly after activating an officer/coordinator.")
            UserRole.entries.forEach { value ->
                Row {
                    RadioButton(selected = role == value, onClick = { role = value })
                    Text(value.labelEn)
                }
            }
            OutlinedTextField(department, { department = it }, label = { Text("Department ID (optional)") })
            Text(CivicDataRepository.departments.joinToString { "${it.id}: ${it.name}" }, style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(office, { office = it }, label = { Text("Office ID (optional)") })
            Text(CivicDataRepository.jurisdictions.joinToString { "${it.id}: ${it.name}" }, style = MaterialTheme.typography.labelSmall)
            if (role == UserRole.CONTRACTOR) {
                OutlinedTextField(contractor, { contractor = it }, label = { Text("Contractor ID (required)") })
                Text(CivicDataRepository.contractors.joinToString { "${it.id}: ${it.name}" }, style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider()
            Text("Approved department/jurisdiction pair")
            OutlinedTextField(scopeDepartment, { scopeDepartment = it }, label = { Text("Scope Department ID") })
            OutlinedTextField(scopeJurisdiction, { scopeJurisdiction = it }, label = { Text("Scope Jurisdiction ID") })
            Row {
                TextButton(onClick = { CivicDataRepository.launchOperation {
                    CivicDataRepository.grantScope(user, scopeDepartment, scopeJurisdiction, true)
                } }) { Text("Grant Scope") }
                TextButton(onClick = { CivicDataRepository.launchOperation {
                    CivicDataRepository.grantScope(user, scopeDepartment, scopeJurisdiction, false)
                } }) { Text("Revoke Scope") }
            }
        }
    }, confirmButton = {
        Button(onClick = { CivicDataRepository.launchOperation {
            CivicDataRepository.authorizeUser(user, role, department.ifBlank { null }, office.ifBlank { null },
                if (role == UserRole.CONTRACTOR) contractor.ifBlank { null } else null)
            onDismiss()
        } }, enabled = user.id != CivicDataRepository.currentUser?.id) { Text("Save Role") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
