package com.example.civic

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicBottomNavigation
import com.example.civic.ui.components.CivicDrawerContent
import com.example.civic.ui.navigation.Screen
import com.example.civic.ui.qr.ProjectQRPrintHelper
import com.example.civic.ui.qr.ProjectQRUtils
import com.example.civic.ui.theme.CivicTheme
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProjectQRIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val originalLanguage = CivicDataRepository.currentLanguage
    @After fun restoreLanguage() { CivicDataRepository.currentLanguage = originalLanguage }

    @Test fun citizenBarOrderCenterActionAndNavigation() {
        CivicDataRepository.currentLanguage = AppLanguage.ENGLISH
        var destination: Screen? = null
        compose.setContent { CivicTheme {
            CivicBottomNavigation(Screen.CitizenDashboard.route) { destination = it }
        } }
        val labels = listOf("Home", "Projects", "Scan", "Map", "Complaints")
        val nodes = labels.map { compose.onNodeWithText(it).fetchSemanticsNode() }
        assertTrue(nodes.zipWithNext().all { (a, b) -> a.boundsInRoot.center.x < b.boundsInRoot.center.x })
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(root.center.x, nodes[2].boundsInRoot.center.x, 1f)
        val scanIcon = compose.onNodeWithContentDescription("Scan", useUnmergedTree = true).fetchSemanticsNode()
        val homeIcon = compose.onNodeWithContentDescription("Home", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(scanIcon.boundsInRoot.width > homeIcon.boundsInRoot.width)
        compose.onNodeWithText("Profile").assertDoesNotExist()
        val screens = listOf(Screen.CitizenDashboard, Screen.PublicProjects, Screen.ProjectQRScanner, Screen.ProjectMap, Screen.MyComplaints)
        labels.zip(screens).forEach { (label, screen) ->
            if (label == "Scan") compose.onNodeWithContentDescription(label).performClick()
            else compose.onNodeWithText(label).performClick()
            compose.runOnIdle { assertEquals(screen, destination) }
        }
    }

    @Test fun profileAndSavedProjectsUseExistingScreenInBothLanguages() {
        CivicDataRepository.currentLanguage = AppLanguage.ENGLISH
        var destination: Screen? = null
        compose.setContent { CivicTheme {
            CivicDrawerContent(UserRole.CITIZEN, Screen.CitizenDashboard.route, { destination = it }, {})
        } }
        listOf("Profile", "Saved Projects").forEach {
            compose.onNodeWithText(it).performClick()
            compose.runOnIdle { assertEquals(Screen.CitizenProfile, destination) }
        }
        compose.runOnIdle { CivicDataRepository.currentLanguage = AppLanguage.TAMIL }
        compose.onNodeWithText("சுயவிவரம்").assertExists()
        compose.onNodeWithText("சேமிக்கப்பட்டவை").assertExists()
    }

    @Test fun printableA4PageRemainsScannableInBothLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = CivicDataRepository.projects.first()
        val qr = ProjectQRUtils.bitmap(project.id)
        try {
            listOf(false, true).forEach { isTa ->
                val file = File.createTempFile("project-qr", ".pdf", context.cacheDir)
                try {
                    val adapter = ProjectQRPrintHelper.ProjectAdapter(context, project, qr, isTa)
                    val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("test", "test", 300, 300))
                        .setMinMargins(PrintAttributes.Margins(350, 350, 350, 350)).build()
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE).use { fd ->
                        adapter.writePdf(attributes, fd, CancellationSignal())
                    }
                    PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                        assertEquals(1, renderer.pageCount)
                        renderer.openPage(0).use { page ->
                            val image = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            try {
                                page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val pixels = IntArray(image.width * image.height)
                                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                                val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels)))
                                assertEquals(project.id, QRCodeReader().decode(binary).text)
                            } finally { image.recycle() }
                        }
                    }
                } finally { file.delete() }
            }
        } finally { qr.recycle() }
    }
}
