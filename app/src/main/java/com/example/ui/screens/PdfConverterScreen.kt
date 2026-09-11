package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.AiToolBottomSheet
import com.example.ui.components.OcrResultEditorCard
import com.example.ui.components.ScanImagePreviewCard
import com.example.ui.viewmodel.AiDialogType
import com.example.ui.viewmodel.OcrStatus
import com.example.ui.viewmodel.OcrViewModel

@Composable
fun PdfConverterScreen(viewModel: OcrViewModel) {
    val pdfPages by viewModel.pdfPages.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedPdfIndex.collectAsStateWithLifecycle()
    val pdfName by viewModel.pdfName.collectAsStateWithLifecycle()
    val ocrStatus by viewModel.ocrStatus.collectAsStateWithLifecycle()
    val editableText by viewModel.editableText.collectAsStateWithLifecycle()
    val documentTitle by viewModel.documentTitle.collectAsStateWithLifecycle()
    val activeAiDialog by viewModel.activeAiDialog.collectAsStateWithLifecycle()
    val aiTaskStatus by viewModel.aiTaskStatus.collectAsStateWithLifecycle()
    val aiResultText by viewModel.aiResultText.collectAsStateWithLifecycle()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
            viewModel.loadPdfUri(uri, fileName)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("pdf_converter_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // PDF Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PDF to Text Converter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Extract handwritten notes & printed content from scanned PDF files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Document Upload & Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (pdfPages.isEmpty()) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No PDF Loaded",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Select a PDF file from your device storage to extract text.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.testTag("btn_select_pdf_file")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select PDF Document")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = pdfName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${pdfPages.size} pages available",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.testTag("btn_change_pdf")
                                ) {
                                    Text("Change PDF", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Page Thumbnails Strip
                            Text(
                                text = "Select Page for OCR Conversion:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("pdf_pages_row")
                            ) {
                                itemsIndexed(pdfPages) { index, pageBmp ->
                                    val isSelected = index == selectedIndex
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .size(75.dp, 100.dp)
                                            .clickable { viewModel.selectPdfPage(index) }
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Image(
                                                bitmap = pageBmp.asImageBitmap(),
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(bottomStart = 8.dp),
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            ) {
                                                Text(
                                                    text = "P.${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // If a page is selected, display image enhancement preview
            if (pdfPages.isNotEmpty() && selectedIndex in pdfPages.indices) {
                item {
                    ScanImagePreviewCard(
                        bitmap = pdfPages[selectedIndex],
                        viewModel = viewModel,
                        ocrStatus = ocrStatus
                    )
                }
            }

            // Extracted OCR text editor
            if (editableText.isNotBlank() || ocrStatus is OcrStatus.Success) {
                item {
                    OcrResultEditorCard(
                        viewModel = viewModel,
                        editableText = editableText,
                        title = documentTitle,
                        ocrStatus = ocrStatus
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // AI Tool Modal Bottom Sheet
        if (activeAiDialog != AiDialogType.NONE) {
            AiToolBottomSheet(
                viewModel = viewModel,
                activeDialog = activeAiDialog,
                aiStatus = aiTaskStatus,
                aiResult = aiResultText,
                onDismiss = { viewModel.closeAiDialog() }
            )
        }
    }
}
