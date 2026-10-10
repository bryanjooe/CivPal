package com.example.civic.ui.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withTranslation
import com.example.civic.data.models.Project
import java.io.FileOutputStream

object ProjectQRPrintHelper {
    fun print(context: Context, project: Project, qr: Bitmap, isTa: Boolean) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: error("Printing unavailable")
        manager.print("CivPal ${project.id}", ProjectAdapter(context, project, qr, isTa),
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMinMargins(PrintAttributes.Margins(350, 350, 350, 350))
                .setResolution(PrintAttributes.Resolution("qr", "Project QR", 300, 300)).build())
    }

    internal class ProjectAdapter(
        val context: Context, val project: Project, val qr: Bitmap, val isTa: Boolean
    ) : PrintDocumentAdapter() {
        private var attributes: PrintAttributes? = null
        private fun label(en: String, ta: String) = if (isTa) ta else en

        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
            attributes = newAttributes
            callback.onLayoutFinished(PrintDocumentInfo.Builder("CivPal-${project.id}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(),
                oldAttributes != newAttributes)
        }

        override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
            if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
            if (pages.none { it.start <= 0 && it.end >= 0 }) {
                callback.onWriteFinished(emptyArray()); return
            }
            try {
                writePdf(requireNotNull(attributes), destination, cancellationSignal)
                if (cancellationSignal.isCanceled) callback.onWriteCancelled()
                else callback.onWriteFinished(arrayOf(PageRange(0, 0)))
            } catch (_: Exception) {
                if (cancellationSignal.isCanceled) callback.onWriteCancelled()
                else callback.onWriteFailed(label("Unable to create the printable QR page.", "அச்சிடக்கூடிய QR பக்கத்தை உருவாக்க முடியவில்லை."))
            }
        }

        internal fun writePdf(attributes: PrintAttributes, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal) {
                val document = PrintedPdfDocument(context, attributes)
                try {
                    val page = document.startPage(0)
                    val canvas = page.canvas
                    val rect = page.info.contentRect
                    val scale = minOf(rect.width() / 515f, rect.height() / 740f)
                    canvas.translate(rect.left + (rect.width() - 515 * scale) / 2,
                        rect.top + (rect.height() - 740 * scale) / 2)
                    canvas.scale(scale, scale)
                    canvas.drawColor(Color.WHITE)

                    fun text(value: String, y: Float, size: Float, center: Boolean = false,
                        bold: Boolean = false, maxLines: Int = 2): Int {
                        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK; textSize = size
                            typeface = android.graphics.Typeface.create("sans-serif",
                                if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                        }
                        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, 515)
                            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                            .setIncludePad(false).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()
                        canvas.withTranslation(0f, y) { layout.draw(this) }
                        return layout.height
                    }
                    text("CIVPAL TN", 6f, 24f, center = true, bold = true)
                    text(label("PUBLIC WORK INFORMATION", "பொதுப் பணி தகவல்"), 44f, 17f, center = true, bold = true)
                    var y = 90f
                    listOf(
                        label("Project Name", "திட்டப் பெயர்") to project.title,
                        label("Project ID", "திட்ட ID") to project.id,
                        label("Municipality", "நகராட்சி") to project.municipalBody,
                        label("Department", "துறை") to project.department
                    ).forEach { (name, value) ->
                        y += text("$name: $value", y, 14f) + 14f
                    }
                    // The bitmap includes a four-module quiet zone. Disable interpolation
                    // to retain crisp black/white modules on the printed page.
                    val qrTop = maxOf(300f, y + 8f)
                    val qrSize = minOf(320f, 630f - qrTop)
                    val qrLeft = (515f - qrSize) / 2
                    canvas.drawBitmap(qr, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
                        Paint().apply { isFilterBitmap = false })
                    text(label("SCAN TO VIEW PROJECT DETAILS", "திட்ட விவரங்களைக் காண ஸ்கேன் செய்யவும்"),
                        645f, 16f, center = true, bold = true)
                    text(label("This QR code provides information about the public work being carried out at this location.",
                        "இங்கு நடைபெறும் பொதுப் பணி பற்றிய தகவல்களை இந்த QR குறியீடு வழங்குகிறது."),
                        687f, 12f, center = true)
                    document.finishPage(page)
                    if (cancellationSignal.isCanceled) return
                    FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
                } finally {
                    document.close()
                }
        }
    }
}
