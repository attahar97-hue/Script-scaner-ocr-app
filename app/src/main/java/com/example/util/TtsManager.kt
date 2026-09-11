package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(_speechRate.value)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }
            })
            isInitialized = true
        } else {
            Log.e("TtsManager", "TTS initialization failed")
        }
    }

    fun speak(text: String, languageCode: String? = null) {
        if (!isInitialized || text.isBlank()) return
        stop()

        if (languageCode != null) {
            try {
                val locale = when (languageCode.lowercase()) {
                    "hindi", "hi" -> Locale("hi", "IN")
                    "spanish", "es" -> Locale("es", "ES")
                    "french", "fr" -> Locale("fr", "FR")
                    "german", "de" -> Locale("de", "DE")
                    "arabic", "ar" -> Locale("ar", "SA")
                    "urdu", "ur" -> Locale("ur", "PK")
                    else -> Locale.US
                }
                tts?.setLanguage(locale)
            } catch (e: Exception) {
                tts?.setLanguage(Locale.US)
            }
        }

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ScriptScanUtterance")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ScriptScanUtterance")
        _isPlaying.value = true
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
