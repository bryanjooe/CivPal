package com.example.civic.ui.screens.officer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.qr.ProjectQRUtils
import com.example.civic.ui.qr.ProjectQRPrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectQRGeneratorScreen(onNavigateBack: () -> Unit) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val context = LocalContext.current
    val user = CivicDataRepository.currentUser
    val authorized = user?.role == UserRole.OFFICER && user.isActive
    val projects = ProjectQRUtils.managedProjects(user, CivicDataRepository.projects)
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
        CivicTopAppBar(title = if (isTa) "திட்ட QR உருவாக்கு" else "Generate Project QR",
            canNavigateBack = true, onNavigateBack = onNavigateBack)
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!authorized) {
                item { Text(if (isTa) "இந்த வசதி அரசு அலுவலர்களுக்கு மட்டுமே." else "This feature is available to government officers only.") }
            } else if (selected == null) {
                item { Text(if (isTa) "பதிவு செய்யப்பட்ட திட்டத்தைத் தேர்ந்தெடுக்கவும்." else "Select an existing registered project.") }
                if (projects.isEmpty()) item { Text(if (isTa) "திட்டங்கள் இல்லை." else "No projects available.") }
                items(projects, key = { it.id }) { project ->
                    Card(onClick = { selectedId = project.id; printError = false }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(project.title, style = MaterialTheme.typography.titleMedium)
                            Text(project.id)
                            Text(project.municipalBody, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text((if (isTa) "திட்டப் பெயர்: " else "Project Name: ") + selected.title)
                        Text((if (isTa) "திட்ட ID: " else "Project ID: ") + selected.id)
                        Text((if (isTa) "நகராட்சி: " else "Municipality: ") + selected.municipalBody)
                        Text((if (isTa) "துறை: " else "Department: ") + selected.department)
                        val bitmap = qr?.getOrNull()
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), contentDescription =
                                (if (isTa) "திட்ட QR குறியீடு: " else "Project QR code: ") + selected.id,
                                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally))
                        } else if (qr == null) {
                            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Text(if (isTa) "QR உருவாக்க முடியவில்லை. மீண்டும் திட்டத்தைத் தேர்ந்தெடுக்கவும்." else "Unable to generate QR code. Please select the project again.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = {
                            // Recheck current authorization and retrieve the latest project before printing.
                            val latest = ProjectQRUtils.managedProjects(CivicDataRepository.currentUser, CivicDataRepository.projects)
                                .firstOrNull { it.id == selectedId }
                            printError = latest == null || runCatching {
                                ProjectQRPrintHelper.print(context, latest, requireNotNull(bitmap), isTa)
                            }.isFailure
                        }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isTa) "QR குறியீட்டை அச்சிடு" else "Print QR Code")
                        }
                        if (printError) Text(if (isTa) "அச்சிடலைத் தொடங்க முடியவில்லை. மீண்டும் முயலவும்." else "Unable to start printing. Please try again.",
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { selectedId = null; printError = false }) {
                            Text(if (isTa) "வேறு திட்டத்தைத் தேர்ந்தெடு" else "Choose Another Project")
                        }
                    }
                }
            }
        }
    }
}
