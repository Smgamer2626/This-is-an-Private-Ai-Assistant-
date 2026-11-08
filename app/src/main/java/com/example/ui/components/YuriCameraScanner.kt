package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.theme.YuriBorderCyan
import com.example.ui.theme.YuriCyanGlow
import com.example.ui.theme.YuriCyanPrimary
import com.example.ui.theme.YuriStatusListening
import com.example.ui.theme.YuriStatusOnline
import java.util.concurrent.Executors

@Composable
fun YuriCameraScanner(
    onDismiss: () -> Unit,
    onAnalyzeImage: (Bitmap, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission required for optical analysis", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }

    // HUD laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Futuristic JARVIS Optical HUD Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Targeting box in center
            val boxW = w * 0.75f
            val boxH = h * 0.45f
            val left = (w - boxW) / 2f
            val top = (h - boxH) / 2.3f
            val right = left + boxW
            val bottom = top + boxH

            val cornerLen = 32.dp.toPx()
            val cyan = Color(0xFF00E5FF)
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

            // Top-left
            drawLine(cyan, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke.width)
            drawLine(cyan, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = stroke.width)

            // Top-right
            drawLine(cyan, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = stroke.width)
            drawLine(cyan, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = stroke.width)

            // Bottom-left
            drawLine(cyan, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = stroke.width)
            drawLine(cyan, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = stroke.width)

            // Bottom-right
            drawLine(cyan, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = stroke.width)
            drawLine(cyan, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = stroke.width)

            // Moving horizontal cyan laser line
            val laserY = top + (boxH * laserProgress)
            drawLine(
                color = cyan.copy(alpha = 0.85f),
                start = Offset(left, laserY),
                end = Offset(right, laserY),
                strokeWidth = 2.dp.toPx()
            )

            // Center targeting reticle
            val cx = w / 2f
            val cy = top + boxH / 2f
            drawCircle(cyan.copy(alpha = 0.4f), radius = 14.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            drawLine(cyan.copy(alpha = 0.5f), Offset(cx - 8.dp.toPx(), cy), Offset(cx + 8.dp.toPx(), cy), strokeWidth = 1.dp.toPx())
            drawLine(cyan.copy(alpha = 0.5f), Offset(cx, cy - 8.dp.toPx()), Offset(cx, cy + 8.dp.toPx()), strokeWidth = 1.dp.toPx())
        }

        // Top HUD Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPTICAL SENSOR // ONLINE",
                    color = YuriCyanPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "TARGET: AUTO ACQUISITION",
                    color = YuriCyanGlow,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, YuriBorderCyan, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = YuriCyanPrimary
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Frame subject and tap Analyze to feed Qwen vision model",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset: Schematic / Circuit
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, YuriBorderCyan, RoundedCornerShape(12.dp))
                        .clickable {
                            captureAndSend(
                                imageCapture = imageCapture,
                                cameraExecutor = cameraExecutor,
                                prompt = "Analyze this schematic or circuit diagram and explain the components.",
                                onStart = { isCapturing = true },
                                onSuccess = { bmp, p ->
                                    isCapturing = false
                                    onAnalyzeImage(bmp, p)
                                },
                                onError = {
                                    isCapturing = false
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("CIRCUIT / CODE", color = YuriCyanGlow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                // Primary Trigger Button
                IconButton(
                    onClick = {
                        if (!isCapturing) {
                            captureAndSend(
                                imageCapture = imageCapture,
                                cameraExecutor = cameraExecutor,
                                prompt = "Describe what you see in this frame in detail and provide technical analysis.",
                                onStart = { isCapturing = true },
                                onSuccess = { bmp, p ->
                                    isCapturing = false
                                    onAnalyzeImage(bmp, p)
                                },
                                onError = {
                                    isCapturing = false
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(YuriCyanPrimary)
                        .border(3.dp, Color.White, CircleShape)
                        .testTag("camera_analyze_trigger")
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Analyze Target",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Preset: General Object
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, YuriBorderCyan, RoundedCornerShape(12.dp))
                        .clickable {
                            captureAndSend(
                                imageCapture = imageCapture,
                                cameraExecutor = cameraExecutor,
                                prompt = "Identify what object is shown in this camera frame and what it does.",
                                onStart = { isCapturing = true },
                                onSuccess = { bmp, p ->
                                    isCapturing = false
                                    onAnalyzeImage(bmp, p)
                                },
                                onError = {
                                    isCapturing = false
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("IDENTIFY OBJECT", color = YuriCyanGlow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun captureAndSend(
    imageCapture: ImageCapture,
    cameraExecutor: java.util.concurrent.Executor,
    prompt: String,
    onStart: () -> Unit,
    onSuccess: (Bitmap, String) -> Unit,
    onError: (String) -> Unit
) {
    onStart()
    imageCapture.takePicture(
        cameraExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = image.toBitmap()
                    image.close()
                    // Dispatch to main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSuccess(bitmap, prompt)
                    }
                } catch (e: Exception) {
                    image.close()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("Capture error: ${e.localizedMessage}")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Camera capture failed: ${exception.message}")
                }
            }
        }
    )
}
