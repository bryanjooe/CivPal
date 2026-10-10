package com.example.civic.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.theme.*

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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Citizen", isTa = isTa)

            OutlinedTextField(
                value = mobileOrEmail,
                onValueChange = { mobileOrEmail = it },
                label = { Text(if (isTa) "கைபேசி எண் / மின்னஞ்சல்" else "Email Address") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isTa) "கடவுச்சொல்" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CivicPrimaryBlue) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { CivicDataRepository.launchOperation { CivicDataRepository.resetPassword(mobileOrEmail); showDialogText = "If this email is registered, reset instructions have been sent." } }) {
                    Text(if (isTa) "கடவுச்சொல் மறந்ததா?" else "Forgot Password?", color = CivicPrimaryBlue)
                }

                TextButton(onClick = { CivicDataRepository.launchOperation { CivicDataRepository.signUp(mobileOrEmail, password); password = ""; showDialogText = "Check your email to confirm your citizen account, then sign in." } }) {
                    Text(if (isTa) "புதிய கணக்கு உருவாக்கு" else "Create Account", color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                        CivicDataRepository.login(mobileOrEmail, password)
                        password = ""
                        onLoginSuccess()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = CivicSubtleBorder)

            ElevatedButton(
                onClick = {
                    mobileOrEmail = "test@gmail.com"
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CivicPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "மாதிரி உள்நுழைவு (Test Email)" else "Use Test Email for Citizen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .background(CivicPaleBlueBg)
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
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isTa) "கடவுச்சொல்" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CivicPrimaryBlue) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                    CivicDataRepository.launchOperation {
                        CivicDataRepository.login(username, password)
                        password = ""
                        onLoginSuccess()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login as Officer", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "officer@test.com"
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CivicPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTa) "மாதிரி உள்நுழைவு (Demo Officer)" else "Use Test Email for Officer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .background(CivicPaleBlueBg)
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
                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CivicPrimaryBlue) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                        CivicDataRepository.login(username, password)
                        password = ""
                        onLoginSuccess()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isTa) "உள்நுழைக" else "Login as Coordinator", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "coordinator@test.com"
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CivicPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Test Email for Coordinator", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .background(CivicPaleBlueBg)
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
                leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CivicPrimaryBlue) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                        CivicDataRepository.login(contractorId, password)
                        password = ""
                        onLoginSuccess()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Login as Contractor", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    contractorId = "contractor@test.com"
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CivicPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Test Email for Contractor", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .background(CivicPaleBlueBg)
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoBanner(roleName = "Administrator", isTa = isTa)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CivicPrimaryBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CivicPrimaryBlue) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    CivicDataRepository.launchOperation {
                        CivicDataRepository.login(username, password)
                        password = ""
                        onLoginSuccess()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Login as Administrator", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = {
                    username = "admin@test.com"
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CivicPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Test Email for Admin", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DemoBanner(roleName: String, isTa: Boolean) {
    Surface(
        color = CivicPrimaryBlue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = CivicPrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTa) "$roleName: மின்னஞ்சல் மற்றும் கடவுச்சொல்லுடன் உள்நுழைக. பங்கு சேவையகத்தில் சரிபார்க்கப்படும்." else "$roleName: use your account email and password. Your role is verified by the server.",
                style = MaterialTheme.typography.labelSmall,
                color = CivicDarkBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
