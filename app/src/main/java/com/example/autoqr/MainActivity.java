package com.example.autoqr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.autoqr.utils.UserManager;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private Button btnScanQR, btnManageUsers;
    private TextView tvUserCount, tvActiveUsers;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeUserManager();
        setupClickListeners();
        checkCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserStats();
    }

    private void initializeViews() {
        btnScanQR = findViewById(R.id.btnScanQR);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
    }

    private void initializeUserManager() {
        userManager = new UserManager(this);
    }

    private void setupClickListeners() {
        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userManager.getActiveUserCount() == 0) {
                    Toast.makeText(MainActivity.this,
                            "Please add at least one active user first!",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(MainActivity.this, UserManagementActivity.class));
                } else {
                    startQRScanner();
                }
            }
        });

        btnManageUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UserManagementActivity.class));
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, QRScannerActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR code",
                    Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUserStats() {
        int totalUsers = userManager.getUserCount();
        int activeUsers = userManager.getActiveUserCount();

        tvUserCount.setText("Total Users: " + totalUsers);
        tvActiveUsers.setText("Active Users: " + activeUsers);

        btnScanQR.setEnabled(activeUsers > 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission is required for QR scanning",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}