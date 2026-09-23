package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ImageUtils
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * CamScanner 4-Corner Perspective Angle & Auto-Crop Dialog
 * Lets user auto-detect or manually adjust 4 corners of documents, CNIC cards, or paper notes,
 * corrects perspective tilt, and on OK (✔) auto-triggers OCR scanning.
 */
@Composable
fun DocumentAngleCropDialog(
    originalBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var workingBitmap by remember { mutableStateOf(originalBitmap) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    // Normalized coordinates (0.0f to 1.0f) for 4 corners detected automatically:
    // [0]=TL, [1]=TR, [2]=BR, [3]=BL
    val initialCorners = remember(workingBitmap) {
        ImageUtils.detectDocumentNormalizedCorners(workingBitmap)
    }

    var cornerTL by remember(workingBitmap) { mutableStateOf(Offset(initialCorners[0], initialCorners[1])) }
    var cornerTR by remember(workingBitmap) { mutableStateOf(Offset(initialCorners[2], initialCorners[3])) }
    var cornerBR by remember(workingBitmap) { mutableStateOf(Offset(initialCorners[4], initialCorners[5])) }
    var cornerBL by remember(workingBitmap) { mutableStateOf(Offset(initialCorners[6], initialCorners[7])) }

    var selectedMode by remember { mutableStateOf("AUTO") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("document_angle_crop_dialog"),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_crop")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Auto Angle & Crop",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "A4 / CNIC / Document Detect",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    IconButton(
                        onClick = {
                            workingBitmap = ImageUtils.rotateBitmap(workingBitmap, 90f)
                        },
                        modifier = Modifier.testTag("btn_rotate_crop")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90°", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Crop Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                        .onGloballyPositioned { coordinates ->
                            canvasSize = coordinates.size
                        }
                        .pointerInput(canvasSize) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                if (w <= 0 || h <= 0) return@detectDragGestures

                                val dragNormX = dragAmount.x / w
                                val dragNormY = dragAmount.y / h
                                val touchNorm = Offset(change.position.x / w, change.position.y / h)

                                // Find closest corner to touch position
                                val distTL = hypot(touchNorm.x - cornerTL.x, touchNorm.y - cornerTL.y)
                                val distTR = hypot(touchNorm.x - cornerTR.x, touchNorm.y - cornerTR.y)
                                val distBR = hypot(touchNorm.x - cornerBR.x, touchNorm.y - cornerBR.y)
                                val distBL = hypot(touchNorm.x - cornerBL.x, touchNorm.y - cornerBL.y)

                                val minDist = minOf(distTL, distTR, distBR, distBL)
                                when (minDist) {
                                    distTL -> cornerTL = clampNorm(cornerTL + Offset(dragNormX, dragNormY))
                                    distTR -> cornerTR = clampNorm(cornerTR + Offset(dragNormX, dragNormY))
                                    distBR -> cornerBR = clampNorm(cornerBR + Offset(dragNormX, dragNormY))
                                    distBL -> cornerBL = clampNorm(cornerBL + Offset(dragNormX, dragNormY))
                                }
                            }
                        }
                        .testTag("interactive_crop_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cW = size.width
                        val cH = size.height
                        val bW = workingBitmap.width.toFloat()
                        val bH = workingBitmap.height.toFloat()

                        // Calculate aspect ratio fit of the bitmap
                        val scale = min(cW / bW, cH / bH)
                        val drawW = bW * scale
                        val drawH = bH * scale
                        val offsetX = (cW - drawW) / 2f
                        val offsetY = (cH - drawH) / 2f

                        // Draw background bitmap
                        drawImage(
                            image = workingBitmap.asImageBitmap(),
                            dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                            dstSize = IntSize(drawW.toInt(), drawH.toInt())
                        )

                        // Convert normalized corners to canvas pixel coords inside the image bounds
                        val pTL = Offset(offsetX + cornerTL.x * drawW, offsetY + cornerTL.y * drawH)
                        val pTR = Offset(offsetX + cornerTR.x * drawW, offsetY + cornerTR.y * drawH)
                        val pBR = Offset(offsetX + cornerBR.x * drawW, offsetY + cornerBR.y * drawH)
                        val pBL = Offset(offsetX + cornerBL.x * drawW, offsetY + cornerBL.y * drawH)

                        // Draw translucent dimming outside quadrilateral
                        val fullPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(cW, 0f)
                            lineTo(cW, cH)
                            lineTo(0f, cH)
                            close()
                        }
                        val quadPath = Path().apply {
                            moveTo(pTL.x, pTL.y)
                            lineTo(pTR.x, pTR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBL.x, pBL.y)
                            close()
                        }
                        drawPath(fullPath, color = Color(0x66000000))
                        drawPath(quadPath, color = Color(0x1A00FF66)) // Green tint inside quad

                        // Draw CamScanner Green Neon Border Lines
                        val neonGreen = Color(0xFF00E676)
                        drawPath(
                            quadPath,
                            color = neonGreen,
                            style = Stroke(width = 4f)
                        )

                        // Draw Grid lines inside quad (Rule of thirds)
                        val pT1 = lerp(pTL, pTR, 0.33f)
                        val pT2 = lerp(pTL, pTR, 0.66f)
                        val pB1 = lerp(pBL, pBR, 0.33f)
                        val pB2 = lerp(pBL, pBR, 0.66f)
                        drawLine(color = neonGreen.copy(alpha = 0.35f), start = pT1, end = pB1, strokeWidth = 2f)
                        drawLine(color = neonGreen.copy(alpha = 0.35f), start = pT2, end = pB2, strokeWidth = 2f)

                        val pL1 = lerp(pTL, pBL, 0.33f)
                        val pL2 = lerp(pTL, pBL, 0.66f)
                        val pR1 = lerp(pTR, pBR, 0.33f)
                        val pR2 = lerp(pTR, pBR, 0.66f)
                        drawLine(color = neonGreen.copy(alpha = 0.35f), start = pL1, end = pR1, strokeWidth = 2f)
                        drawLine(color = neonGreen.copy(alpha = 0.35f), start = pL2, end = pR2, strokeWidth = 2f)

                        // Draw 4 Large Touch Corner Handles
                        listOf(pTL, pTR, pBR, pBL).forEach { point ->
                            drawCircle(color = Color.White, radius = 24f, center = point)
                            drawCircle(color = neonGreen, radius = 18f, center = point)
                            drawCircle(color = Color.White, radius = 6f, center = point)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Aspect Ratio & Auto Presets Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PresetChip(
                        label = "A4 Doc",
                        icon = Icons.Default.Description,
                        isSelected = selectedMode == "A4",
                        onClick = {
                            selectedMode = "A4"
                            // A4 portrait ratio 1 : 1.414 centered
                            cornerTL = Offset(0.08f, 0.05f)
                            cornerTR = Offset(0.92f, 0.05f)
                            cornerBR = Offset(0.92f, 0.95f)
                            cornerBL = Offset(0.08f, 0.95f)
                        }
                    )

                    PresetChip(
                        label = "ID / CNIC",
                        icon = Icons.Default.CreditCard,
                        isSelected = selectedMode == "CNIC",
                        onClick = {
                            selectedMode = "CNIC"
                            // Card ratio approx 1.58 : 1 (horizontal card)
                            cornerTL = Offset(0.08f, 0.25f)
                            cornerTR = Offset(0.92f, 0.25f)
                            cornerBR = Offset(0.92f, 0.75f)
                            cornerBL = Offset(0.08f, 0.75f)
                        }
                    )

                    PresetChip(
                        label = "Auto Detect",
                        icon = Icons.Default.Crop,
                        isSelected = selectedMode == "AUTO",
                        onClick = {
                            selectedMode = "AUTO"
                            val detected = ImageUtils.detectDocumentNormalizedCorners(workingBitmap)
                            cornerTL = Offset(detected[0], detected[1])
                            cornerTR = Offset(detected[2], detected[3])
                            cornerBR = Offset(detected[4], detected[5])
                            cornerBL = Offset(detected[6], detected[7])
                        }
                    )

                    PresetChip(
                        label = "Full Image",
                        icon = Icons.Default.Fullscreen,
                        isSelected = selectedMode == "FULL",
                        onClick = {
                            selectedMode = "FULL"
                            cornerTL = Offset(0f, 0f)
                            cornerTR = Offset(1f, 0f)
                            cornerBR = Offset(1f, 1f)
                            cornerBL = Offset(0f, 1f)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Green OK / Tick (✔) "Scan Document" Button
                Button(
                    onClick = {
                        val bW = workingBitmap.width.toFloat()
                        val bH = workingBitmap.height.toFloat()

                        val srcPoints = floatArrayOf(
                            cornerTL.x * bW, cornerTL.y * bH,
                            cornerTR.x * bW, cornerTR.y * bH,
                            cornerBR.x * bW, cornerBR.y * bH,
                            cornerBL.x * bW, cornerBL.y * bH
                        )

                        // Compute perspective corrected bitmap
                        val croppedBmp = ImageUtils.cropPerspective(workingBitmap, srcPoints)
                        onCropConfirmed(croppedBmp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_crop_scan"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853), // CamScanner Vibrant Green
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "OK Scan",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OK - Auto Scan Document (✔)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.25f) else Color(0xFF242424),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00E676) else Color.DarkGray
        ),
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures { _, _ -> }
        }
    ) {
        Row(
            modifier = Modifier
                .pointerInput(Unit) { }
                .background(Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSelected) Color(0xFF00E676) else Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF00E676) else Color.White
                )
            }
        }
    }
}

private fun clampNorm(offset: Offset): Offset {
    return Offset(
        x = offset.x.coerceIn(0f, 1f),
        y = offset.y.coerceIn(0f, 1f)
    )
}

private fun lerp(start: Offset, end: Offset, fraction: Float): Offset {
    return Offset(
        start.x + (end.x - start.x) * fraction,
        start.y + (end.y - start.y) * fraction
    )
}
