package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AiDialogType
import com.example.ui.viewmodel.OcrStatus
import com.example.ui.viewmodel.OcrViewModel
import com.example.util.DocumentFilterMode
import com.example.util.PdfUtils

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanImagePreviewCard(
    bitmap: Bitmap,
    viewModel: OcrViewModel,
    ocrStatus: OcrStatus
) {
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val selectedLang by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()

    var langDropdownExpanded by remember { mutableStateOf(false) }
    var showAdjustCropDialog by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    val supportedLangs = listOf("Auto Detect", "English", "سنڌي (Sindhi)", "اردو (Urdu)", "हिन्दी (Hindi)", "Arabic (العربية)", "Spanish", "French", "German", "Chinese", "Japanese")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_image_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Preview Header & Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Document Enhancer & OCR",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Language Selector
                ExposedDropdownMenuBox(
                    expanded = langDropdownExpanded,
                    onExpandedChange = { langDropdownExpanded = !langDropdownExpanded }
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .menuAnchor()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .testTag("lang_selector_surface")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedLang, style = MaterialTheme.typography.labelMedium)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = langDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false }
                    ) {
                        supportedLangs.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    langDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bitmap Image Canvas (Full width & responsive height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp, max = 460.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Handwritten note preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                // Quick Floating Actions: Fullscreen & CamScanner Crop
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { showFullScreenImage = true },
                        modifier = Modifier.size(34.dp).testTag("btn_preview_full_screen")
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showAdjustCropDialog = true },
                        modifier = Modifier.size(34.dp).testTag("btn_preview_crop_angles")
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = "Adjust Angles", tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-Crop, Rotate & Document Filters Strip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { showAdjustCropDialog = true },
                    label = { Text("A4 & Angle Crop (✔)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                    leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF00C853)) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF00C853).copy(alpha = 0.15f)),
                    modifier = Modifier.testTag("btn_camscanner_crop_chip")
                )

                FilterChip(
                    selected = false,
                    onClick = { viewModel.triggerAutoCrop() },
                    label = { Text("Auto Crop", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.testTag("btn_auto_crop")
                )

                FilterChip(
                    selected = false,
                    onClick = { viewModel.rotateImage() },
                    label = { Text("Rotate 90°", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("btn_rotate_image")
                )

                FilterChip(
                    selected = activeFilter == DocumentFilterMode.PRINT_READY,
                    onClick = { viewModel.setDocumentFilter(if (activeFilter == DocumentFilterMode.PRINT_READY) DocumentFilterMode.ORIGINAL else DocumentFilterMode.PRINT_READY) },
                    label = { Text("Print-Ready (پرنٹ)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.testTag("btn_filter_print_ready")
                )

                FilterChip(
                    selected = activeFilter == DocumentFilterMode.MAGIC_COLOR,
                    onClick = { viewModel.setDocumentFilter(if (activeFilter == DocumentFilterMode.MAGIC_COLOR) DocumentFilterMode.ORIGINAL else DocumentFilterMode.MAGIC_COLOR) },
                    label = { Text("Magic Color", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )

                FilterChip(
                    selected = activeFilter == DocumentFilterMode.BLACK_AND_WHITE,
                    onClick = { viewModel.setDocumentFilter(if (activeFilter == DocumentFilterMode.BLACK_AND_WHITE) DocumentFilterMode.ORIGINAL else DocumentFilterMode.BLACK_AND_WHITE) },
                    label = { Text("B&W Clean", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )

                FilterChip(
                    selected = activeFilter == DocumentFilterMode.GRAYSCALE,
                    onClick = { viewModel.setDocumentFilter(if (activeFilter == DocumentFilterMode.GRAYSCALE) DocumentFilterMode.ORIGINAL else DocumentFilterMode.GRAYSCALE) },
                    label = { Text("Grayscale", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }

            if (showAdjustCropDialog) {
                DocumentAngleCropDialog(
                    originalBitmap = bitmap,
                    onDismiss = { showAdjustCropDialog = false },
                    onCropConfirmed = { cropped ->
                        showAdjustCropDialog = false
                        viewModel.setImageBitmap(cropped, source = "CAMSCANNER", defaultTitle = "Scanned Note")
                        viewModel.runHandwritingOcr()
                    }
                )
            }

            if (showFullScreenImage) {
                Dialog(
                    onDismissRequest = { showFullScreenImage = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .testTag("full_screen_image_dialog"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full Screen Document",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = { showFullScreenImage = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color(0x88000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Special Tools Row: Math Solver, Signature, Table CSV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionToolCard(
                    title = "Solve Math",
                    icon = Icons.Default.Functions,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.runMathSolver() },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_solve_math"
                )

                AiActionToolCard(
                    title = "Signature",
                    icon = Icons.Default.Draw,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.extractDigitalSignature() },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_signature"
                )

                AiActionToolCard(
                    title = "To CSV",
                    icon = Icons.Default.TableChart,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { viewModel.performTableToCsv() },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_table_to_csv"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extract Text (OCR) Main CTA Button
            Button(
                onClick = { viewModel.runHandwritingOcr() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_extract_text_ocr"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = ocrStatus !is OcrStatus.Loading
            ) {
                if (ocrStatus is OcrStatus.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ocrStatus.progressMessage, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOffline) "Extract Text (Offline Mode)" else "Extract Handwritten Text (AI)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrResultEditorCard(
    viewModel: OcrViewModel,
    editableText: String,
    title: String,
    ocrStatus: OcrStatus
) {
    val context = LocalContext.current
    val summaryText by viewModel.summaryText.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.ttsManager.isPlaying.collectAsStateWithLifecycle()
    val speechRate by viewModel.ttsManager.speechRate.collectAsStateWithLifecycle()
    var isExpandedView by remember { mutableStateOf(false) }

    val wordCount = remember(editableText) {
        if (editableText.isBlank()) 0 else editableText.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(editableText) { editableText.length }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ocr_result_editor_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Note Title
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateDocumentTitle(it) },
                label = { Text("Note Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_document_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Features Action Row: Summarize, Fix & Polish, Translate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionToolCard(
                    title = "Summarize",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.openAiDialog(AiDialogType.SUMMARIZE) },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_open_ai_summarize"
                )

                AiActionToolCard(
                    title = "Fix & Polish",
                    icon = Icons.Default.Edit,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.openAiDialog(AiDialogType.FIX_GRAMMAR) },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_open_ai_fix"
                )

                AiActionToolCard(
                    title = "Translate",
                    icon = Icons.Default.Translate,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { viewModel.openAiDialog(AiDialogType.TRANSLATE) },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_open_ai_translate"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Summary Banner if generated
            AnimatedVisibility(visible = summaryText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Key Takeaways",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { PdfUtils.copyToClipboard(context, summaryText) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Header for Editable Digitized Text with Expand/Collapse & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Full Digitized Text",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$wordCount words • $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpandedView = !isExpandedView },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpandedView) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isExpandedView) "Standard View" else "Expand Full Page",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Editable Digitized Text Field with full page expansion
            OutlinedTextField(
                value = editableText,
                onValueChange = { viewModel.updateEditableText(it) },
                label = { Text(if (isExpandedView) "Full Page Digitized Content" else "Digitized Editable Text") },
                minLines = if (isExpandedView) 18 else 8,
                maxLines = if (isExpandedView) 60 else 18,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_editable_ocr_text"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Multilingual Translation Quick-Bar
            Text(
                text = "⚡ 1-Tap Quick Translation (ترجمو / ترجمہ):",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "English" to "🌐 English",
                    "Urdu (اردو)" to "🌐 اردو",
                    "Sindhi (سنڌي)" to "🌐 سنڌي",
                    "Hindi (हिन्दी)" to "🌐 हिन्दी",
                    "Arabic (العربية)" to "🌐 العربية"
                ).forEach { (targetLang, label) ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.runQuickTranslate(targetLang) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Player Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) viewModel.stopVoiceOutput() else viewModel.playVoiceOutput()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .testTag("btn_voice_tts_play")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop Voice" else "Read Aloud",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSpeaking) "Reading aloud..." else "Voice Output (TTS)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Hear notes aloud with natural voice",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Speed Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.75f, 1.0f, 1.25f).forEach { speed ->
                            FilterChip(
                                selected = speechRate == speed,
                                onClick = { viewModel.setSpeechSpeed(speed) },
                                label = { Text("${speed}x", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature 3: Full Professional Export Bar (PDF, Word, CSV, TXT, Copy, Share)
            Text(
                text = "Export & Share Document Formats:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { PdfUtils.copyToClipboard(context, editableText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                Button(
                    onClick = { PdfUtils.exportAndSharePdf(context, title, editableText, summaryText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { PdfUtils.exportAndShareWord(context, title, editableText, summaryText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Word (.doc)", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { PdfUtils.exportAndShareCsv(context, title, editableText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel/CSV", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { PdfUtils.exportAndShareTxt(context, title, editableText, summaryText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TextSnippet, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TXT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                Button(
                    onClick = { PdfUtils.sharePlainText(context, editableText, title) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AiActionToolCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
