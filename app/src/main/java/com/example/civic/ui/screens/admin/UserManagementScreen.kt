package com.example.civic.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun UserManagementScreen(
    onOpenDrawer: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val users = CivicDataRepository.users
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<com.example.civic.data.models.User?>(null) }

    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "பயனர்கள் நிர்வாகம்" else "User Management",
                onOpenDrawer = onOpenDrawer
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddUserDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White) },
                text = { Text(if (isTa) "பயனரை அழைக்கவும்" else "Invite User", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                containerColor = com.example.civic.ui.theme.CivicPrimaryBlue,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
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
                text = if (isTa) "பதிவுசெய்யப்பட்ட பயனர்கள் மற்றும் பங்குகள்:" else "Registered System Users & Role Privileges:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(users) { user ->
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
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = if (isTa) user.role.labelTa else user.role.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (user.officeJurisdiction != null) {
                                Text(
                                    text = "Office: ${user.officeJurisdiction}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingUser = user },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Role", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { CivicDataRepository.launchOperation { CivicDataRepository.setUser(user, active = !user.isActive) } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (user.isActive) "Disable + clear scopes" else "Enable", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text(if (isTa) "பயனரை அழைக்கவும்" else "Invite New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isTa) "மின்னஞ்சல் உறுதிப்படுத்திய பிறகு பங்கு மற்றும் அதிகார வரம்பை அமைக்கவும்." else "An email invitation creates a citizen account. Assign its role and scope after email confirmation.")
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                    CivicDataRepository.launchOperation {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            CivicDataRepository.addUser(
                                name = newName,
                                email = newEmail,
                                role = UserRole.CITIZEN,
                                department = null,
                                jurisdiction = null
                            )
                            showAddUserDialog = false
                            newName = ""
                            newEmail = ""
                        }
                    
                    }
                }
                ) {
                    Text(if (isTa) "அழைப்பு அனுப்பு" else "Send invitation")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    editingUser?.let { user ->
        UserAuthorizationDialog(user, onDismiss = { editingUser = null })
    }
}
