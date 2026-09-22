package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

enum class DocumentFilterMode {
    ORIGINAL,
    PRINT_READY,      // Print-Ready clean document (Pure white paper + crisp black ink)
    MAGIC_COLOR,      // Brightens paper & sharpens ink
    BLACK_AND_WHITE,  // Pure binary B&W for ultra crisp print
    GRAYSCALE,        // Smooth grayscale
    DOCUMENT_CLEAN    // Removes shadows and background stains
}

object ImageUtils {

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input)
            input?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * CamScanner 4-Corner Angle & Perspective Transformation
     * Maps 4 quad corners [tlX, tlY, trX, trY, brX, brY, blX, blY] into a rectangular destination
     */
    fun cropPerspective(
        bitmap: Bitmap,
        points: FloatArray, // 8 floats: topLeft (x,y), topRight (x,y), bottomRight (x,y), bottomLeft (x,y)
        outputWidth: Int = 0,
        outputHeight: Int = 0
    ): Bitmap {
        if (points.size < 8) return bitmap

        val tlX = points[0]
        val tlY = points[1]
        val trX = points[2]
        val trY = points[3]
        val brX = points[4]
        val brY = points[5]
        val blX = points[6]
        val blY = points[7]

        // Calculate average widths and heights for natural aspect ratio if not passed
        val topWidth = Math.hypot((trX - tlX).toDouble(), (trY - tlY).toDouble()).toFloat()
        val bottomWidth = Math.hypot((brX - blX).toDouble(), (brY - blY).toDouble()).toFloat()
        val leftHeight = Math.hypot((blX - tlX).toDouble(), (blY - tlY).toDouble()).toFloat()
        val rightHeight = Math.hypot((brX - trX).toDouble(), (brY - trY).toDouble()).toFloat()

        val targetWidth = if (outputWidth > 0) outputWidth else max(100, max(topWidth, bottomWidth).toInt())
        val targetHeight = if (outputHeight > 0) outputHeight else max(100, max(leftHeight, rightHeight).toInt())

        val src = points
        val dst = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        if (!success) {
            return bitmap
        }

        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return result
    }

    /**
     * Auto Document Cropping & Edge Detection
     * Scans pixels from the edges inward to detect dark boundaries or messy shadows
     * and crops to the genuine paper note bounds.
     */
    fun autoCropPaperDocument(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Downsampled check for speed
        val sampleStep = 8
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3

                // Detect if pixel is part of the bright paper note rather than dark surface
                if (brightness > 90) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Add 2% safety padding inside bounds
        val padX = ((maxX - minX) * 0.02f).toInt()
        val padY = ((maxY - minY) * 0.02f).toInt()

        val left = max(0, minX - padX)
        val top = max(0, minY - padY)
        val right = min(width, maxX + padX)
        val bottom = min(height, maxY + padY)

        val cropWidth = right - left
        val cropHeight = bottom - top

        return if (cropWidth > width * 0.3f && cropHeight > height * 0.3f) {
            Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        } else {
            bitmap
        }
    }

    /**
     * Feature 1: Advanced Document Filters (Magic Color, B&W, Document Clean)
     */
    fun applyDocumentFilter(bitmap: Bitmap, filterMode: DocumentFilterMode): Bitmap {
        if (filterMode == DocumentFilterMode.ORIGINAL) return bitmap

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        when (filterMode) {
            DocumentFilterMode.ORIGINAL -> return bitmap
            DocumentFilterMode.PRINT_READY -> {
                // Adaptive Print Transformation: Brightens paper to crisp pure white and makes text deep black
                val matrix = ColorMatrix()
                matrix.setSaturation(0.05f) // Subtle ink tint preservation
                val contrast = 2.2f
                val brightness = -45f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            DocumentFilterMode.MAGIC_COLOR -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.4f, 0f, 0f, 0f, -30f,
                        0f, 1.4f, 0f, 0f, -30f,
                        0f, 0f, 1.4f, 0f, -30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            DocumentFilterMode.BLACK_AND_WHITE -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val contrast = 1.9f
                val brightness = -60f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            DocumentFilterMode.GRAYSCALE -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            DocumentFilterMode.DOCUMENT_CLEAN -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, 10f,
                        0f, 1.25f, 0f, 0f, 10f,
                        0f, 0f, 1.25f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }
        return output
    }

    /**
     * Feature 6: Digital Signature & Stamp Extractor
     * Extracts handwritten ink signature from background paper and turns background into transparent Alpha.
     */
    fun extractSignatureTransparent(bitmap: Bitmap, inkThreshold: Int = 180): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (r + g + b) / 3

            if (brightness < inkThreshold) {
                // Ink stroke detected: calculate opacity based on darkness
                val alpha = (255 * (inkThreshold - brightness) / inkThreshold).coerceIn(40, 255)
                // Boost blue/black ink tone
                pixels[i] = Color.argb(alpha, (r * 0.7f).toInt(), (g * 0.7f).toInt(), (b * 0.9f).toInt())
            } else {
                // Background paper made 100% transparent
                pixels[i] = Color.TRANSPARENT
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Preset handwritten note generators for quick testing
     */
    fun createSampleNoteBitmap(type: SampleNoteType): Pair<Bitmap, String> {
        val width = 900
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Warm notebook paper
        val paperColor = when (type) {
            SampleNoteType.STUDENT_LECTURE -> Color.rgb(255, 253, 245)
            SampleNoteType.MEETING_MINUTES -> Color.rgb(248, 250, 252)
            SampleNoteType.RECIPE_CARD -> Color.rgb(254, 243, 199)
            SampleNoteType.SINDHI_URDU_NOTE -> Color.rgb(255, 255, 250)
            SampleNoteType.HINDI_MULTILINGUAL -> Color.rgb(255, 255, 255)
            SampleNoteType.MATH_PHYSICS_EQUATION -> Color.rgb(255, 255, 255)
            SampleNoteType.TABLE_INVOICE -> Color.rgb(250, 250, 250)
        }
        canvas.drawColor(paperColor)

        // Lined notebook lines
        val linePaint = Paint().apply {
            color = Color.rgb(219, 234, 254)
            strokeWidth = 2f
        }
        for (y in 140..height step 48) {
            canvas.drawLine(40f, y.toFloat(), width - 40f, y.toFloat(), linePaint)
        }

        // Left margin red line
        val marginPaint = Paint().apply {
            color = Color.rgb(248, 113, 113)
            strokeWidth = 2.5f
        }
        canvas.drawLine(120f, 60f, 120f, height - 40f, marginPaint)

        val headerPaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        var startY = 110f
        val startX = 140f

        val (title, lines, expectedText) = when (type) {
            SampleNoteType.STUDENT_LECTURE -> Triple(
                "Physics 101 - Thermodynamics",
                listOf(
                    "• 1st Law: Delta U = Q - W",
                    "• Energy is conserved in isolated systems.",
                    "• Entropy increases in spontaneous processes.",
                    "• Carnot Efficiency: eta = 1 - (Tc / Th)",
                    "• Assignment: Read Chapter 4 by Thursday."
                ),
                """Physics 101 - Thermodynamics
• 1st Law: ΔU = Q - W
• Energy is conserved in isolated systems.
• Entropy increases in spontaneous processes.
• Carnot Efficiency: η = 1 - (Tc / Th)
• Assignment: Read Chapter 4 by Thursday."""
            )
            SampleNoteType.MEETING_MINUTES -> Triple(
                "Sprint Planning & Next Steps",
                listOf(
                    "Date: Sept 15 | Team: Mobile Dev",
                    "1. Finish OCR camera scanner pipeline",
                    "2. Integrate AI bullet point summarizer",
                    "3. Add multilingual speech outputs (TTS)",
                    "4. Release v1.0 beta build to testers"
                ),
                """Sprint Planning & Next Steps
Date: Sept 15 | Team: Mobile Dev
1. Finish OCR camera scanner pipeline
2. Integrate AI bullet point summarizer
3. Add multilingual speech outputs (TTS)
4. Release v1.0 beta build to testers"""
            )
            SampleNoteType.RECIPE_CARD -> Triple(
                "Secret Grandma's Lemon Cake",
                listOf(
                    "Ingredients:",
                    "- 2 cups all-purpose flour",
                    "- 1 cup organic raw sugar",
                    "- 3 fresh lemons (zest + juice)",
                    "- 1/2 cup unsalted butter",
                    "Bake at 350°F for 35 mins until golden."
                ),
                """Secret Grandma's Lemon Cake
Ingredients:
- 2 cups all-purpose flour
- 1 cup organic raw sugar
- 3 fresh lemons (zest + juice)
- 1/2 cup unsalted butter
Bake at 350°F for 35 mins until golden."""
            )
            SampleNoteType.SINDHI_URDU_NOTE -> Triple(
                "سنڌي ۽ اردو نوٽ (Sindhi & Urdu)",
                listOf(
                    "۱. سنڌي هٿ اکر نوٽ اسڪينر ڪامياب ٿيو",
                    "۲. صاف ڪمپيوٽر ٽائپنگ ۾ تبديل ڪريو",
                    "۳. اردو ۽ سنڌي مان انگريزي ۾ ترجمو ڪريو",
                    "۴. پی ڈی ایف (PDF) اور ورڈ میں محفوظ کریں",
                    "حالت: مڪمل ۽ تيار (Status: Ready)"
                ),
                """سنڌي ۽ اردو نوٽ (Sindhi & Urdu)
۱. سنڌي هٿ اکر نوٽ اسڪينر ڪامياب ٿيو
۲. صاف ڪمپيوٽر ٽائپنگ ۾ تبديل ڪريو
۳. اردو ۽ سنڌي مان انگريزي ۾ ترجمو ڪريو
۴. پی ڈی ایف (PDF) اور ورڈ میں محفوظ کریں
حالت: مڪمل ۽ تيار (Status: Ready)"""
            )
            SampleNoteType.HINDI_MULTILINGUAL -> Triple(
                "हस्तलेख नोट / Project Checklist",
                listOf(
                    "1. लिपि स्कैन - OCR परीक्षण सफल",
                    "2. हस्तलिखित नोट्स को डिजिटल टेक्स्ट में बदलें",
                    "3. AI Summarize & Voice Output",
                    "4. PDF और TXT میں آسانی سے شیئر کریں",
                    "Status: Completed & Ready"
                ),
                """हस्तलेख नोट / Project Checklist
1. लिपि स्कैन - OCR परीक्षण सफल
2. हस्तलिखित नोट्स को डिजिटल टेक्स्ट में बदलें
3. AI Summarize & Voice Output
4. PDF اور TXT میں آسانی سے شیئر کریں
Status: Completed & Ready"""
            )
            SampleNoteType.MATH_PHYSICS_EQUATION -> Triple(
                "Calculus & Quadratic Formula",
                listOf(
                    "f(x) = 2x^2 + 5x - 3 = 0",
                    "Roots: x = (-b +- sqrt(b^2 - 4ac)) / 2a",
                    "x = (-5 +- sqrt(25 - 4(2)(-3))) / 4",
                    "x = (-5 +- sqrt(49)) / 4 => x = 0.5, -3",
                    "Integral: int(3x^2 dx) = x^3 + C"
                ),
                """Calculus & Quadratic Formula
f(x) = 2x² + 5x - 3 = 0
Roots: x = (-b ± √(b² - 4ac)) / (2a)
x = (-5 ± √(25 - 4(2)(-3))) / 4
x = (-5 ± √49) / 4 => x = 0.5 or x = -3
Integral: ∫ 3x² dx = x³ + C"""
            )
            SampleNoteType.TABLE_INVOICE -> Triple(
                "Grocery & Store Expense Log",
                listOf(
                    "Item, Qty, Price, Total",
                    "Notebooks, 3, $4.50, $13.50",
                    "Gel Pens (Pack), 2, $6.00, $12.00",
                    "Sticky Notes, 4, $2.00, $8.00",
                    "Grand Total: $33.50"
                ),
                """Item, Qty, Price, Total
Notebooks, 3, $4.50, $13.50
Gel Pens (Pack), 2, $6.00, $12.00
Sticky Notes, 4, $2.00, $8.00
Grand Total: $33.50"""
            )
        }

        canvas.drawText(title, startX, startY, headerPaint)
        startY += 55f

        for (line in lines) {
            canvas.drawText(line, startX, startY, textPaint)
            startY += 48f
        }

        return Pair(bitmap, expectedText)
    }
}

enum class SampleNoteType(val displayName: String, val subtitle: String) {
    SINDHI_URDU_NOTE("سنڌي ۽ اردو نوٽ (Sindhi & Urdu)", "Sindhi & Urdu handwritten script"),
    STUDENT_LECTURE("Physics Lecture", "Formulas & bullet points"),
    MATH_PHYSICS_EQUATION("Math & Equations", "Calculus, integrals & solutions"),
    TABLE_INVOICE("Table & Invoice", "Tabular data for Excel / CSV"),
    MEETING_MINUTES("Meeting Notes", "Action items & tasks"),
    RECIPE_CARD("Handwritten Recipe", "Ingredients & directions"),
    HINDI_MULTILINGUAL("Urdu & Hindi Note", "Multi-language recognition")
}
