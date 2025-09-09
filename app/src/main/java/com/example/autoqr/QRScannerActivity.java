package com.example.autoqr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";
    private PreviewView previewView;
    private boolean isScanning = true;
    private ScaleGestureDetector scaleGestureDetector;
    private ToneGenerator toneGenerator;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private MultiFormatReader multiFormatReader;

    // Variables for lens switching
    private ProcessCameraProvider processCameraProvider;
    private final List<CameraInfo> backCameraInfos = new ArrayList<>();
    private int currentCameraIndex = 0;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        previewView = findViewById(R.id.preview_view);

        initializeScanSound();
        cameraExecutor = Executors.newSingleThreadExecutor();
        multiFormatReader = new MultiFormatReader();

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        final OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                this.processCameraProvider = cameraProviderFuture.get();
                enumerateBackCameras(this.processCameraProvider);
                bindPreviewAndAnalysis(this.processCameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
                Toast.makeText(this, "Error starting camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void enumerateBackCameras(ProcessCameraProvider cameraProvider) {
        backCameraInfos.clear();
        if (cameraProvider == null) return;

        try {
            for (CameraInfo cameraInfo : cameraProvider.getAvailableCameraInfos()) {
                int lensFacing = cameraInfo.getLensFacing();
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    backCameraInfos.add(cameraInfo);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enumerating cameras: " + e.getMessage(), e);
        }
        currentCameraIndex = 0;
    }

    private void switchNextCamera() {
        if (backCameraInfos.size() > 1 && processCameraProvider != null) {
            currentCameraIndex = (currentCameraIndex + 1) % backCameraInfos.size();
            bindPreviewAndAnalysis(processCameraProvider);
        }
    }

    @SuppressLint({"UnsafeOptInUsageError", "RestrictedApi"})
    private void bindPreviewAndAnalysis(ProcessCameraProvider cameraProvider) {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider not available for binding.");
            return;
        }

        // CameraX Use Cases - ensure these are class members
        Preview previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysisUseCase = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisUseCase.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (!isScanning) {
                    imageProxy.close();
                    return;
                }
                 if (imageProxy.getFormat() != ImageFormat.YUV_420_888 && imageProxy.getFormat() != ImageFormat.YUV_422_888 && imageProxy.getFormat() != ImageFormat.YUV_444_888) {
                    imageProxy.close();
                    return;
                }

                ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
                ByteBuffer yBuffer = yPlane.getBuffer();
                byte[] yData = new byte[yBuffer.remaining()];
                yBuffer.get(yData);

                LuminanceSource source = new PlanarYUVLuminanceSource(
                        yData, yPlane.getRowStride(), imageProxy.getHeight(),
                        0, 0, yPlane.getRowStride(), imageProxy.getHeight(), false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    Result result = multiFormatReader.decode(bitmap);
                    if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                        isScanning = false;
                        final String scannedUrl = result.getText().trim();
                        runOnUiThread(() -> {
                            if (isValidUrl(scannedUrl)) {
                                playSuccessSound();
                                Toast.makeText(QRScannerActivity.this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(QRScannerActivity.this, WebViewActivity.class);
                                intent.putExtra("url", scannedUrl);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(QRScannerActivity.this, "Invalid QR code. Please scan a valid attendance URL.", Toast.LENGTH_LONG).show();
                                isScanning = true;
                            }
                        });
                    }
                } catch (NotFoundException e) {
                    // Not found
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding QR code", e);
                } finally {
                    multiFormatReader.reset();
                }
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector;
        if (!backCameraInfos.isEmpty() && currentCameraIndex < backCameraInfos.size()) {
            cameraSelector = backCameraInfos.get(currentCameraIndex).getCameraSelector();
        } else {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
            Log.w(TAG, "Using default back camera selector.");
        }

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase, analysisUseCase); // Use class members
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed: " + e.getMessage(), e);
            Toast.makeText(this, "Could not bind camera use cases.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeScanSound() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ToneGenerator", e);
            toneGenerator = null;
        }
    }

    private void playSuccessSound() {
        try {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing success sound", e);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (camera == null || camera.getCameraInfo().getZoomState().getValue() == null) {
                return true;
            }

            float currentZoomRatio = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
            float minZoomRatio = camera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
            float maxZoomRatio = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
            float scaleFactor = detector.getScaleFactor();
            float newZoomRatio = currentZoomRatio * scaleFactor;

            float finalZoomRatio = Math.max(minZoomRatio, Math.min(newZoomRatio, maxZoomRatio));

            if (Math.abs(finalZoomRatio - currentZoomRatio) > 0.001f) {
                camera.getCameraControl().setZoomRatio(finalZoomRatio);
            }
            return true;
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null) return false;
        String lowerCaseUrl = url.toLowerCase();
        boolean startsWithHttpOrHttps = lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://");
        boolean containsClic = lowerCaseUrl.contains("clic");
        boolean containsOscMmu = lowerCaseUrl.contains("osc.mmu.edu.my");
        return startsWithHttpOrHttps && (containsClic || containsOscMmu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanning = true;
        if (processCameraProvider != null && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            enumerateBackCameras(processCameraProvider);
            bindPreviewAndAnalysis(processCameraProvider);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (multiFormatReader != null) {
            multiFormatReader.reset();
        }
    }
}
