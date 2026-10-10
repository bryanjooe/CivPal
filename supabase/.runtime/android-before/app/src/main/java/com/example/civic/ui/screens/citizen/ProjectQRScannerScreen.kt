package com.example.civic.ui.screens.citizen

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.qr.ProjectQRUtils
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun ProjectQRScannerScreen(onNavigateBack: () -> Unit, onProjectScanned: (String) -> Unit) {
    val context = LocalContext.current
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var scanning by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val onBack by rememberUpdatedState(onNavigateBack)
    val onFound by rememberUpdatedState(onProjectScanned)
    val active = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
    DisposableEffect(Unit) {
        active.set(true)
        onDispose { active.set(false) }
    }

    fun startScan() {
        if (scanning || finished || !active.get()) return
        error = null
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            error = "camera"
            return
        }
        scanning = true
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { barcode ->
                    if (active.get() && !finished) {
                        scanning = false
                        when (val result = ProjectQRUtils.lookup(barcode.rawValue, CivicDataRepository.projects)) {
                            is ProjectQRUtils.LookupResult.Found -> {
                                finished = true
                                onFound(result.project.id)
                            }
                            ProjectQRUtils.LookupResult.Invalid -> error = "invalid"
                            ProjectQRUtils.LookupResult.NotFound -> error = "missing"
                        }
                    }
                }
                .addOnCanceledListener {
                    if (active.get() && !finished) {
                        finished = true
                        scanning = false
                        onBack()
                    }
                }
                .addOnFailureListener {
                    if (active.get() && !finished) {
                        scanning = false
                        error = "scanner"
                    }
                }
        } catch (_: Exception) {
            scanning = false
            error = "scanner"
        }
    }

    LaunchedEffect(Unit) { startScan() }
    Scaffold(topBar = {
        CivicTopAppBar(title = if (isTa) "திட்ட QR ஸ்கேன்" else "Scan Project QR",
            canNavigateBack = true, onNavigateBack = onNavigateBack)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (isTa) "கட்டுமானத் தளத்தில் உள்ள திட்ட QR குறியீட்டை கேமராவில் காட்டவும்."
                else "Point your camera at the project QR code displayed at the construction site.")
            error?.let {
                Text(when (it) {
                    "invalid" -> if (isTa) "செல்லாத திட்ட QR குறியீடு" else "Invalid Project QR Code"
                    "missing" -> if (isTa) "திட்டம் கிடைக்கவில்லை" else "Project Not Found"
                    "camera" -> if (isTa) "இந்த சாதனத்தில் கேமரா கிடைக்கவில்லை." else "No camera is available on this device."
                    else -> if (isTa) "ஸ்கேனரைத் திறக்க முடியவில்லை. கேமரா அணுகல் மற்றும் Google Play சேவைகளைச் சரிபார்த்து, இணைய இணைப்புடன் மீண்டும் முயலவும்."
                        else "Unable to open the scanner. Check camera access and Google Play services, then retry with an internet connection."
                }, color = MaterialTheme.colorScheme.error)
            }
            if (scanning) CircularProgressIndicator()
            Button(onClick = { startScan() }, enabled = !scanning && !finished) {
                Text(if (isTa) "மீண்டும் ஸ்கேன் செய்" else "Scan Again")
            }
            TextButton(onClick = onNavigateBack) { Text(if (isTa) "திரும்பு" else "Back") }
        }
    }
}
