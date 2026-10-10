package com.example.civic.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.User
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar

@Composable
fun CitizenLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var mobileOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDialogText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "குடிமகன் உள்நுழைவு" else "Citizen Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Citizen", isTa = isTa)

            OutlinedTextField(
                value = mobileOrEmail,
                onValueChange = { mobileOrEmail = it },
                label = { Text(if (isTa) "கைபேசி எண் / மின்னஞ்சல்" else "Mobile Number or Email") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isTa) "கடவுச்சொல்" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { showDialogText = "Password reset instructions have been sent to your registered mobile number." }) {
                    Text(if (isTa) "கடவுச்சொல் மறந்ததா?" else "Forgot Password?")
                }

                TextButton(onClick = { showDialogText = "Citizen registration account form opened. Demo accounts can log in directly." }) {
                    Text(if (isTa) "புதிய கணக்கு உருவாக்கு" else "Create Account")
                }
            }

            Button(
                onClick = {
                    CivicDataRepository.currentUser = User(
                        id = "U-CITIZEN",
                        name = "S. Karthik (Citizen)",
                        email = if (mobileOrEmail.contains("@")) mobileOrEmail else "citizen.karthik@gmail.com",
                        role = UserRole.CITIZEN
                    )
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            ElevatedButton(
                onClick = {
                    mobileOrEmail = "9876543210"
                    password = "demoPassword"
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.CITIZEN }
                    onLoginSuccess()
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "மாதிரி உள்நுழைவு (Demo Login)" else "Demo Login as Citizen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    showDialogText?.let { msg ->
        AlertDialog(
            onDismissRequest = { showDialogText = null },
            confirmButton = { Button(onClick = { showDialogText = null }) { Text("OK") } },
            title = { Text("Information") },
            text = { Text(msg) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var selectedDept by remember { mutableStateOf("Public Works Department") }
    var selectedOffice by remember { mutableStateOf("Highways & PWD Chennai Metro Division") }
    var deptExpanded by remember { mutableStateOf(false) }
    var officeExpanded by remember { mutableStateOf(false) }

    val departments = listOf("Public Works Department", "TWAD Board", "Highways Department", "Municipal Administration")
    val offices = listOf("Highways & PWD Chennai Metro Division", "TWAD Water Supply Circle Coimbatore", "Madurai Corporation Engineering Wing", "Tiruchirappalli Highways Circle")

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "அரசு அலுவலர் உள்நுழைவு" else "Officer Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Government Officer", isTa = isTa)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (isTa) "அரசு பயனர் பெயர்" else "Official Username or Email") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isTa) "கடவுச்சொல்" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = deptExpanded,
                onExpandedChange = { deptExpanded = !deptExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDept,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Department") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = deptExpanded,
                    onDismissRequest = { deptExpanded = false }
                ) {
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept) },
                            onClick = {
                                selectedDept = dept
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = officeExpanded,
                onExpandedChange = { officeExpanded = !officeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedOffice,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Office / Jurisdiction") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = officeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = officeExpanded,
                    onDismissRequest = { officeExpanded = false }
                ) {
                    offices.forEach { office ->
                        DropdownMenuItem(
                            text = { Text(office) },
                            onClick = {
                                selectedOffice = office
                                officeExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    CivicDataRepository.currentUser = User(
                        id = "U-OFFICER",
                        name = if (username.isNotBlank()) username else "Er. R. Kanimozhi",
                        email = "officer.kanimozhi@pwd.tn.gov.in",
                        role = UserRole.OFFICER,
                        department = selectedDept,
                        officeJurisdiction = selectedOffice
                    )
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login as Officer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "officer.kanimozhi@pwd.tn.gov.in"
                    password = "demoPassword"
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.OFFICER }
                    onLoginSuccess()
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "மாதிரி உள்நுழைவு (Demo Officer)" else "Demo Login as Officer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinatorLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஒருங்கிணைப்பாளர் உள்நுழைவு" else "Coordinator Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Department Coordinator", isTa = isTa)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Coordinator Username") },
                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.COORDINATOR }
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login as Coordinator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "coord.raman@tn.gov.in"
                    password = "demoPassword"
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.COORDINATOR }
                    onLoginSuccess()
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Demo Login as Coordinator", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContractorLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var contractorId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "ஒப்பந்ததாரர் உள்நுழைவு" else "Contractor Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Contractor", isTa = isTa)

            OutlinedTextField(
                value = contractorId,
                onValueChange = { contractorId = it },
                label = { Text("Contractor License ID or Email") },
                leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.CONTRACTOR }
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Login as Contractor", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    contractorId = "CON-LT-9081"
                    password = "demoPassword"
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.CONTRACTOR }
                    onLoginSuccess()
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Demo Login as Contractor", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "நிர்வாகி உள்நுழைவு" else "Administrator Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Administrator", isTa = isTa)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Admin Username") },
                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.ADMINISTRATOR }
                    onLoginSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Login as Administrator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "admin.civic@tn.gov.in"
                    password = "demoPassword"
                    CivicDataRepository.currentUser = CivicDataRepository.users.find { it.role == UserRole.ADMINISTRATOR }
                    onLoginSuccess()
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Demo Login as Admin", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DemoBanner(roleName: String, isTa: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTa) "$roleName மாதிரி கணக்கு - கீழே உள்ள 'மாதிரி உள்நுழைவு' பொத்தானைப் பயன்படுத்தவும்." else "$roleName Demo Account - Tap 'Demo Login' below for instant access.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
