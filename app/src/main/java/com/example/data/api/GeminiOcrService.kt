package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiOcrService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Model candidates: priority gemini-3.6-flash as requested by user, with graceful fallbacks
    private val modelEndpoints = listOf(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"
    )

    private fun getApiKey(): String {
        val configuredKey = BuildConfig.GEMINI_API_KEY
        if (!configuredKey.isNullOrBlank() &&
            configuredKey != "MY_GEMINI_API_KEY" &&
            configuredKey != "your_api_key_here" &&
            !configuredKey.startsWith("your_")) {
            return configuredKey
        }
        return "AQ.Ab8RN6LvXn83zhsmfxn3YGjGK2gF51QZLXFQm8EL4XC6nZlGhw"
    }

    private suspend fun executeWithFallback(requestJson: JSONObject): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        var lastError = "Unknown error"

        for (endpoint in modelEndpoints) {
            try {
                val request = Request.Builder()
                    .url("$endpoint?key=$apiKey")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val parsed = parseGeminiTextResponse(responseBody)
                        if (parsed.isNotBlank()) {
                            return@withContext Result.success(parsed)
                        }
                    } else {
                        val errorMsg = parseErrorMessage(responseBody)
                        lastError = "API Error (${response.code}): $errorMsg"
                        Log.w("GeminiOcrService", "Failed on $endpoint: $lastError")
                        // If 404 (model unavailable) or 400, try next model in list
                        if (response.code == 404 || response.code == 400) {
                            continue
                        } else {
                            return@withContext Result.failure(Exception(lastError))
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Network error"
                Log.w("GeminiOcrService", "Exception on $endpoint", e)
            }
        }
        Result.failure(Exception(lastError))
    }

    suspend fun recognizeHandwriting(
        bitmap: Bitmap,
        languageHint: String = "Auto Detect"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()

            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 2400)
            val base64Image = bitmapToBase64(scaledBitmap)

            val prompt = buildString {
                append("You are an expert universal Optical Character Recognition (OCR) and handwriting digitizer engine. ")
                append("Accurately and COMPLETELY transcribe ALL handwritten, cursive, printed, or scribbled text from this image from top to bottom. ")
                append("Do NOT skip, summarize, or truncate any part of the page. Transcribe EVERY single word, paragraph, line, formula, bullet point, table, or note present on the page. ")
                append("Provide native support for Sindhi (سنڌي in Perso-Arabic script), Urdu (اردو in Nastaliq script), Arabic (العربية), Hindi (हिन्दी in Devanagari), and English. ")
                append("Convert handwritten characters into clean, perfectly typed digital Unicode characters as if typed on a computer keyboard. ")
                append("Preserve original layout, paragraph breaks, bullet points, numbers, and Right-to-Left (RTL) flow for Sindhi/Urdu/Arabic. ")
                if (languageHint != "Auto Detect") {
                    append("The document language is specified as $languageHint. ")
                } else {
                    append("Automatically detect the languages present in the image (including mixed Sindhi/Urdu/English). ")
                }
                append("Output the COMPLETE full extracted digitized text from the entire page without any preamble or markdown fences.")
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("topP", 0.95)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Log.e("GeminiOcrService", "Handwriting recognition failed", e)
            Result.failure(e)
        }
    }

    /**
     * Feature 4: Handwritten Math & Science Equation Solver
     */
    suspend fun solveHandwrittenMath(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 2400)
            val base64Image = bitmapToBase64(scaledBitmap)

            val prompt = """
                You are an advanced AI Mathematical and Scientific Solver.
                1. Transcribe the entire handwritten math equation, formula, or problem in the image into clean text and LaTeX notation.
                2. Solve the problem step-by-step with clear explanations.
                3. Highlight the final answer clearly at the end.
                Format clearly with:
                - [TRANSCRIPTION & LATEX]
                - [STEP-BY-STEP SOLUTION]
                - [FINAL ANSWER]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Feature 3: Table / Receipt / Form OCR to CSV
     */
    suspend fun extractTableToCsv(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 2400)
            val base64Image = bitmapToBase64(scaledBitmap)

            val prompt = "Transcribe the table, invoice, or columnar data from this image into comma-separated (CSV) format. Include headers in the first row. Output ONLY valid CSV lines without markdown code blocks."

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarizeText(
        text: String,
        style: String = "bullet_points"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = when (style) {
                "bullet_points" -> "Summarize the following note into comprehensive, clear bullet points capturing all facts, takeaways, and key points:\n\n$text"
                "executive" -> "Provide a comprehensive summary of the entire document in structured, clean paragraphs:\n\n$text"
                "action_items" -> "Extract all to-dos, tasks, dates, and actionable items from this note:\n\n$text"
                else -> "Summarize the key points of the following text thoroughly:\n\n$text"
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Log.e("GeminiOcrService", "Summarization failed", e)
            Result.failure(e)
        }
    }

    suspend fun fixAndFormatText(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Correct any OCR spelling mistakes, normalize messy punctuation, format paragraphs cleanly for the entire document, while strictly preserving original meaning:\n\n$text"

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, targetLanguage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Translate the following scanned document text COMPLETELY and accurately into $targetLanguage.
                Translate the ENTIRE text from beginning to end without omitting, skipping, or cutting off any paragraph.
                - If translating to Urdu (اردو), use standard typed Urdu vocabulary, proper grammar, and clean formatting.
                - If translating to Sindhi (سنڌي), use accurate Sindhi Perso-Arabic alphabet (ڪ, ڱ, ڄ, ڃ, ڦ, ڇ, ٽ, ڊ etc.) and natural grammar.
                - If translating to Hindi (हिन्दी), use clean Devanagari script.
                - If translating to English, produce clear, professional English.
                - Maintain all paragraph formatting, bullet points, headers, and structure.
                - Output ONLY the full translated text without commentary or notes.
                
                Text to translate:
                $text
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 8192)
                })
            }

            executeWithFallback(requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeminiTextResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                sb.append(part.optString("text", ""))
            }
            sb.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseErrorMessage(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val error = root.optJSONObject("error")
            error?.optString("message") ?: "Unknown error"
        } catch (e: Exception) {
            "HTTP error"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
