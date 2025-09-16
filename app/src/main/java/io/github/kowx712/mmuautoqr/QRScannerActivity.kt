package io.github.kowx712.mmuautoqr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {
    private var toneGenerator: ToneGenerator? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private var multiFormatReader: MultiFormatReader? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera() else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
                finish()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)
        initializeScanSound()
        cameraExecutor = Executors.newSingleThreadExecutor()
        multiFormatReader = MultiFormatReader()

        setContent {
            AutoqrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ScannerScreen(
                        onReady = {
                            checkPermissionAndStart()
                        }
                    )
                }
            }
        }
    }

    private fun checkPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) startCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture?.addListener({
            val provider = cameraProviderFuture?.get() ?: return@addListener
            bindPreviewAndAnalysis(provider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewAndAnalysis(cameraProvider: ProcessCameraProvider) {
        val previewView = PreviewView(this)
        val previewUseCase = CameraPreview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        var isScanning = true

        analysisUseCase.setAnalyzer(cameraExecutor!!) { imageProxy: ImageProxy ->
            if (!isScanning) {
                imageProxy.close(); return@setAnalyzer
            }
            if (imageProxy.format != ImageFormat.YUV_420_888 && imageProxy.format != ImageFormat.YUV_422_888 && imageProxy.format != ImageFormat.YUV_444_888) {
                imageProxy.close(); return@setAnalyzer
            }

            val yPlane = imageProxy.planes[0]
            val yBuffer: ByteBuffer = yPlane.buffer
            val yData = ByteArray(yBuffer.remaining())
            yBuffer.get(yData)

            val source = PlanarYUVLuminanceSource(
                yData, yPlane.rowStride, imageProxy.height,
                0, 0, yPlane.rowStride, imageProxy.height, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result: Result? = multiFormatReader?.decode(bitmap)
                if (result != null && !result.text.isNullOrEmpty()) {
                    isScanning = false
                    val scannedUrl = result.text.trim()
                    runOnUiThread {
                        if (isValidUrl(scannedUrl)) {
                            playSuccessSound()
                            Toast.makeText(this, getString(R.string.qr_scanned_success), Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, WebViewActivity::class.java)
                            intent.putExtra("url", scannedUrl)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, getString(R.string.invalid_qr_code), Toast.LENGTH_LONG).show()
                            isScanning = true
                        }
                    }
                }
            } catch (e: NotFoundException) {
            } catch (e: Exception) {
            } finally {
                multiFormatReader?.reset()
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase, analysisUseCase)
        } catch (_: Exception) { }

        runOnUiThread {
            setContent {
                enableEdgeToEdge()
                AutoqrTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ScannerScreen(previewView = previewView, onReady = { }, onScale = { scale ->
                            val state = camera?.cameraInfo?.zoomState?.value ?: return@ScannerScreen
                            val newRatio = (state.zoomRatio * scale).coerceIn(state.minZoomRatio, state.maxZoomRatio)
                            camera?.cameraControl?.setZoomRatio(newRatio)
                        })
                    }
                }
            }
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val lower = url.lowercase()
        val starts = lower.startsWith("http://") || lower.startsWith("https://")
        val contains = lower.contains("clic") || lower.contains("osc.mmu.edu.my")
        val debugging = url.contains("192.168")
        return starts && contains || debugging
    }

    private fun initializeScanSound() {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            null
        }
    }

    private fun playSuccessSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        cameraExecutor?.shutdown()
        multiFormatReader?.reset()
    }
}

@Preview(showBackground = false)
@Composable
private fun ScannerScreenPreview() {
    ScannerScreen(previewView = null, onReady = {}, onScale = {})
}

@Composable
private fun ScannerScreen(previewView: PreviewView? = null, onReady: () -> Unit, onScale: (Float) -> Unit = {}) {
    LaunchedEffect(Unit) { onReady() }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
                if (zoom != 1f) onScale(zoom)
            }
        }
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx -> previewView ?: PreviewView(ctx) },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xC0000000))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xC0000000), Color.Transparent),
                                startY = 0f,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.qr_scanner_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.qr_scanner_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        // Scanning frame implemented with Jetpack Compose
        ScanningFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .alpha(0.5f)
        )

        // Bottom spacer overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xC0000000))
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x80000000)),
                            startY = 0f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ScanningFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cornerRadius = 20.dp.toPx()
        val thickness = 2.dp.toPx()
        val lineLength = 30.dp.toPx()
        val halfThickness = thickness / 2

        // Top Left
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(halfThickness, halfThickness),
            size = Size(cornerRadius * 2 - thickness, cornerRadius * 2 - thickness),
            style = Stroke(width = thickness)
        )
        drawLine(
            color = Color.White,
            start = Offset(cornerRadius, halfThickness),
            end = Offset(cornerRadius + lineLength, halfThickness),
            strokeWidth = thickness
        )
        drawLine(
            color = Color.White,
            start = Offset(halfThickness, cornerRadius),
            end = Offset(halfThickness, cornerRadius + lineLength),
            strokeWidth = thickness
        )

        // Top Right
        val trArcTopLeft = Offset(size.width - cornerRadius * 2 + halfThickness, halfThickness)
        drawArc(
            color = Color.White,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = trArcTopLeft,
            size = Size(cornerRadius * 2 - thickness, cornerRadius * 2 - thickness),
            style = Stroke(width = thickness)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width - cornerRadius, halfThickness),
            end = Offset(size.width - cornerRadius - lineLength, halfThickness),
            strokeWidth = thickness
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width - halfThickness, cornerRadius),
            end = Offset(size.width - halfThickness, cornerRadius + lineLength),
            strokeWidth = thickness
        )

        // Bottom Left
        val blArcTopLeft = Offset(halfThickness, size.height - cornerRadius * 2 + halfThickness)
        drawArc(
            color = Color.White,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = blArcTopLeft,
            size = Size(cornerRadius * 2 - thickness, cornerRadius * 2 - thickness),
            style = Stroke(width = thickness)
        )
        drawLine(
            color = Color.White,
            start = Offset(cornerRadius, size.height - halfThickness),
            end = Offset(cornerRadius + lineLength, size.height - halfThickness),
            strokeWidth = thickness
        )
        drawLine(
            color = Color.White,
            start = Offset(halfThickness, size.height - cornerRadius),
            end = Offset(halfThickness, size.height - cornerRadius - lineLength),
            strokeWidth = thickness
        )

        // Bottom Right
        val brArcTopLeft = Offset(size.width - cornerRadius * 2 + halfThickness, size.height - cornerRadius * 2 + halfThickness)
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = brArcTopLeft,
            size = Size(cornerRadius * 2 - thickness, cornerRadius * 2 - thickness),
            style = Stroke(width = thickness)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width - cornerRadius, size.height - halfThickness),
            end = Offset(size.width - cornerRadius - lineLength, size.height - halfThickness),
            strokeWidth = thickness
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width - halfThickness, size.height - cornerRadius),
            end = Offset(size.width - halfThickness, size.height - cornerRadius - lineLength),
            strokeWidth = thickness
        )
    }
}
