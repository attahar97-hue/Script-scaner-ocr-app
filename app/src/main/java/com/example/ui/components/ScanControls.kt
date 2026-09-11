package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AiDialogType
import com.example.ui.viewmodel.OcrStatus
import com.example.ui.viewmodel.OcrViewModel
import com.example.util.PdfUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanImagePreviewCard(
    bitmap: Bitmap,
    viewModel: OcrViewModel,
    ocrStatus: OcrStatus
) {
    val contrast by viewModel.enhanceContrast.collectAsStateWithLifecycle()
    val grayscale by viewModel.toGrayscale.collectAsStateWithLifecycle()
    val selectedLang by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    var langDropdownExpanded by remember { mutableStateOf(false) }
    val supportedLangs = listOf("Auto Detect", "English", "Hindi", "Spanish", "French", "German", "Arabic", "Urdu", "Chinese", "Japanese")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_image_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Preview Header
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
                        text = "Image & Handwriting Enhancer",
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

            // Bitmap Image Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Enhancement Adjustments Bar
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.rotateImage() },
                    label = { Text("Rotate 90°", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("btn_rotate_image")
                )

                FilterChip(
                    selected = contrast,
                    onClick = { viewModel.toggleContrastEnhancement() },
                    label = { Text("Contrast Boost", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Contrast, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("btn_toggle_contrast")
                )

                FilterChip(
                    selected = grayscale,
                    onClick = { viewModel.toggleGrayscale() },
                    label = { Text("B&W / Grayscale", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.FilterBAndW, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("btn_toggle_grayscale")
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
                    Text("Extract Handwritten Text", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ocr_result_editor_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Note Title & Header Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Features Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.openAiDialog(AiDialogType.SUMMARIZE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_open_ai_summarize")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Summarize", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.openAiDialog(AiDialogType.FIX_GRAMMAR) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_open_ai_fix")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fix & Polish", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.openAiDialog(AiDialogType.TRANSLATE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_open_ai_translate")
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Translate", fontSize = 12.sp)
                }
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

            // Editable Digitized Text Field
            OutlinedTextField(
                value = editableText,
                onValueChange = { viewModel.updateEditableText(it) },
                label = { Text("Digitized Editable Text") },
                minLines = 6,
                maxLines = 14,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_editable_ocr_text"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text-To-Speech (Voice Output) Player Bar
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

            // Export & Share Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { PdfUtils.copyToClipboard(context, editableText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_copy_ocr_text"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp)
                }

                Button(
                    onClick = { PdfUtils.exportAndSharePdf(context, title, editableText, summaryText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_pdf"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 13.sp)
                }

                Button(
                    onClick = { PdfUtils.exportAndShareTxt(context, title, editableText, summaryText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_txt"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.TextSnippet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TXT", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 13.sp)
                }

                Button(
                    onClick = { PdfUtils.sharePlainText(context, editableText, title) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_share_text"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp)
                }
            }
        }
    }
}
