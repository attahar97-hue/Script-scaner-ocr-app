package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AiDialogType
import com.example.ui.viewmodel.AiTaskStatus
import com.example.ui.viewmodel.OcrViewModel
import com.example.util.PdfUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolBottomSheet(
    viewModel: OcrViewModel,
    activeDialog: AiDialogType,
    aiStatus: AiTaskStatus,
    aiResult: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val signatureBmp by viewModel.signatureBitmap.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_tool_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (activeDialog) {
                                AiDialogType.SUMMARIZE -> Icons.Default.AutoAwesome
                                AiDialogType.FIX_GRAMMAR -> Icons.Default.Check
                                AiDialogType.TRANSLATE -> Icons.Default.Translate
                                AiDialogType.MATH_SOLVER -> Icons.Default.Functions
                                AiDialogType.TABLE_TO_CSV -> Icons.Default.TableChart
                                AiDialogType.SIGNATURE_EXTRACTOR -> Icons.Default.Draw
                                else -> Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (activeDialog) {
                                AiDialogType.SUMMARIZE -> "AI Note Summarizer"
                                AiDialogType.FIX_GRAMMAR -> "AI Fix & Polish"
                                AiDialogType.TRANSLATE -> "AI Note Translator"
                                AiDialogType.MATH_SOLVER -> "Math & Equation Solver"
                                AiDialogType.TABLE_TO_CSV -> "Table & Form to CSV"
                                AiDialogType.SIGNATURE_EXTRACTOR -> "Digital Signature Extractor"
                                else -> "AI Assistant"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (activeDialog) {
                                AiDialogType.SUMMARIZE -> "Extract key points & action items"
                                AiDialogType.FIX_GRAMMAR -> "Fix handwriting OCR spelling & grammar"
                                AiDialogType.TRANSLATE -> "Translate into regional & global languages"
                                AiDialogType.MATH_SOLVER -> "LaTeX transcription & step-by-step solutions"
                                AiDialogType.TABLE_TO_CSV -> "Convert handwritten tables to Excel/CSV"
                                AiDialogType.SIGNATURE_EXTRACTOR -> "Cut out ink signature with transparent PNG"
                                else -> "Smart AI tools"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_ai_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signature Extractor Mode
            if (activeDialog == AiDialogType.SIGNATURE_EXTRACTOR && signatureBmp != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Transparent PNG Preview (Paper Removed)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Checkerboard / Canvas preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = signatureBmp!!.asImageBitmap(),
                                contentDescription = "Extracted signature",
                                modifier = Modifier.padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                PdfUtils.exportAndShareSignaturePng(context, signatureBmp!!)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Transparent Signature (.PNG)")
                        }
                    }
                }
            }

            // Summary Action Triggers
            if (activeDialog == AiDialogType.SUMMARIZE) {
                Text(
                    text = "Choose Summary Style:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.performAiSummarize("bullet_points") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_summary_bullets")
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bullets", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.performAiSummarize("executive") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_summary_executive")
                    ) {
                        Icon(Icons.Default.ShortText, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Brief", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.performAiSummarize("action_items") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_summary_actions")
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tasks", fontSize = 12.sp)
                    }
                }
            }

            // Translate Action Triggers
            if (activeDialog == AiDialogType.TRANSLATE) {
                var expanded by remember { mutableStateOf(false) }
                var targetLang by remember { mutableStateOf("Hindi") }
                val languages = listOf("Urdu", "Hindi", "Spanish", "French", "German", "Arabic", "Chinese", "Japanese", "English")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = targetLang,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_target_language")
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    targetLang = lang
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.performAiTranslate(targetLang) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_translate_action")
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate Now")
                }
            }

            // Fix Grammar Action Trigger
            if (activeDialog == AiDialogType.FIX_GRAMMAR) {
                Button(
                    onClick = { viewModel.performAiFixGrammar() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_fix_grammar_action")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fix OCR Errors & Format")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Status & Result View
            when (aiStatus) {
                is AiTaskStatus.Processing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = aiStatus.taskName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is AiTaskStatus.Failed -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Error: ${aiStatus.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is AiTaskStatus.Completed -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activeDialog == AiDialogType.MATH_SOLVER) "Math & LaTeX Solution" else "Generated Result",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    IconButton(
                                        onClick = { PdfUtils.copyToClipboard(context, aiStatus.result) },
                                        modifier = Modifier.size(32.dp).testTag("copy_ai_result_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiStatus.result,
                                style = if (activeDialog == AiDialogType.MATH_SOLVER || activeDialog == AiDialogType.TABLE_TO_CSV) {
                                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("ai_result_text")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeDialog == AiDialogType.TABLE_TO_CSV) {
                                    Button(
                                        onClick = {
                                            PdfUtils.exportAndShareCsv(context, "Scanned_Table", aiStatus.result)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export CSV")
                                    }
                                }

                                Button(
                                    onClick = { viewModel.applyAiResultToEditor() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("btn_apply_ai_to_editor")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply to Note")
                                }
                            }
                        }
                    }
                }

                AiTaskStatus.Idle -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
