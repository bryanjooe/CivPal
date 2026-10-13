package com.example.civic.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onSelectRole: (UserRole) -> Unit,
    onScanQrCode: () -> Unit,
    onCitizenLoginSuccess: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CivicPaleBlueBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CivicPrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isTa) "தமிழ்நாடு அரசு" else "Govt of Tamil Nadu",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicPrimaryBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = CivicPrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = CivicDataRepository.currentLanguage == AppLanguage.ENGLISH,
                        onClick = { CivicDataRepository.setLanguage(AppLanguage.ENGLISH) },
                        label = { Text("English", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = CivicDataRepository.currentLanguage == AppLanguage.TAMIL,
                        onClick = { CivicDataRepository.setLanguage(AppLanguage.TAMIL) },
                        label = { Text("தமிழ்", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // App Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Surface(
                    color = CivicPrimaryBlue,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "CivPal",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CivPal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CivicPrimaryBlue,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isTa) "பொதுப்பணி திட்டங்கள் வெளிப்படைத்தன்மை போர்டல்" else "Public Works Transparency & Coordination Portal",
                    style = MaterialTheme.typography.titleSmall,
                    color = CivicTextSecondaryGray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Direct Tabbed Login Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = CivicSecondaryBg,
                        contentColor = CivicPrimaryBlue
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
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> CitizenTabSection(
                                onLoginSuccess = onCitizenLoginSuccess,
                                onScanQrCode = onScanQrCode,
                                isTa = isTa
                            )
                            1 -> OfficialTabSection(
                                onSelectRole = onSelectRole,
                                isTa = isTa
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CitizenTabSection(
    onLoginSuccess: () -> Unit,
    onScanQrCode: () -> Unit,
    isTa: Boolean
) {
    var mobileOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDialogText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = mobileOrEmail,
            onValueChange = { mobileOrEmail = it },
            label = { Text(if (isTa) "கைபேசி எண் / மின்னஞ்சல்" else "Email Address / Mobile") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CivicPrimaryBlue) },
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
                Text(if (isTa) "கடவுச்சொல் மறந்ததா?" else "Forgot Password?", color = CivicPrimaryBlue, fontSize = 13.sp)
            }

            TextButton(onClick = { CivicDataRepository.launchOperation { CivicDataRepository.signUp(mobileOrEmail, password); password = ""; showDialogText = "Check your email to confirm your citizen account, then sign in." } }) {
                Text(if (isTa) "புதிய கணக்கு உருவாக்கு" else "Create Account", color = CivicPrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue, contentColor = Color.White)
        ) {
            Text(if (isTa) "குடிமகனாக உள்நுழைக" else "Login as Citizen", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        ElevatedButton(
            onClick = {
                mobileOrEmail = "test@gmail.com"
                password = "12345678"
            },
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CivicPrimaryBlue,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTa) "மாதிரி உள்நுழைவு (Test Email)" else "Use Test Email for Citizen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = CivicSubtleBorder)

        OutlinedButton(
            onClick = onScanQrCode,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CivicPrimaryBlue)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CivicPrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTa) "திட்ட QR குறியீட்டை ஸ்கேன் செய்" else "Scan Project QR Code", fontSize = 14.sp, color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
        }
    }

    showDialogText?.let { msg ->
        AlertDialog(
            onDismissRequest = { showDialogText = null },
            confirmButton = { Button(onClick = { showDialogText = null }, shape = RoundedCornerShape(10.dp)) { Text("OK") } },
            title = { Text("Information") },
            text = { Text(msg) },
            shape = RoundedCornerShape(16.dp),
            containerColor = CivicSurfaceWhite
        )
    }
}

@Composable
private fun OfficialTabSection(
    onSelectRole: (UserRole) -> Unit,
    isTa: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoleCardItem(
            titleEn = "Government / Municipal Officer",
            titleTa = "அரசு / நகராட்சி அலுவலர்",
            descEn = "Register projects, verify progress & manage handovers.",
            descTa = "திட்டங்களைப் பதிவு செய்யவும், முன்னேற்றத்தைச் சரிபார்க்கவும்.",
            icon = Icons.Default.Badge,
            onClick = { onSelectRole(UserRole.OFFICER) }
        )

        RoleCardItem(
            titleEn = "Department Coordinator",
            titleTa = "துறை ஒருங்கிணைப்பாளர்",
            descEn = "Cross-departmental schedule tracking & conflict resolution.",
            descTa = "துறைசார் திட்ட முரண்பாடுகளைக் கண்டறிந்து தீர்க்கவும்.",
            icon = Icons.Default.AccountTree,
            onClick = { onSelectRole(UserRole.COORDINATOR) }
        )

        RoleCardItem(
            titleEn = "Authorized Contractor",
            titleTa = "ஒப்பந்ததாரர் உள்நுழைவு",
            descEn = "Submit progress reports, site photos & extension requests.",
            descTa = "பணி முன்னேற்ற அறிக்கைகள் மற்றும் புகைப்படங்களைச் சமர்ப்பிக்கவும்.",
            icon = Icons.Default.Engineering,
            onClick = { onSelectRole(UserRole.CONTRACTOR) }
        )

        RoleCardItem(
            titleEn = "System Administrator",
            titleTa = "அமைப்பு நிர்வாகி",
            descEn = "Manage users, departments & audit log monitoring.",
            descTa = "பயனர்கள், துறைகள் மற்றும் தணிக்கைப் பதிவுகளை நிர்வகிக்கவும்.",
            icon = Icons.Default.AdminPanelSettings,
            onClick = { onSelectRole(UserRole.ADMINISTRATOR) }
        )
    }
}

@Composable
private fun RoleCardItem(
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
        colors = CardDefaults.cardColors(containerColor = CivicSecondaryBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CivicSubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CivicPrimaryBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CivicPrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTa) titleTa else titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CivicTextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isTa) descTa else descEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = CivicTextSecondaryGray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = CivicTextSecondaryGray
            )
        }
    }
}
