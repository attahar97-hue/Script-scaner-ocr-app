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

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun recognizeHandwriting(
        bitmap: Bitmap,
        languageHint: String = "Auto Detect"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured. Please set your key in the AI Studio Secrets panel.")
                )
            }

            // Downscale bitmap if too huge for fast transfer while keeping sharp lines
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1600)
            val base64Image = bitmapToBase64(scaledBitmap)

            val prompt = buildString {
                append("You are an expert Optical Character Recognition (OCR) engine specialized in handwriting recognition. ")
                append("Accurately transcribe all handwritten or printed text from this image. ")
                append("Handle messy handwriting, cursive scripts, scribbled notes, lists, headers, and mathematical symbols. ")
                append("Preserve the original layout, paragraph breaks, bullet points, and structure as faithfully as possible. ")
                if (languageHint != "Auto Detect") {
                    append("The handwritten text is likely in $languageHint language. ")
                }
                append("Provide ONLY the raw extracted transcript without conversational intro or markdown meta remarks.")
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Text prompt part
                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                // Image inlineData part
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // Generation config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1) // Low temperature for high OCR fidelity
                    put("topP", 0.95)
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(responseBody)
                    return@withContext Result.failure(Exception("OCR API Error (${response.code}): $errorMsg"))
                }

                val recognizedText = parseGeminiTextResponse(responseBody)
                if (recognizedText.isNotBlank()) {
                    Result.success(recognizedText)
                } else {
                    Result.failure(Exception("No readable text could be recognized in the image."))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiOcrService", "Handwriting recognition failed", e)
            Result.failure(e)
        }
    }

    suspend fun summarizeText(
        text: String,
        style: String = "bullet_points"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured. Please set your key in the AI Studio Secrets panel.")
                )
            }

            val prompt = when (style) {
                "bullet_points" -> "Summarize the following note into concise, high-impact bullet points capturing all key facts, takeaways, and action items:\n\n$text"
                "executive" -> "Provide an executive summary of the following document in 1-2 structured, clean paragraphs:\n\n$text"
                "action_items" -> "Extract all to-dos, tasks, dates, and actionable items from this note:\n\n$text"
                else -> "Summarize the key points of the following text:\n\n$text"
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
                    put("temperature", 0.4)
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(responseBody)
                    return@withContext Result.failure(Exception("Summarize Error (${response.code}): $errorMsg"))
                }

                val summaryText = parseGeminiTextResponse(responseBody)
                Result.success(summaryText)
            }
        } catch (e: Exception) {
            Log.e("GeminiOcrService", "Summarization failed", e)
            Result.failure(e)
        }
    }

    suspend fun fixAndFormatText(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("API key missing"))
            }

            val prompt = "Correct any OCR spelling mistakes, normalize messy punctuation, format paragraphs cleanly, while strictly preserving original meaning:\n\n$text"

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply { put("temperature", 0.2) })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Format Error: ${response.code}"))
                }
                Result.success(parseGeminiTextResponse(responseBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, targetLanguage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("API key missing"))
            }

            val prompt = "Translate the following scanned text accurately into $targetLanguage. Output only the translated text:\n\n$text"

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Translation Error: ${response.code}"))
                }
                Result.success(parseGeminiTextResponse(responseBody))
            }
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
