package com.example.civic.ui.screens.officer

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.qr.ProjectQRUtils
import com.example.civic.ui.qr.ProjectQRPrintHelper
import com.example.civic.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectQRGeneratorScreen(onNavigateBack: () -> Unit) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val context = LocalContext.current
    val user = CivicDataRepository.currentUser
    val authorized = user?.role == UserRole.OFFICER && user.isActive
    val projects = ProjectQRUtils.managedProjects(user, CivicDataRepository.managedProjects)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var printError by remember { mutableStateOf(false) }
    val selected = projects.firstOrNull { it.id == selectedId }
    val qr by produceState<Result<Bitmap>?>(null, selected?.id) {
        value = null
        selected?.let { project ->
            value = withContext(Dispatchers.Default) { runCatching { ProjectQRUtils.bitmap(project.id) } }
        }
    }

    Scaffold(topBar = {
        CivicTopAppBar(
            title = if (isTa) "திட்ட QR உருவாக்கு" else "Generate Project QR",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CivicPaleBlueBg)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!authorized) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CivicSubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isTa) "இந்த வசதி அரசு அலுவலர்களுக்கு மட்டுமே." else "This feature is available to government officers only.",
                            modifier = Modifier.padding(20.dp),
                            color = CivicTextSecondaryGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (selected == null) {
                item {
                    Text(
                        text = if (isTa) "பதிவு செய்யப்பட்ட திட்டத்தைத் தேர்ந்தெடுக்கவும்:" else "Select a registered project to generate QR code:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicTextPrimaryDark
                    )
                }
                if (projects.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CivicSubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isTa) "திட்டங்கள் இல்லை." else "No registered projects available.",
                                modifier = Modifier.padding(20.dp),
                                color = CivicTextSecondaryGray
                            )
                        }
                    }
                }
                items(projects, key = { it.id }) { project ->
                    Card(
                        onClick = { selectedId = project.id; printError = false },
                        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, CivicSubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                            Text("ID: ${project.id}", style = MaterialTheme.typography.labelSmall, color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
                            Text(project.municipalBody, style = MaterialTheme.typography.bodySmall, color = CivicTextSecondaryGray)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CivicSubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(selected.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CivicTextPrimaryDark)
                            Text("ID: ${selected.id}", style = MaterialTheme.typography.labelLarge, color = CivicPrimaryBlue, fontWeight = FontWeight.Bold)
                            Text("${selected.department} • ${selected.municipalBody}", style = MaterialTheme.typography.bodyMedium, color = CivicTextSecondaryGray)

                            val bitmap = qr?.getOrNull()
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = (if (isTa) "திட்ட QR குறியீடு: " else "Project QR code: ") + selected.id,
                                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally)
                                )
                            } else if (qr == null) {
                                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = CivicPrimaryBlue)
                            } else {
                                Text(
                                    text = if (isTa) "QR உருவாக்க முடியவில்லை. மீண்டும் திட்டத்தைத் தேர்ந்தெடுக்கவும்." else "Unable to generate QR code. Please select the project again.",
                                    color = CivicErrorRedRes
                                )
                            }

                            Button(
                                onClick = {
                                    val latest = ProjectQRUtils.managedProjects(CivicDataRepository.currentUser, CivicDataRepository.projects)
                                        .firstOrNull { it.id == selectedId }
                                    printError = latest == null || runCatching {
                                        ProjectQRPrintHelper.print(context, latest, requireNotNull(bitmap), isTa)
                                    }.isFailure
                                },
                                enabled = bitmap != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(if (isTa) "QR குறியீட்டை அச்சிடு" else "Print QR Code", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            if (printError) {
                                Text(
                                    text = if (isTa) "அச்சிடலைத் தொடங்க முடியவில்லை. மீண்டும் முயலவும்." else "Unable to start printing. Please try again.",
                                    color = CivicErrorRedRes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            OutlinedButton(
                                onClick = { selectedId = null; printError = false },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CivicSubtleBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isTa) "வேறு திட்டத்தைத் தேர்ந்தெடு" else "Choose Another Project", color = CivicTextSecondaryGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
