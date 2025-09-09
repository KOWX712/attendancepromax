package com.example.autoqr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";
    private CompoundBarcodeView barcodeView;
    private boolean isScanning = true;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeView = findViewById(R.id.barcode_scanner);
        View scanningFrame = findViewById(R.id.scanning_frame);

        initializeScanSound();

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        scanningFrame.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
        });

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isScanning && result.getText() != null && !result.getText().isEmpty()) {
                    isScanning = false;

                    String scannedUrl = result.getText().trim();

                    if (isValidUrl(scannedUrl)) {
                        playSuccessSound();

                        Toast.makeText(QRScannerActivity.this,
                                "QR Code scanned successfully!",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(QRScannerActivity.this, WebViewActivity.class);
                        intent.putExtra("url", scannedUrl);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(QRScannerActivity.this,
                                "Invalid QR code. Please scan a valid attendance URL.",
                                Toast.LENGTH_LONG).show();
                        isScanning = true;
                    }
                }
            }
        });

        CameraSettings settings = new CameraSettings();
        settings.setAutoFocusEnabled(true);
        barcodeView.getBarcodeView().setCameraSettings(settings);

        barcodeView.post(new Runnable() {
            @Override
            public void run() {
                barcodeView.setPivotX(barcodeView.getWidth() / 2f);
                barcodeView.setPivotY(barcodeView.getHeight() / 2f);
            }
        });

        final OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                try {
                    if (!isFinishing()) {
                       QRScannerActivity.this.getOnBackPressedDispatcher().onBackPressed();
                    }
                } finally {
                    if (!isFinishing()) {
                       setEnabled(true);
                    }
                }
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
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
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));

            try {
                barcodeView.setScaleX(scaleFactor);
                barcodeView.setScaleY(scaleFactor);

                // Keep the barcode view centered
                barcodeView.setPivotX(barcodeView.getWidth() / 2f);
                barcodeView.setPivotY(barcodeView.getHeight() / 2f);
            } catch (Exception e) {
                Log.e(TAG, "Error scaling barcode view", e);
            }

            return true;
        }
    }

    private boolean isValidUrl(String url) {
        if (url != null &&
                (url.toLowerCase().startsWith("http://") ||
                        url.toLowerCase().startsWith("https://")) &&
                url.contains("clic")) return true;
        assert url != null;
        return url.contains("osc.mmu.edu.my");
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
        isScanning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
