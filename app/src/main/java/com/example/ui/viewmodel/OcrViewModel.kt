package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiOcrService
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.local.ScanRepository
import com.example.util.ImageUtils
import com.example.util.PdfUtils
import com.example.util.SampleNoteType
import com.example.util.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OcrStatus {
    object Idle : OcrStatus
    data class Loading(val progressMessage: String) : OcrStatus
    data class Success(val text: String, val isInstantSample: Boolean = false) : OcrStatus
    data class Error(val message: String) : OcrStatus
}

sealed interface AiTaskStatus {
    object Idle : AiTaskStatus
    data class Processing(val taskName: String) : AiTaskStatus
    data class Completed(val result: String) : AiTaskStatus
    data class Failed(val error: String) : AiTaskStatus
}

enum class AiDialogType {
    NONE, SUMMARIZE, FIX_GRAMMAR, TRANSLATE, ASK_AI
}

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    private val geminiService = GeminiOcrService()
    val ttsManager = TtsManager(application)

    init {
        val db = AppDatabase.getInstance(application)
        repository = ScanRepository(db.scanDao())
    }

    // Navigation & Tabs (0 = Home/OCR, 1 = PDF Studio, 2 = History, 3 = Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Active image & processing state
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _enhanceContrast = MutableStateFlow(false)
    val enhanceContrast: StateFlow<Boolean> = _enhanceContrast.asStateFlow()

    private val _toGrayscale = MutableStateFlow(false)
    val toGrayscale: StateFlow<Boolean> = _toGrayscale.asStateFlow()

    // OCR result & editing fields
    private val _ocrStatus = MutableStateFlow<OcrStatus>(OcrStatus.Idle)
    val ocrStatus: StateFlow<OcrStatus> = _ocrStatus.asStateFlow()

    private val _editableText = MutableStateFlow("")
    val editableText: StateFlow<String> = _editableText.asStateFlow()

    private val _documentTitle = MutableStateFlow("Handwritten Note")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText.asStateFlow()

    private val _sourceType = MutableStateFlow("HANDWRITING")
    val sourceType: StateFlow<String> = _sourceType.asStateFlow()

    // AI Tools State
    private val _aiTaskStatus = MutableStateFlow<AiTaskStatus>(AiTaskStatus.Idle)
    val aiTaskStatus: StateFlow<AiTaskStatus> = _aiTaskStatus.asStateFlow()

    private val _activeAiDialog = MutableStateFlow(AiDialogType.NONE)
    val activeAiDialog: StateFlow<AiDialogType> = _activeAiDialog.asStateFlow()

    private val _aiResultText = MutableStateFlow("")
    val aiResultText: StateFlow<String> = _aiResultText.asStateFlow()

    // PDF Studio State
    private val _pdfPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val pdfPages: StateFlow<List<Bitmap>> = _pdfPages.asStateFlow()

    private val _selectedPdfIndex = MutableStateFlow(0)
    val selectedPdfIndex: StateFlow<Int> = _selectedPdfIndex.asStateFlow()

    private val _pdfName = MutableStateFlow("")
    val pdfName: StateFlow<String> = _pdfName.asStateFlow()

    // History & Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow("ALL")
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    val historyScans: StateFlow<List<ScanEntity>> = combine(
        repository.allScans,
        _searchQuery,
        _filterCategory
    ) { all, query, filter ->
        all.filter { scan ->
            val matchesQuery = query.isBlank() ||
                scan.title.contains(query, ignoreCase = true) ||
                scan.rawText.contains(query, ignoreCase = true) ||
                scan.summary.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "FAVORITES" -> scan.isFavorite
                "HANDWRITING" -> scan.source == "HANDWRITING" || scan.source == "CAMERA" || scan.source == "SAMPLE"
                "PDF" -> scan.source == "PDF"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings & Theme
    private val _selectedLanguage = MutableStateFlow("Auto Detect")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _darkModeSetting = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val darkModeSetting: StateFlow<Boolean?> = _darkModeSetting.asStateFlow()

    // ----------------------------------------------------
    // User Actions
    // ----------------------------------------------------

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun setDarkMode(dark: Boolean?) {
        _darkModeSetting.value = dark
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun updateEditableText(newText: String) {
        _editableText.value = newText
    }

    fun updateDocumentTitle(newTitle: String) {
        _documentTitle.value = newTitle
    }

    // Set Image from Camera / Gallery
    fun setImageBitmap(bitmap: Bitmap, source: String = "HANDWRITING", defaultTitle: String = "Scanned Note") {
        _originalBitmap.value = bitmap
        _rotationDegrees.value = 0f
        _enhanceContrast.value = false
        _toGrayscale.value = false
        _sourceType.value = source
        _documentTitle.value = defaultTitle
        _summaryText.value = ""
        _ocrStatus.value = OcrStatus.Idle
        _editableText.value = ""
        updateProcessedBitmap()
    }

    // Load from Sample Preset
    fun loadSampleNote(sampleType: SampleNoteType) {
        val (bitmap, expectedTranscript) = ImageUtils.createSampleNoteBitmap(sampleType)
        setImageBitmap(bitmap, source = "SAMPLE", defaultTitle = sampleType.displayName)
        _editableText.value = expectedTranscript
        _ocrStatus.value = OcrStatus.Success(expectedTranscript, isInstantSample = true)
        saveCurrentScanToHistory(autoSave = true)
    }

    fun rotateImage() {
        _rotationDegrees.value = (_rotationDegrees.value + 90f) % 360f
        updateProcessedBitmap()
    }

    fun toggleContrastEnhancement() {
        _enhanceContrast.value = !_enhanceContrast.value
        updateProcessedBitmap()
    }

    fun toggleGrayscale() {
        _toGrayscale.value = !_toGrayscale.value
        updateProcessedBitmap()
    }

    private fun updateProcessedBitmap() {
        val orig = _originalBitmap.value ?: return
        var bmp = orig
        if (_rotationDegrees.value != 0f) {
            bmp = ImageUtils.rotateBitmap(bmp, _rotationDegrees.value)
        }
        if (_enhanceContrast.value || _toGrayscale.value) {
            bmp = ImageUtils.applyHandwritingEnhancement(bmp, _enhanceContrast.value, _toGrayscale.value)
        }
        _processedBitmap.value = bmp
    }

    // Perform OCR with Gemini
    fun runHandwritingOcr() {
        val bitmap = _processedBitmap.value ?: _originalBitmap.value ?: return
        _ocrStatus.value = OcrStatus.Loading("Transcribing handwriting with AI...")
        viewModelScope.launch {
            val result = geminiService.recognizeHandwriting(bitmap, _selectedLanguage.value)
            result.onSuccess { text ->
                _editableText.value = text
                _ocrStatus.value = OcrStatus.Success(text)
                saveCurrentScanToHistory(autoSave = true)
            }.onFailure { error ->
                _ocrStatus.value = OcrStatus.Error(error.message ?: "Recognition failed")
            }
        }
    }

    // PDF Handlers
    fun loadPdfUri(uri: Uri, fileName: String) {
        _pdfName.value = fileName
        viewModelScope.launch {
            _ocrStatus.value = OcrStatus.Loading("Rendering PDF pages...")
            val pages = PdfUtils.renderPdfPages(getApplication(), uri)
            _pdfPages.value = pages
            _selectedPdfIndex.value = 0
            if (pages.isNotEmpty()) {
                setImageBitmap(pages[0], source = "PDF", defaultTitle = fileName)
                _ocrStatus.value = OcrStatus.Idle
            } else {
                _ocrStatus.value = OcrStatus.Error("Could not render PDF document.")
            }
        }
    }

    fun selectPdfPage(index: Int) {
        val pages = _pdfPages.value
        if (index in pages.indices) {
            _selectedPdfIndex.value = index
            setImageBitmap(pages[index], source = "PDF", defaultTitle = "${_pdfName.value} - Page ${index + 1}")
        }
    }

    // AI Tools
    fun openAiDialog(dialogType: AiDialogType) {
        _activeAiDialog.value = dialogType
        _aiTaskStatus.value = AiTaskStatus.Idle
        _aiResultText.value = ""
    }

    fun closeAiDialog() {
        _activeAiDialog.value = AiDialogType.NONE
        _aiTaskStatus.value = AiTaskStatus.Idle
    }

    fun performAiSummarize(style: String = "bullet_points") {
        val text = _editableText.value
        if (text.isBlank()) return
        _aiTaskStatus.value = AiTaskStatus.Processing("Generating AI summary...")
        viewModelScope.launch {
            val res = geminiService.summarizeText(text, style)
            res.onSuccess { summary ->
                _summaryText.value = summary
                _aiResultText.value = summary
                _aiTaskStatus.value = AiTaskStatus.Completed(summary)
                saveCurrentScanToHistory(autoSave = true)
            }.onFailure { err ->
                _aiTaskStatus.value = AiTaskStatus.Failed(err.message ?: "Summarization failed")
            }
        }
    }

    fun performAiFixGrammar() {
        val text = _editableText.value
        if (text.isBlank()) return
        _aiTaskStatus.value = AiTaskStatus.Processing("Polishing & correcting transcript...")
        viewModelScope.launch {
            val res = geminiService.fixAndFormatText(text)
            res.onSuccess { fixed ->
                _aiResultText.value = fixed
                _aiTaskStatus.value = AiTaskStatus.Completed(fixed)
            }.onFailure { err ->
                _aiTaskStatus.value = AiTaskStatus.Failed(err.message ?: "Correction failed")
            }
        }
    }

    fun performAiTranslate(targetLang: String) {
        val text = _editableText.value
        if (text.isBlank()) return
        _aiTaskStatus.value = AiTaskStatus.Processing("Translating to $targetLang...")
        viewModelScope.launch {
            val res = geminiService.translateText(text, targetLang)
            res.onSuccess { translated ->
                _aiResultText.value = translated
                _aiTaskStatus.value = AiTaskStatus.Completed(translated)
            }.onFailure { err ->
                _aiTaskStatus.value = AiTaskStatus.Failed(err.message ?: "Translation failed")
            }
        }
    }

    fun applyAiResultToEditor() {
        if (_aiResultText.value.isNotBlank()) {
            _editableText.value = _aiResultText.value
            closeAiDialog()
        }
    }

    // TTS Voice Output
    fun playVoiceOutput() {
        val text = _editableText.value
        if (text.isNotBlank()) {
            ttsManager.speak(text, _selectedLanguage.value)
        }
    }

    fun stopVoiceOutput() {
        ttsManager.stop()
    }

    fun setSpeechSpeed(speed: Float) {
        ttsManager.setSpeechRate(speed)
    }

    // History & Persistence Actions
    fun saveCurrentScanToHistory(autoSave: Boolean = false) {
        val text = _editableText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            val entity = ScanEntity(
                title = _documentTitle.value.ifBlank { "Untitled Note" },
                rawText = text,
                summary = _summaryText.value,
                language = _selectedLanguage.value,
                source = _sourceType.value
            )
            repository.insertScan(entity)
        }
    }

    fun loadScanFromHistory(scan: ScanEntity) {
        _documentTitle.value = scan.title
        _editableText.value = scan.rawText
        _summaryText.value = scan.summary
        _sourceType.value = scan.source
        _selectedLanguage.value = scan.language
        _ocrStatus.value = OcrStatus.Success(scan.rawText)
        _selectedTab.value = 0 // Navigate to Scan & OCR tab
    }

    fun toggleFavorite(scan: ScanEntity) {
        viewModelScope.launch {
            repository.updateScan(scan.copy(isFavorite = !scan.isFavorite))
        }
    }

    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllScans()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
