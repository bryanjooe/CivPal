package com.example.civic

import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.qr.ProjectQRUtils
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test

class ProjectQRTest {
    private val project get() = CivicDataRepository.projects.first()

    @Test fun payloadIsPermanentIdAndUnaffectedByProgress() {
        val original = project
        val updated = original.copy(progressPercentage = 99, title = "Updated title")
        assertEquals(original.id, ProjectQRUtils.payload(original))
        assertEquals(ProjectQRUtils.payload(original), ProjectQRUtils.payload(updated))
        assertEquals(ProjectQRUtils.matrix(original.id), ProjectQRUtils.matrix(updated.id))
    }

    @Test fun highResolutionQrDecodesToExactUtf8Id() {
        // Includes spaces and Unicode that the existing free-text registration can produce.
        listOf(project.id, "TN-PUB-2025-03", "TN-தமிழ்-2025-04", "TN-A B-2025-05").forEach { id ->
            val matrix = ProjectQRUtils.matrix(id)
            assertEquals(1024, matrix.width)
            assertFalse(matrix[0, 0])
            val pixels = IntArray(matrix.width * matrix.height) { i ->
                if (matrix[i % matrix.width, i / matrix.width]) 0xff000000.toInt() else 0xffffffff.toInt()
            }
            val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))
            assertEquals(id, QRCodeReader().decode(image, mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text)
        }
    }

    @Test fun lookupRejectsUnrelatedContentAndDistinguishesUnknownIds() {
        listOf(null, "", "https://example.com", "javascript:alert(1)", "${project.id}\n", project.id.lowercase()).forEach {
            assertEquals(ProjectQRUtils.LookupResult.Invalid, ProjectQRUtils.lookup(it, listOf(project)))
        }
        assertEquals(ProjectQRUtils.LookupResult.NotFound, ProjectQRUtils.lookup("TN-PWD-2025-999999", listOf(project)))
    }

    @Test fun lookupReturnsLatestRecordWithoutMutatingRepository() {
        val before = CivicDataRepository.projects.toList()
        val latest = project.copy(progressPercentage = 92)
        val result = ProjectQRUtils.lookup(latest.id, listOf(latest)) as ProjectQRUtils.LookupResult.Found
        assertSame(latest, result.project)
        assertEquals(before, CivicDataRepository.projects.toList())
        val unusual = latest.copy(id = "TN-A&B-2025-03")
        assertEquals(ProjectQRUtils.LookupResult.Found(unusual), ProjectQRUtils.lookup(unusual.id, listOf(unusual)))
    }

    @Test fun generationUsesExistingOfficerScopeOnly() {
        val officer = CivicDataRepository.users.first { it.role == UserRole.OFFICER }
        val projects = CivicDataRepository.projects.toList()
        assertEquals(projects, ProjectQRUtils.managedProjects(officer, projects))
        assertTrue(ProjectQRUtils.managedProjects(null, projects).isEmpty())
        assertTrue(ProjectQRUtils.managedProjects(officer.copy(isActive = false), projects).isEmpty())
        UserRole.entries.filter { it != UserRole.OFFICER }.forEach {
            assertTrue(ProjectQRUtils.managedProjects(officer.copy(role = it), projects).isEmpty())
        }
    }
}
