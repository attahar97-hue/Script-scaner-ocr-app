package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    /**
     * Renders pages of a PDF document Uri into Bitmaps for OCR processing.
     */
    fun renderPdfPages(context: Context, pdfUri: Uri, maxPages: Int = 10): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return emptyList()
            renderer = PdfRenderer(pfd)
            val pageCount = minOf(renderer.pageCount, maxPages)

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // Render at high DPI for crisp OCR
                val width = (page.width * 2).coerceAtLeast(800)
                val height = (page.height * 2).coerceAtLeast(1000)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
        return bitmaps
    }

    /**
     * Generates a cleanly formatted PDF document containing the extracted text and summary,
     * and shares it via system Intent.
     */
    fun exportAndSharePdf(context: Context, title: String, textContent: String, summary: String = "") {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard (points)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 64, 175)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 11f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1.5f
            }

            var currentY = 50f
            val marginX = 40f
            val contentWidth = 595 - (marginX * 2)

            // App Header & Branding
            canvas.drawText("ScriptScan OCR - Digitized Document", marginX, currentY, titlePaint)
            currentY += 25f
            canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, linePaint)
            currentY += 25f

            // Document Title
            canvas.drawText("Title: $title", marginX, currentY, headerPaint)
            currentY += 20f

            if (summary.isNotBlank()) {
                canvas.drawText("AI Summary & Key Takeaways:", marginX, currentY, headerPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, summary, marginX, currentY, contentWidth, bodyPaint, 780f)
                currentY += 15f
                canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, linePaint)
                currentY += 20f
            }

            // Extracted Text
            canvas.drawText("Extracted Text:", marginX, currentY, headerPaint)
            currentY += 18f
            drawWrappedText(canvas, textContent, marginX, currentY, contentWidth, bodyPaint, 800f)

            pdfDocument.finishPage(page)

            // Save to internal cache directory
            val file = File(context.cacheDir, "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}_ocr.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Share using FileProvider or fallback Intent
            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScriptScan OCR: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports text to a .txt file and opens share intent.
     */
    fun exportAndShareTxt(context: Context, title: String, content: String, summary: String = "") {
        try {
            val file = File(context.cacheDir, "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.txt")
            val fullText = buildString {
                appendLine("=== ScriptScan OCR ===")
                appendLine("Title: $title")
                appendLine()
                if (summary.isNotBlank()) {
                    appendLine("--- AI SUMMARY ---")
                    appendLine(summary)
                    appendLine()
                }
                appendLine("--- EXTRACTED TEXT ---")
                appendLine(content)
            }
            file.writeText(fullText)

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, fullText)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Text Document"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to direct text share
            sharePlainText(context, content, title)
        }
    }

    fun sharePlainText(context: Context, text: String, title: String = "ScriptScan OCR") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    }

    fun copyToClipboard(context: Context, text: String, label: String = "ScriptScan OCR") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        maxY: Float
    ): Float {
        var y = startY
        val lines = text.split("\n")
        for (line in lines) {
            if (y > maxY) break
            if (line.isEmpty()) {
                y += paint.textSize * 1.2f
                continue
            }
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val measuredWidth = paint.measureText(testLine)
                if (measuredWidth > maxWidth) {
                    canvas.drawText(currentLine, x, y, paint)
                    y += paint.textSize * 1.35f
                    if (y > maxY) break
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotEmpty() && y <= maxY) {
                canvas.drawText(currentLine, x, y, paint)
                y += paint.textSize * 1.35f
            }
        }
        return y
    }
}
