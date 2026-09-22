package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.util.ImageUtils
import java.io.File
import java.nio.ByteBuffer

enum class ScanDocType(val label: String, val tip: String) {
    A4_DOCUMENT("A4 Document", "Align A4 page inside the green borders"),
    CNIC_CARD("ID / CNIC Card", "Place front/back of CNIC inside frame"),
    BOOK_NOTE("Note / Book", "Hold steady over notebook or paper")
}

/**
 * CamScanner Live Camera Viewfinder Dialog
 * High-definition camera capture with real-time A4/CNIC framing, flashlight control,
 * and seamless fallback to high-resolution system camera.
 */
@Composable
fun CamScannerCameraDialog(
    onDismiss: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission needed to scan documents", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // High-resolution system camera fallback using FileProvider
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val bmp = ImageUtils.loadBitmapFromUri(context, tempPhotoUri!!)
            if (bmp != null) {
                onImageCaptured(bmp)
            }
        }
    }

    var selectedDocType by remember { mutableStateOf(ScanDocType.A4_DOCUMENT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isGridVisible by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camscanner_camera_dialog"),
            color = Color.Black
        ) {
            if (!hasCameraPermission) {
                // Permission Fallback Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Access Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To scan A4 documents, CNIC cards, and notes in real-time, please allow camera permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.testTag("btn_grant_camera_permission")
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cacheFile
                            )
                            tempPhotoUri = uri
                            systemCameraLauncher.launch(uri)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                    ) {
                        Text("Use System Camera App", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            } else {
                // Live CameraX Viewfinder
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                    .build()
                                imageCaptureInstance = imageCapture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                    cameraInstance = cam
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        }
                    )

                    // CamScanner Viewfinder Overlay & Framing Guides
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Calculate frame size based on selected doc type
                        val (frameW, frameH) = when (selectedDocType) {
                            ScanDocType.A4_DOCUMENT -> {
                                // A4 Portrait Aspect Ratio 1 : 1.414
                                val fw = w * 0.86f
                                val fh = fw * 1.38f
                                Pair(fw, minOf(fh, h * 0.72f))
                            }
                            ScanDocType.CNIC_CARD -> {
                                // CNIC Card Aspect Ratio 1.58 : 1
                                val fw = w * 0.88f
                                val fh = fw / 1.58f
                                Pair(fw, fh)
                            }
                            ScanDocType.BOOK_NOTE -> {
                                Pair(w * 0.88f, h * 0.70f)
                            }
                        }

                        val left = (w - frameW) / 2f
                        val top = (h - frameH) / 2f - 30f // slight upward bias for bottom controls
                        val right = left + frameW
                        val bottom = top + frameH

                        val neonGreen = Color(0xFF00E676)

                        // Outer translucent scrim
                        drawRect(
                            color = Color(0x66000000),
                            topLeft = Offset.Zero,
                            size = Size(w, top)
                        )
                        drawRect(
                            color = Color(0x66000000),
                            topLeft = Offset(0f, bottom),
                            size = Size(w, h - bottom)
                        )
                        drawRect(
                            color = Color(0x66000000),
                            topLeft = Offset(0f, top),
                            size = Size(left, frameH)
                        )
                        drawRect(
                            color = Color(0x66000000),
                            topLeft = Offset(right, top),
                            size = Size(w - right, frameH)
                        )

                        // Bounding Box
                        drawRect(
                            color = neonGreen.copy(alpha = 0.4f),
                            topLeft = Offset(left, top),
                            size = Size(frameW, frameH),
                            style = Stroke(width = 2f)
                        )

                        // 4 CamScanner Corner Brackets
                        val cornerLen = 32.dp.toPx()
                        val cornerThickness = 4.dp.toPx()

                        // Top-Left
                        drawLine(neonGreen, Offset(left, top), Offset(left + cornerLen, top), cornerThickness)
                        drawLine(neonGreen, Offset(left, top), Offset(left, top + cornerLen), cornerThickness)

                        // Top-Right
                        drawLine(neonGreen, Offset(right, top), Offset(right - cornerLen, top), cornerThickness)
                        drawLine(neonGreen, Offset(right, top), Offset(right, top + cornerLen), cornerThickness)

                        // Bottom-Left
                        drawLine(neonGreen, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerThickness)
                        drawLine(neonGreen, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerThickness)

                        // Bottom-Right
                        drawLine(neonGreen, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerThickness)
                        drawLine(neonGreen, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerThickness)

                        // Optional 3x3 Grid
                        if (isGridVisible) {
                            val col1 = left + frameW / 3f
                            val col2 = left + frameW * 2f / 3f
                            val row1 = top + frameH / 3f
                            val row2 = top + frameH * 2f / 3f

                            drawLine(neonGreen.copy(alpha = 0.2f), Offset(col1, top), Offset(col1, bottom), 1f)
                            drawLine(neonGreen.copy(alpha = 0.2f), Offset(col2, top), Offset(col2, bottom), 1f)
                            drawLine(neonGreen.copy(alpha = 0.2f), Offset(left, row1), Offset(right, row1), 1f)
                            drawLine(neonGreen.copy(alpha = 0.2f), Offset(left, row2), Offset(right, row2), 1f)
                        }
                    }

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color(0x88000000), CircleShape)
                                .size(42.dp)
                                .testTag("btn_close_camera")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        // Guidance text badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xAA000000)
                        ) {
                            Text(
                                text = selectedDocType.tip,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E676),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Grid Toggle
                            IconButton(
                                onClick = { isGridVisible = !isGridVisible },
                                modifier = Modifier
                                    .background(Color(0x88000000), CircleShape)
                                    .size(42.dp)
                            ) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = "Grid",
                                    tint = if (isGridVisible) Color(0xFF00E676) else Color.White
                                )
                            }

                            // Flash Toggle
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier
                                    .background(Color(0x88000000), CircleShape)
                                    .size(42.dp)
                                    .testTag("btn_toggle_flash")
                            ) {
                                Icon(
                                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flash",
                                    tint = if (isFlashOn) Color(0xFFFFD600) else Color.White
                                )
                            }
                        }
                    }

                    // Bottom Controls Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Document Mode Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ScanDocType.values().forEach { docType ->
                                val isSelected = selectedDocType == docType
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF00C853) else Color(0xFF222222),
                                    modifier = Modifier
                                        .clickable { selectedDocType = docType }
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = docType.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Shutter & Helper Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery Picker inside Camera
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onOpenGallery()
                                },
                                modifier = Modifier
                                    .background(Color(0xFF2E2E2E), CircleShape)
                                    .size(48.dp)
                                    .testTag("btn_cam_open_gallery")
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = "Gallery", tint = Color.White)
                            }

                            // Big Round CamScanner Shutter Capture Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(6.dp)
                                    .background(if (isCapturing) Color.Gray else Color(0xFF00C853), CircleShape)
                                    .clickable(enabled = !isCapturing) {
                                        val capture = imageCaptureInstance
                                        if (capture != null) {
                                            isCapturing = true
                                            val executor = ContextCompat.getMainExecutor(context)
                                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val bitmap = imageProxyToBitmap(image)
                                                    image.close()
                                                    isCapturing = false
                                                    if (bitmap != null) {
                                                        onImageCaptured(bitmap)
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    isCapturing = false
                                                    Toast.makeText(context, "Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                        } else {
                                            // Fallback to system camera
                                            val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                cacheFile
                                            )
                                            tempPhotoUri = uri
                                            systemCameraLauncher.launch(uri)
                                        }
                                    }
                                    .testTag("btn_capture_shutter"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            // System Camera Fallback Icon Button
                            IconButton(
                                onClick = {
                                    val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        cacheFile
                                    )
                                    tempPhotoUri = uri
                                    systemCameraLauncher.launch(uri)
                                },
                                modifier = Modifier
                                    .background(Color(0xFF2E2E2E), CircleShape)
                                    .size(48.dp)
                                    .testTag("btn_system_camera_fallback")
                            ) {
                                Icon(Icons.Default.Camera, contentDescription = "System Camera", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts ImageProxy from CameraX into a correctly oriented Bitmap
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
