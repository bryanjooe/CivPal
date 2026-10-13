package com.example.civic.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar
import kotlinx.coroutines.launch

@Composable
fun LoginTypeSelectScreen(
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onSelectRole: (UserRole) -> Unit,
    onCitizenLoginSuccess: () -> Unit = {}
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab) { 2 }

    Scaffold(
        topBar = {
            CivicTopAppBar(
                title = if (isTa) "CivPal உள்நுழைவு" else "CivPal Login",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(if (isTa) "குடிமகன்" else "Citizen Login", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(if (isTa) "அதிகாரப்பூர்வ" else "Official Login", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> CitizenTabPage(
                        onLoginSuccess = onCitizenLoginSuccess,
                        isTa = isTa
                    )
                    1 -> OfficialTabPage(
                        onSelectRole = onSelectRole,
                        isTa = isTa
                    )
                }
            }
        }
    }
}

@Composable
private fun CitizenTabPage(
    onLoginSuccess: () -> Unit,
    isTa: Boolean
) {
    var mobileOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDialogText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = mobileOrEmail,
            onValueChange = { mobileOrEmail = it },
            label = { Text(if (isTa) "கைபேசி எண் / மின்னஞ்சல்" else "Email Address / Mobile") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
            TextButton(onClick = { CivicDataRepository.launchOperation { CivicDataRepository.resetPassword(mobileOrEmail); showDialogText = "If this email is registered, reset instructions have been sent." } }) {
                Text(if (isTa) "கடவுச்சொல் மறந்ததா?" else "Forgot Password?")
            }

            TextButton(onClick = { CivicDataRepository.launchOperation { CivicDataRepository.signUp(mobileOrEmail, password); password = ""; showDialogText = "Check your email to confirm your citizen account, then sign in." } }) {
                Text(if (isTa) "புதிய கணக்கு உருவாக்கு" else "Create Account")
            }
        }

        Button(
            onClick = {
                CivicDataRepository.launchOperation {
                    val email = mobileOrEmail.ifBlank { "test@gmail.com" }
                    val pass = password.ifBlank { "12345678" }
                    CivicDataRepository.login(email, pass)
                    password = ""
                    onLoginSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (isTa) "குடிமகனாக உள்நுழைக" else "Login as Citizen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider()

        ElevatedButton(
            onClick = {
                mobileOrEmail = "test@gmail.com"
                password = "12345678"
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
            Text(if (isTa) "மாதிரி உள்நுழைவு (Test Email)" else "Use Test Email for Citizen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun OfficialTabPage(
    onSelectRole: (UserRole) -> Unit,
    isTa: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isTa) "அதிகாரப்பூர்வ தளம் உள்நுழைவு" else "Official Workspace Sign In",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isTa) "அதிகாரப்பூர்வ பணிக்கான கணக்கை தேர்வு செய்யவும்."
                        else "Select your official role below to access the workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        RoleCard(
            role = UserRole.OFFICER,
            titleEn = "Government / Municipal Officer",
            titleTa = "அரசு / நகராட்சி அலுவலர்",
            descEn = "Register projects, verify contractor progress, record delays, and manage handovers.",
            descTa = "திட்டங்களைப் பதிவு செய்யவும், முன்னேற்றத்தைச் சரிபார்க்கவும்.",
            icon = Icons.Default.Badge,
            onClick = { onSelectRole(UserRole.OFFICER) }
        )

        RoleCard(
            role = UserRole.COORDINATOR,
            titleEn = "Department Coordinator",
            titleTa = "துறை ஒருங்கிணைப்பாளர்",
            descEn = "Cross-departmental schedule tracking, inter-work conflict resolution & reports.",
            descTa = "துறைசார் திட்ட முரண்பாடுகளைக் கண்டறிந்து தீர்க்கவும்.",
            icon = Icons.Default.AccountTree,
            onClick = { onSelectRole(UserRole.COORDINATOR) }
        )

        RoleCard(
            role = UserRole.CONTRACTOR,
            titleEn = "Authorized Contractor",
            titleTa = "ஒப்பந்ததாரர் உள்நுழைவு",
            descEn = "Submit milestone progress reports, site photos, and deadline revision requests.",
            descTa = "பணி முன்னேற்ற அறிக்கைகள் மற்றும் புகைப்படங்களைச் சமர்ப்பிக்கவும்.",
            icon = Icons.Default.Engineering,
            onClick = { onSelectRole(UserRole.CONTRACTOR) }
        )

        RoleCard(
            role = UserRole.ADMINISTRATOR,
            titleEn = "System Administrator",
            titleTa = "அமைப்பு நிர்வாகி",
            descEn = "Manage users, departments, office jurisdictions, and audit log monitoring.",
            descTa = "பயனர்கள், துறைகள் மற்றும் தணிக்கைப் பதிவுகளை நிர்வகிக்கவும்.",
            icon = Icons.Default.AdminPanelSettings,
            onClick = { onSelectRole(UserRole.ADMINISTRATOR) }
        )
    }
}

@Composable
private fun RoleCard(
    role: UserRole,
    titleEn: String,
    titleTa: String,
    descEn: String,
    descTa: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTa) titleTa else titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isTa) descTa else descEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
