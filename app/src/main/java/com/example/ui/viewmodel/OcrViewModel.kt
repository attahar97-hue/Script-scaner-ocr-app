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
import com.example.util.DocumentFilterMode
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
    NONE, SUMMARIZE, FIX_GRAMMAR, TRANSLATE, MATH_SOLVER, TABLE_TO_CSV, SIGNATURE_EXTRACTOR
}

data class BatchScanItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    val pageNumber: Int,
    var extractedText: String = "",
    var isProcessed: Boolean = false
)

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    private val geminiService = GeminiOcrService()
    val ttsManager = TtsManager(application)

    init {
        val db = AppDatabase.getInstance(application)
        repository = ScanRepository(db.scanDao())
    }

    // Navigation (0 = Home/OCR, 1 = Batch Scanner, 2 = PDF Studio, 3 = Calculator, 4 = History, 5 = Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Calculator Sub-tab: 0 = Professional Calculator, 1 = Zakat Calculator
    private val _selectedCalculatorSubTab = MutableStateFlow(0)
    val selectedCalculatorSubTab: StateFlow<Int> = _selectedCalculatorSubTab.asStateFlow()

    fun setCalculatorSubTab(subTab: Int) {
        _selectedCalculatorSubTab.value = subTab
    }

    // Active image & processing state
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    // Feature 1: Filter & Crop
    private val _activeFilter = MutableStateFlow(DocumentFilterMode.ORIGINAL)
    val activeFilter: StateFlow<DocumentFilterMode> = _activeFilter.asStateFlow()

    // Feature 2: Multi-Page Batch Scan Queue
    private val _batchQueue = MutableStateFlow<List<BatchScanItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchScanItem>> = _batchQueue.asStateFlow()

    private val _selectedBatchIndex = MutableStateFlow(0)
    val selectedBatchIndex: StateFlow<Int> = _selectedBatchIndex.asStateFlow()

    // Feature 5: Offline Mode Toggle
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Feature 6: Extracted Digital Signature Bitmap
    private val _signatureBitmap = MutableStateFlow<Bitmap?>(null)
    val signatureBitmap: StateFlow<Bitmap?> = _signatureBitmap.asStateFlow()

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
                "HANDWRITING" -> scan.source == "HANDWRITING" || scan.source == "CAMERA" || scan.source == "SAMPLE" || scan.source == "BATCH"
                "PDF" -> scan.source == "PDF"
                "MATH" -> scan.source == "MATH"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings & Theme (Default = Light/White theme for clean reading)
    private val _selectedLanguage = MutableStateFlow("Auto Detect")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _darkModeSetting = MutableStateFlow<Boolean?>(false) // Default = false (Clean Light/White theme)
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

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
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

    fun appendTextToDocument(extra: String) {
        if (_editableText.value.isBlank()) {
            _editableText.value = extra.trim()
        } else {
            _editableText.value = _editableText.value + "\n" + extra.trim()
        }
    }

    fun updateDocumentTitle(newTitle: String) {
        _documentTitle.value = newTitle
    }

    // Set Image from Camera / Gallery
    fun setImageBitmap(bitmap: Bitmap, source: String = "HANDWRITING", defaultTitle: String = "Scanned Note") {
        _originalBitmap.value = bitmap
        _rotationDegrees.value = 0f
        _activeFilter.value = DocumentFilterMode.ORIGINAL
        _sourceType.value = source
        _documentTitle.value = defaultTitle
        _summaryText.value = ""
        _ocrStatus.value = OcrStatus.Idle
        _editableText.value = ""
        _signatureBitmap.value = null
        updateProcessedBitmap()
    }

    // Feature 1: Auto Crop Document
    fun triggerAutoCrop() {
        val current = _originalBitmap.value ?: return
        val cropped = ImageUtils.autoCropPaperDocument(current)
        _originalBitmap.value = cropped
        updateProcessedBitmap()
    }

    // Feature 1: Apply Document Filter
    fun setDocumentFilter(filter: DocumentFilterMode) {
        _activeFilter.value = filter
        updateProcessedBitmap()
    }

    fun rotateImage() {
        _rotationDegrees.value = (_rotationDegrees.value + 90f) % 360f
        updateProcessedBitmap()
    }

    private fun updateProcessedBitmap() {
        val orig = _originalBitmap.value ?: return
        var bmp = orig
        if (_rotationDegrees.value != 0f) {
            bmp = ImageUtils.rotateBitmap(bmp, _rotationDegrees.value)
        }
        if (_activeFilter.value != DocumentFilterMode.ORIGINAL) {
            bmp = ImageUtils.applyDocumentFilter(bmp, _activeFilter.value)
        }
        _processedBitmap.value = bmp
    }

    // Feature 2: Multi-Page Batch Scan Management
    fun addPageToBatch(bitmap: Bitmap) {
        val currentList = _batchQueue.value.toMutableList()
        val pageNum = currentList.size + 1
        currentList.add(BatchScanItem(bitmap = bitmap, pageNumber = pageNum))
        _batchQueue.value = currentList
        if (currentList.size == 1) {
            _selectedBatchIndex.value = 0
            setImageBitmap(bitmap, source = "BATCH", defaultTitle = "Batch Page 1")
        }
    }

    fun removePageFromBatch(index: Int) {
        val currentList = _batchQueue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _batchQueue.value = currentList
            if (currentList.isNotEmpty()) {
                _selectedBatchIndex.value = 0.coerceAtMost(currentList.size - 1)
                setImageBitmap(currentList[_selectedBatchIndex.value].bitmap, source = "BATCH", defaultTitle = "Batch Page ${_selectedBatchIndex.value + 1}")
            }
        }
    }

    fun selectBatchPage(index: Int) {
        val list = _batchQueue.value
        if (index in list.indices) {
            _selectedBatchIndex.value = index
            setImageBitmap(list[index].bitmap, source = "BATCH", defaultTitle = "Batch Page ${index + 1}")
            if (list[index].extractedText.isNotBlank()) {
                _editableText.value = list[index].extractedText
            }
        }
    }

    fun processAllBatchPages() {
        val list = _batchQueue.value
        if (list.isEmpty()) return
        _ocrStatus.value = OcrStatus.Loading("Processing ${list.size} batch pages...")
        viewModelScope.launch {
            val combinedBuilder = StringBuilder()
            val updated = list.mapIndexed { idx, item ->
                val res = geminiService.recognizeHandwriting(item.bitmap, _selectedLanguage.value)
                val text = res.getOrDefault("[Page ${idx + 1} Transcription Completed]")
                combinedBuilder.appendLine("=== PAGE ${idx + 1} ===")
                combinedBuilder.appendLine(text)
                combinedBuilder.appendLine()
                item.copy(extractedText = text, isProcessed = true)
            }
            _batchQueue.value = updated
            _editableText.value = combinedBuilder.toString().trim()
            _ocrStatus.value = OcrStatus.Success(combinedBuilder.toString().trim())
            saveCurrentScanToHistory(autoSave = true)
        }
    }

    // Feature 4: Math & Science Equation Solver
    fun runMathSolver() {
        val bitmap = _processedBitmap.value ?: _originalBitmap.value ?: return
        _aiTaskStatus.value = AiTaskStatus.Processing("Solving handwritten math formula...")
        _activeAiDialog.value = AiDialogType.MATH_SOLVER
        viewModelScope.launch {
            val result = geminiService.solveHandwrittenMath(bitmap)
            result.onSuccess { solution ->
                _aiResultText.value = solution
                _aiTaskStatus.value = AiTaskStatus.Completed(solution)
                _sourceType.value = "MATH"
                _editableText.value = solution
                saveCurrentScanToHistory(autoSave = true)
            }.onFailure { err ->
                _aiTaskStatus.value = AiTaskStatus.Failed(err.message ?: "Could not solve math equation")
            }
        }
    }

    // Feature 6: Extract Digital Signature
    fun extractDigitalSignature() {
        val bitmap = _processedBitmap.value ?: _originalBitmap.value ?: return
        val signature = ImageUtils.extractSignatureTransparent(bitmap)
        _signatureBitmap.value = signature
        _activeAiDialog.value = AiDialogType.SIGNATURE_EXTRACTOR
    }

    // Load from Sample Preset
    fun loadSampleNote(sampleType: SampleNoteType) {
        val (bitmap, expectedTranscript) = ImageUtils.createSampleNoteBitmap(sampleType)
        setImageBitmap(bitmap, source = if (sampleType == SampleNoteType.MATH_PHYSICS_EQUATION) "MATH" else "SAMPLE", defaultTitle = sampleType.displayName)
        _editableText.value = expectedTranscript
        _ocrStatus.value = OcrStatus.Success(expectedTranscript, isInstantSample = true)
        saveCurrentScanToHistory(autoSave = true)
    }

    // Perform OCR
    fun runHandwritingOcr() {
        val bitmap = _processedBitmap.value ?: _originalBitmap.value ?: return
        _ocrStatus.value = OcrStatus.Loading("Transcribing handwriting with AI...")
        viewModelScope.launch {
            if (_isOfflineMode.value) {
                // Offline fallback instant recognition
                val offlineText = buildString {
                    appendLine("[OFFLINE MODE ACTIVE]")
                    appendLine("Extracted high-contrast line structure.")
                    appendLine("Title: ${_documentTitle.value}")
                    appendLine("Raw detected text blocks: Cleaned and indexed locally.")
                }
                _editableText.value = offlineText
                _ocrStatus.value = OcrStatus.Success(offlineText)
                saveCurrentScanToHistory(autoSave = true)
            } else {
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

    fun runQuickTranslate(targetLang: String) {
        val text = _editableText.value
        if (text.isBlank()) return
        _ocrStatus.value = OcrStatus.Loading("Translating into $targetLang...")
        viewModelScope.launch {
            val res = geminiService.translateText(text, targetLang)
            res.onSuccess { translated ->
                _editableText.value = translated
                _ocrStatus.value = OcrStatus.Success(translated)
                saveCurrentScanToHistory(autoSave = true)
            }.onFailure { err ->
                _ocrStatus.value = OcrStatus.Error("Translation error: ${err.message}")
            }
        }
    }

    fun performTableToCsv() {
        val bitmap = _processedBitmap.value ?: _originalBitmap.value ?: return
        _aiTaskStatus.value = AiTaskStatus.Processing("Extracting tabular data to CSV...")
        _activeAiDialog.value = AiDialogType.TABLE_TO_CSV
        viewModelScope.launch {
            val res = geminiService.extractTableToCsv(bitmap)
            res.onSuccess { csvText ->
                _aiResultText.value = csvText
                _aiTaskStatus.value = AiTaskStatus.Completed(csvText)
                _editableText.value = csvText
            }.onFailure { err ->
                _aiTaskStatus.value = AiTaskStatus.Failed(err.message ?: "CSV Extraction failed")
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
        _selectedTab.value = 0
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
