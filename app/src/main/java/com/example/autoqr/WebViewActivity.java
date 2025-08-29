package com.example.autoqr;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autoqr.models.User;
import com.example.autoqr.utils.UserManager;

import java.util.List;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private UserManager userManager;
    private List<User> activeUsers;
    private int currentUserIndex = 0;
    private String attendanceUrl;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        initializeViews();
        setupWebView();
        loadAttendanceData();
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAutoLogin();
                    }
                }, 2000);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadAttendanceData() {
        attendanceUrl = getIntent().getStringExtra("url");

        userManager = new UserManager(this);
        activeUsers = userManager.getActiveUsers();

        if (activeUsers.isEmpty()) {
            Toast.makeText(this, "No active users found!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        updateStatus("Loading attendance page...");
        webView.loadUrl(attendanceUrl);
    }

    private void startAutoLogin() {
        if (currentUserIndex < activeUsers.size()) {
            User currentUser = activeUsers.get(currentUserIndex);
            updateStatus("Signing in user: " + currentUser.getName() + " (" + (currentUserIndex + 1) + "/" + activeUsers.size() + ")");

            String javascript = "javascript:" +
                    "function fillAndSubmit(user, pass) {" +
                    "  var userField = document.querySelector('input[type=\"text\"], input[name*=\"user\"], input[id*=\"user\"], input[placeholder*=\"User\"]');" +
                    "  var passField = document.querySelector('input[type=\"password\"]');" +
                    "  var submitBtn = document.querySelector('input[type=\"submit\"], button[type=\"submit\"], input[value*=\"Sign\"], button');" +
                    "  " +
                    "  if (userField && passField) {" +
                    "    userField.value = '';" +
                    "    passField.value = '';" +
                    "    setTimeout(function() {" +
                    "      userField.value = user;" +
                    "      passField.value = pass;" +
                    "      if (submitBtn) {" +
                    "        submitBtn.click();" +
                    "        Android.onLoginSubmitted();" +
                    "      } else {" +
                    "        Android.onLoginFailed('Submit button not found');" +
                    "      }" +
                    "    }, 500);" +
                    "  } else {" +
                    "    Android.onLoginFailed('Login fields not found');" +
                    "  }" +
                    "}" +
                    "fillAndSubmit('" + currentUser.getUserId() + "', '" + currentUser.getPassword() + "');";

            webView.evaluateJavascript(javascript, null);
        } else {
            updateStatus("All users signed in successfully!");
            Toast.makeText(this, "Attendance completed for all users!", Toast.LENGTH_LONG).show();

            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
        }
    }
    private void proceedToNextUser() {
        currentUserIndex++;

        if (currentUserIndex < activeUsers.size()) {
            updateStatus("Signing in next user...");

            User nextUser = activeUsers.get(currentUserIndex);

            String javascript = "javascript:" +
                    "fillAndSubmit('" + nextUser.getUserId() + "', '" + nextUser.getPassword() + "');";

            webView.evaluateJavascript(javascript, null);

        } else {
            updateStatus("All users completed!");
            Toast.makeText(this, "Attendance automation completed!", Toast.LENGTH_LONG).show();

            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText(status);
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onLoginSubmitted() {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    User currentUser = activeUsers.get(currentUserIndex);
                    updateStatus("Login submitted for " + currentUser.getName() + ". Processing...");

                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            proceedToNextUser();
                        }
                    }, 3000);
                }
            }, 100);
        }

        @JavascriptInterface
        public void onLoginFailed(String reason) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(WebViewActivity.this,
                            "Login failed: " + reason,
                            Toast.LENGTH_SHORT).show();
                    updateStatus("Login failed for current user. Moving to next...");

                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            proceedToNextUser();
                        }
                    }, 2000);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}