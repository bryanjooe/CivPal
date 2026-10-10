package com.example.civic.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.example.civic.data.models.Project
import com.example.civic.data.models.User
import com.example.civic.data.models.UserRole
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object ProjectQRUtils {
    sealed interface LookupResult {
        data class Found(val project: Project) : LookupResult
        data object Invalid : LookupResult
        data object NotFound : LookupResult
    }

    fun payload(project: Project): String = project.id

    fun lookup(rawValue: String?, projects: List<Project>): LookupResult {
        if (rawValue.isNullOrBlank()) return LookupResult.Invalid
        // Match the actual repository first; registration derives the department portion
        // from free text, so a stricter invented ID format would reject existing IDs.
        projects.firstOrNull { it.id == rawValue }?.let { return LookupResult.Found(it) }
        return if (Regex("TN-[\\p{L}\\p{N} ]+-[0-9]{4}-[0-9]+").matches(rawValue)) {
            LookupResult.NotFound
        } else LookupResult.Invalid
    }

    // OfficerMyProjectsScreen currently exposes all projects to the OFFICER role.
    // Keep that existing scope rather than inventing assignment rules here.
    fun managedProjects(user: User?, projects: List<Project>): List<Project> =
        if (user?.role == UserRole.OFFICER && user.isActive) projects else emptyList()

    fun matrix(projectId: String, size: Int = 1024): BitMatrix {
        require(projectId.isNotBlank())
        return QRCodeWriter().encode(projectId, BarcodeFormat.QR_CODE, size, size, mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 4,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        ))
    }

    fun bitmap(projectId: String): Bitmap {
        val matrix = matrix(projectId)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }
}
