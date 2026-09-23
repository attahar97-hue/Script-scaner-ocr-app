package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AiToolBottomSheet
import com.example.ui.components.OcrResultEditorCard
import com.example.ui.components.ScanImagePreviewCard
import com.example.ui.viewmodel.AiDialogType
import com.example.ui.viewmodel.OcrStatus
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.CamScannerCameraDialog
import com.example.ui.components.DocumentAngleCropDialog
import com.example.ui.viewmodel.OcrViewModel
import com.example.util.ImageUtils
import com.example.util.SampleNoteType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: OcrViewModel) {
    val context = LocalContext.current
    val processedBitmap by viewModel.processedBitmap.collectAsStateWithLifecycle()
    val originalBitmap by viewModel.originalBitmap.collectAsStateWithLifecycle()
    val ocrStatus by viewModel.ocrStatus.collectAsStateWithLifecycle()
    val editableText by viewModel.editableText.collectAsStateWithLifecycle()
    val documentTitle by viewModel.documentTitle.collectAsStateWithLifecycle()
    val activeAiDialog by viewModel.activeAiDialog.collectAsStateWithLifecycle()
    val aiTaskStatus by viewModel.aiTaskStatus.collectAsStateWithLifecycle()
    val aiResultText by viewModel.aiResultText.collectAsStateWithLifecycle()

    var showCameraDialog by remember { mutableStateOf(false) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera Capture Launcher (fallback system camera)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            pendingCropBitmap = bitmap
        }
    }

    // Photo Picker Launcher (zero permission Android photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                pendingCropBitmap = bitmap
            } else {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF Document Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadPdfUri(uri, "Document_${System.currentTimeMillis() % 1000}.pdf")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("home_scrollable_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Hero Banner & Branding Card
                HeroHeaderCard()
            }

            item {
                // Quick Capture & Input Action Grid
                Text(
                    text = "Scan or Upload Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScanSourceCard(
                        title = "Camera",
                        subtitle = "A4 & Angle",
                        icon = Icons.Default.CameraAlt,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_scan_camera"),
                        onClick = {
                            showCameraDialog = true
                        }
                    )

                    ScanSourceCard(
                        title = "Gallery",
                        subtitle = "Auto crop",
                        icon = Icons.Default.Collections,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_scan_gallery"),
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    ScanSourceCard(
                        title = "PDF File",
                        subtitle = "Extract PDF",
                        icon = Icons.Default.PictureAsPdf,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_scan_pdf"),
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Separate Quick Access Card 1: Professional Calculator (کاروباری حساب و کتاب / بل کیلکولیٹر)
                Surface(
                    onClick = {
                        viewModel.setCalculatorSubTab(0)
                        viewModel.setTab(3)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_pro_calculator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = "Calculator Logo",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "پروفیشنل کاروباری کیلکولیٹر (Business Calculator)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "بل، خریداری، منافع اور سائنسی ریاضیاتی حساب کتاب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Separate Quick Access Card 2: Islamic Zakat Calculator (اسلامی زکوٰۃ و نصاب کیلکولیٹر)
                Surface(
                    onClick = {
                        viewModel.setCalculatorSubTab(1)
                        viewModel.setTab(3)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_zakat_calculator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF00C853),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Diamond,
                                        contentDescription = "Zakat Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "اسلامی زکوٰۃ کیلکولیٹر (Zakat & Nisab)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سونا، چاندی، کیش، قرض منہا اور عالمی کرنسیوں میں 2.5% زکوٰۃ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                // Preset Sample Handwritten Notes for Quick Testing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Instant Handwriting Samples",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "1-Tap Test",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("sample_notes_row")
                ) {
                    items(SampleNoteType.values()) { sample ->
                        SampleNoteChip(
                            sampleType = sample,
                            onClick = { viewModel.loadSampleNote(sample) }
                        )
                    }
                }
            }

            // Image Preview & Enhancement Card
            if (processedBitmap != null || originalBitmap != null) {
                item {
                    val activeBmp = processedBitmap ?: originalBitmap!!
                    ScanImagePreviewCard(
                        bitmap = activeBmp,
                        viewModel = viewModel,
                        ocrStatus = ocrStatus
                    )
                }
            }

            // Digitized Text Result Editor Card
            if (editableText.isNotBlank() || ocrStatus is OcrStatus.Success) {
                item {
                    OcrResultEditorCard(
                        viewModel = viewModel,
                        editableText = editableText,
                        title = documentTitle,
                        ocrStatus = ocrStatus
                    )
                }
            }

            // Features Highlight Card
            item {
                FeaturesHighlightSection()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // CamScanner Live Viewfinder Dialog
        if (showCameraDialog) {
            CamScannerCameraDialog(
                onDismiss = { showCameraDialog = false },
                onImageCaptured = { bmp ->
                    showCameraDialog = false
                    pendingCropBitmap = bmp
                },
                onOpenGallery = {
                    showCameraDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // CamScanner 4-Corner Perspective Angle Crop Dialog
        if (pendingCropBitmap != null) {
            DocumentAngleCropDialog(
                originalBitmap = pendingCropBitmap!!,
                onDismiss = { pendingCropBitmap = null },
                onCropConfirmed = { cropped ->
                    pendingCropBitmap = null
                    viewModel.setImageBitmap(cropped, source = "CAMSCANNER", defaultTitle = "Scanned Document")
                    viewModel.runHandwritingOcr()
                }
            )
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

@Composable
fun HeroHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo Icon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_icon),
                        contentDescription = "ScriptScan Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ScriptScan OCR",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Handwriting to Editable Digital Text",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AI Powered",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Cursive & Messy",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanSourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SampleNoteChip(
    sampleType: SampleNoteType,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .testTag("sample_chip_${sampleType.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Draw,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = sampleType.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sampleType.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun FeaturesHighlightSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Why ScriptScan OCR?",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            FeatureBulletItem(
                icon = Icons.Default.Draw,
                title = "Advanced Handwriting Engine",
                description = "Recognizes cursive, printed, and messy scribbled notes accurately."
            )
            Spacer(modifier = Modifier.height(8.dp))

            FeatureBulletItem(
                icon = Icons.Default.AutoAwesome,
                title = "AI Note Summarization",
                description = "Extract key bullet points, executive briefings, and action items in seconds."
            )
            Spacer(modifier = Modifier.height(8.dp))

            FeatureBulletItem(
                icon = Icons.Default.PictureAsPdf,
                title = "PDF to Text & Formats",
                description = "Convert multi-page PDF documents and export to .PDF or .TXT instantly."
            )
            Spacer(modifier = Modifier.height(8.dp))

            FeatureBulletItem(
                icon = Icons.Default.Translate,
                title = "Multi-Language & Voice",
                description = "Supports English, Hindi, and regional languages with text-to-speech aloud."
            )
        }
    }
}

@Composable
fun FeatureBulletItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
