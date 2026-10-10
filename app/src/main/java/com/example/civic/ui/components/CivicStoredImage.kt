package com.example.civic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import kotlinx.coroutines.*

/** Download through the user's authorized Storage endpoint; no public URL or disk cache. */
@Composable
fun CivicStoredImage(bucket: String, path: String) {
    val tamil = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var bitmap by remember(bucket, path, CivicDataRepository.currentUser?.id) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(bucket, path) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(bucket, path, CivicDataRepository.currentUser?.id, attempt) {
        failed = false
        try {
            bitmap = withContext(Dispatchers.IO) {
                val bytes = CivicDataRepository.image(bucket, path)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            if (bitmap == null) failed = true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    when {
        bitmap != null -> Image(bitmap!!.asImageBitmap(), if (tamil) "இணைக்கப்பட்ட புகைப்படம்" else "Attached evidence", Modifier.fillMaxWidth().heightIn(max = 280.dp))
        failed -> TextButton(onClick = { attempt++ }) { Text(if (tamil) "புகைப்படத்தை மீண்டும் ஏற்று" else "Retry loading photo") }
        else -> CircularProgressIndicator(Modifier.size(24.dp))
    }
}
