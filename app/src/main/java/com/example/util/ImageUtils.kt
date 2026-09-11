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
import android.graphics.Typeface
import android.net.Uri
import java.io.InputStream

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

    fun applyHandwritingEnhancement(bitmap: Bitmap, enhanceContrast: Boolean, toGrayscale: Boolean): Bitmap {
        if (!enhanceContrast && !toGrayscale) return bitmap

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        val matrix = ColorMatrix()
        if (toGrayscale) {
            matrix.setSaturation(0f)
        }
        if (enhanceContrast) {
            // Boost contrast to make faint ink lines sharp against paper
            val contrast = 1.35f
            val brightness = -20f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(contrastMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Generates a sample handwritten note on lined paper canvas for instant user testing.
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
            SampleNoteType.HINDI_MULTILINGUAL -> Color.rgb(255, 255, 255)
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
            SampleNoteType.HINDI_MULTILINGUAL -> Triple(
                "हस्तलेख नोट / Project Checklist",
                listOf(
                    "1. लिपि स्कैन - OCR परीक्षण सफल",
                    "2. हस्तलिखित नोट्स को डिजिटल टेक्स्ट में बदलें",
                    "3. AI Summarize & Voice Output",
                    "4. PDF और TXT में आसानी से शेयर करें",
                    "Status: Completed & Ready"
                ),
                """हस्तलेख नोट / Project Checklist
1. लिपि स्कैन - OCR परीक्षण सफल
2. हस्तलिखित नोट्स को डिजिटल टेक्स्ट में बदलें
3. AI Summarize & Voice Output
4. PDF और TXT में आसानी से शेयर करें
Status: Completed & Ready"""
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
    STUDENT_LECTURE("Physics Lecture Notes", "Math formulas & cursive bullets"),
    MEETING_MINUTES("Sprint Planning Notes", "Action items, tasks & dates"),
    RECIPE_CARD("Handwritten Recipe", "Ingredients list & baking steps"),
    HINDI_MULTILINGUAL("हिंदी / Multilingual Note", "Regional language & English mixed")
}
