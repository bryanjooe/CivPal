package com.example.civic.ui.screens.citizen

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.ui.components.CivicTopAppBar
import com.example.civic.ui.qr.ProjectQRUtils
import com.example.civic.ui.theme.*
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

@Composable
fun ProjectQRScannerScreen(onNavigateBack: () -> Unit, onProjectScanned: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                        scope.launch {
                            try {
                                val raw = barcode.rawValue
                                if (raw.isNullOrBlank() || raw.length > 150) { error = "invalid"; return@launch }
                                val project = CivicDataRepository.findProject(raw)
                                if (!active.get()) return@launch
                                if (project == null) {
                                    error = if (ProjectQRUtils.lookup(raw, emptyList()) == ProjectQRUtils.LookupResult.Invalid) "invalid" else "missing"
                                } else {
                                    finished = true
                                    onFound(project.id)
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { if (active.get()) error = "network" }
                            finally { scanning = false }
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
        CivicTopAppBar(
            title = if (isTa) "திட்ட QR ஸ்கேன்" else "Scan Project QR",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CivicPaleBlueBg)
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = CivicPrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = CivicPrimaryBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isTa) "கட்டுமானத் தளத்தில் உள்ள திட்ட QR குறியீட்டை கேமராவில் காட்டவும்."
                            else "Point your camera at the project QR code displayed at the construction site.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CivicTextPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )

                    error?.let { errCode ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (errCode) {
                                "invalid" -> if (isTa) "செல்லாத திட்ட QR குறியீடு" else "Invalid Project QR Code"
                                "missing" -> if (isTa) "திட்டம் கிடைக்கவில்லை" else "Project Not Found"
                                "camera" -> if (isTa) "இந்த சாதனத்தில் கேமரா கிடைக்கவில்லை." else "No camera is available on this device."
                                "network" -> if (isTa) "திட்டத்தை ஏற்ற முடியவில்லை. இணைய இணைப்பைச் சரிபார்த்து மீண்டும் முயலவும்." else "Unable to load the project. Check your connection and retry."
                                else -> if (isTa) "ஸ்கேனரைத் திறக்க முடியவில்லை. கேமரா அணுகல் மற்றும் Google Play சேவைகளைச் சரிபார்த்து, இணைய இணைப்புடன் மீண்டும் முயலவும்."
                                    else "Unable to open the scanner. Check camera access and Google Play services, then retry with an internet connection."
                            },
                            color = CivicErrorRedRes,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = CivicPrimaryBlue)
                    }
                }
            }

            Button(
                onClick = { startScan() },
                enabled = !scanning && !finished,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isTa) "மீண்டும் ஸ்கேன் செய்" else "Scan Again", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isTa) "திரும்பு" else "Back", color = CivicTextSecondaryGray)
            }
        }
    }
}
