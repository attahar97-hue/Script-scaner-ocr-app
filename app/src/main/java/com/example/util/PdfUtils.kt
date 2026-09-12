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
import com.example.data.local.ScanEntity
import org.json.JSONArray
import org.json.JSONObject
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
     * Generates a cleanly formatted, print-quality multi-page PDF document containing the extracted text and summary.
     */
    fun exportAndSharePdf(context: Context, title: String, textContent: String, summary: String = "") {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val marginX = 40f
            val contentWidth = pageWidth - (marginX * 2)

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1.2f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = 50f

            // Document Header on Page 1
            canvas.drawText("ScriptScan OCR - Digitized Document", marginX, currentY, titlePaint)
            currentY += 22f
            canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, linePaint)
            currentY += 22f

            canvas.drawText("Title: $title", marginX, currentY, headerPaint)
            currentY += 18f

            if (summary.isNotBlank()) {
                canvas.drawText("AI Summary & Key Takeaways:", marginX, currentY, headerPaint)
                currentY += 16f
                currentY = drawWrappedText(canvas, summary, marginX, currentY, contentWidth, bodyPaint, pageHeight - 60f)
                currentY += 14f
                canvas.drawLine(marginX, currentY, marginX + contentWidth, currentY, linePaint)
                currentY += 18f
            }

            canvas.drawText("Extracted Digitized Text:", marginX, currentY, headerPaint)
            currentY += 16f

            // Split and wrap lines across pages if needed
            val paragraphs = textContent.split("\n")
            for (para in paragraphs) {
                if (currentY > pageHeight - 60f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 50f
                }

                if (para.isEmpty()) {
                    currentY += bodyPaint.textSize * 1.2f
                    continue
                }

                val words = para.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val measuredWidth = bodyPaint.measureText(testLine)
                    if (measuredWidth > contentWidth) {
                        if (currentY > pageHeight - 60f) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            currentY = 50f
                        }
                        canvas.drawText(currentLine, marginX, currentY, bodyPaint)
                        currentY += bodyPaint.textSize * 1.4f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    if (currentY > pageHeight - 60f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 50f
                    }
                    canvas.drawText(currentLine, marginX, currentY, bodyPaint)
                    currentY += bodyPaint.textSize * 1.4f
                }
            }

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}_ocr.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScriptScan OCR: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Feature 3: Word (.doc / .docx) Export
     */
    fun exportAndShareWord(context: Context, title: String, content: String, summary: String = "") {
        try {
            val file = File(context.cacheDir, "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.doc")
            val htmlWordContent = buildString {
                append("<html><head><meta charset='utf-8'><title>$title</title></head><body>")
                append("<h1 style='color:#1E40AF; font-family:sans-serif;'>$title</h1>")
                append("<p style='color:#64748B; font-size:12px;'>Digitized with ScriptScan OCR</p><hr/>")
                if (summary.isNotBlank()) {
                    append("<h3 style='color:#0F172A;'>AI Summary</h3>")
                    append("<div style='background:#F1F5F9; padding:12px; border-radius:6px;'>${summary.replace("\n", "<br/>")}</div><br/>")
                }
                append("<h3 style='color:#0F172A;'>Extracted Note Content</h3>")
                append("<p style='font-family:sans-serif; line-height:1.6;'>${content.replace("\n", "<br/>")}</p>")
                append("</body></html>")
            }
            file.writeText(htmlWordContent)

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/msword"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title (Word Document)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Word Document"))
        } catch (e: Exception) {
            e.printStackTrace()
            sharePlainText(context, content, title)
        }
    }

    /**
     * Feature 3: Excel / CSV Spreadsheet Table Export
     */
    fun exportAndShareCsv(context: Context, title: String, content: String) {
        try {
            val file = File(context.cacheDir, "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.csv")
            val csvBuilder = StringBuilder()
            val lines = content.split("\n")
            for (line in lines) {
                if (line.isBlank()) continue
                // If line contains commas or tabs or vertical pipes
                if (line.contains(",") || line.contains("\t") || line.contains("|")) {
                    val cells = line.split("[,|\t]".toRegex()).map { "\"${it.trim().replace("\"", "\"\"")}\"" }
                    csvBuilder.appendLine(cells.joinToString(","))
                } else {
                    csvBuilder.appendLine("\"${line.replace("\"", "\"\"")}\"")
                }
            }
            file.writeText(csvBuilder.toString())

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title (CSV / Excel)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share CSV Table"))
        } catch (e: Exception) {
            e.printStackTrace()
            sharePlainText(context, content, title)
        }
    }

    /**
     * Feature 6: Digital Signature PNG Share
     */
    fun exportAndShareSignaturePng(context: Context, signatureBitmap: Bitmap, name: String = "digital_signature") {
        try {
            val file = File(context.cacheDir, "${name.replace("[^a-zA-Z0-9]".toRegex(), "_")}.png")
            val out = FileOutputStream(file)
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Extracted Digital Signature")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Signature PNG"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share signature: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Feature 7: Cloud Backup & Google Drive JSON Export
     */
    fun exportCloudBackupJson(context: Context, scans: List<ScanEntity>) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("appName", "ScriptScan OCR")
            root.put("exportTime", System.currentTimeMillis())

            val array = JSONArray()
            scans.forEach { scan ->
                val obj = JSONObject()
                obj.put("title", scan.title)
                obj.put("rawText", scan.rawText)
                obj.put("summary", scan.summary)
                obj.put("language", scan.language)
                obj.put("source", scan.source)
                obj.put("timestamp", scan.timestamp)
                obj.put("isFavorite", scan.isFavorite)
                array.put(obj)
            }
            root.put("scans", array)

            val file = File(context.cacheDir, "scriptscan_backup_${System.currentTimeMillis() % 10000}.json")
            file.writeText(root.toString(2))

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScriptScan OCR Cloud Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Backup (Google Drive / Files)"))
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
