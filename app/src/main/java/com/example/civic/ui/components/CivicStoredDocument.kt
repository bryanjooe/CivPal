package com.example.civic.ui.components

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Download through Storage RLS, then grant a PDF reader temporary access to this one file. */
@Composable
fun CivicStoredDocument(path: String) {
    val context = LocalContext.current
    val tamil = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    TextButton(enabled = !CivicDataRepository.working, onClick = {
        CivicDataRepository.launchOperation {
            val file = withContext(Dispatchers.IO) {
                val bytes = CivicDataRepository.image("deadline-documents", path)
                val directory = File(context.cacheDir, "deadline-documents").apply { mkdirs() }
                directory.listFiles()?.forEach { it.delete() }
                File(directory, "${java.util.UUID.randomUUID()}.pdf").apply { writeBytes(bytes) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.documents", file)
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { context.startActivity(intent) }
            catch (_: android.content.ActivityNotFoundException) {
                error(if (tamil) "PDF கோப்பைப் படிக்க ஒரு பயன்பாட்டை நிறுவவும்." else "Install a PDF viewer to open this document.")
            }
        }
    }) { Text(if (tamil) "ஆவணத்தைத் திற" else "Open supporting PDF") }
}
