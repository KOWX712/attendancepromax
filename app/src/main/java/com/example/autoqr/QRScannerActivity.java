package com.example.autoqr;

import android.content.Intent;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

public class QRScannerActivity extends AppCompatActivity {

    private CompoundBarcodeView barcodeView;
    private boolean isScanning = true;
    private ScaleGestureDetector scaleGestureDetector;
    private View scanningFrame;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeView = findViewById(R.id.barcode_scanner);
        scanningFrame = findViewById(R.id.scanning_frame);

        initializeScanSound();

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        scanningFrame.setOnTouchListener(new View.OnTouchListener() {
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
    }

    private void initializeScanSound() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        } catch (Exception e) {
            e.printStackTrace();
            toneGenerator = null;
        }
    }

    private void playSuccessSound() {
        try {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                e.printStackTrace();
            }

            return true;
        }
    }

    private boolean isValidUrl(String url) {
        return url != null &&
                (url.toLowerCase().startsWith("http://") ||
                        url.toLowerCase().startsWith("https://")) &&
                url.contains("clic") ||
                url.contains("osc.mmu.edu.my");
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}