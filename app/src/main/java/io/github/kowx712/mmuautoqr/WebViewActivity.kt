package io.github.kowx712.mmuautoqr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import io.github.kowx712.mmuautoqr.models.User
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.UserManager

class WebViewActivity : ComponentActivity() {
    private lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainHandler = Handler(Looper.getMainLooper())

        setContent {
            AutoqrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val dark = isSystemInDarkTheme()
                    SideEffect {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = !dark
                        controller.isAppearanceLightNavigationBars = !dark
                    }
                    WebViewScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }

    @Composable
    private fun WebViewScreen() {
        val userManager = remember { UserManager(this@WebViewActivity) }
        var activeUsers by remember { mutableStateOf<List<User>>(emptyList()) }
        var isLoadingUsers by remember { mutableStateOf(true) }

        LaunchedEffect(key1 = userManager) {
            isLoadingUsers = true
            activeUsers = userManager.getUsers().filter { it.isActive }
            isLoadingUsers = false
        }

        var currentUserIndex by remember { mutableIntStateOf(0) }
        var statusText by remember { mutableStateOf(getString(R.string.loading_attendance_page)) }
        var isLoadingPage by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        var hasRetriedBlank by remember { mutableStateOf(false) }
        val attendanceUrl = remember {
            if (intent.action == Intent.ACTION_VIEW) {
                intent.dataString ?: ""
            } else {
                intent.getStringExtra("url") ?: ""
            }
        }
        var initialLoginCanProceed by remember { mutableStateOf(false) }

        if (isLoadingUsers) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text(getString(R.string.loading), modifier = Modifier.padding(top = 60.dp))
            }
            return
        }

        if (activeUsers.isEmpty()) {
            LaunchedEffect(Unit) {
                Toast.makeText(this@WebViewActivity, "No active users found!", Toast.LENGTH_LONG).show()
                finish()
            }
            return
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AttendanceWebView(
                    url = attendanceUrl,
                    onPageFinished = {
                        isLoadingPage = false
                        mainHandler.postDelayed({
                            initialLoginCanProceed = true
                        }, 2000)
                    },
                    onProvideWebView = { webView ->
                        webView.addJavascriptInterface(object : Any() {
                            @JavascriptInterface
                            fun onLoginSubmitted() {
                                val submittedUserIndex = currentUserIndex
                                if (submittedUserIndex < activeUsers.size) {
                                    val currentUser = activeUsers[submittedUserIndex]
                                    mainHandler.postDelayed({
                                        statusText = getString(R.string.login_submitted, currentUser.name)
                                        mainHandler.postDelayed({
                                            proceedToNextUser(activeUsers, submittedUserIndex) { nextIndex ->
                                                currentUserIndex = nextIndex
                                                initialLoginCanProceed = true
                                            }
                                        }, 3000)
                                    }, 100)
                                }
                            }

                            @JavascriptInterface
                            fun onLoginFailed(reason: String) {
                                val failedUserIndex = currentUserIndex
                                if (failedUserIndex < activeUsers.size) {
                                    val currentUser = activeUsers[failedUserIndex]
                                    mainHandler.post {
                                        Toast.makeText(this@WebViewActivity, getString(R.string.login_failed, reason), Toast.LENGTH_SHORT).show()
                                        statusText = getString(R.string.login_failed, currentUser.name)
                                        mainHandler.postDelayed({
                                            proceedToNextUser(activeUsers, failedUserIndex) { nextIndex ->
                                                currentUserIndex = nextIndex
                                                initialLoginCanProceed = true
                                            }
                                        }, 1000)
                                    }
                                }
                            }
                        }, "Android")
                    },
                    onEvaluateLogin = { webView ->
                        if (currentUserIndex < activeUsers.size) {
                            val user = activeUsers[currentUserIndex]
                            initialLoginCanProceed = false
                            val escUser = user.userId.replace("'", "\\'")
                            val escPass = user.password.replace("'", "\\'")
                            val js = """
                                javascript:
                                function fillAndSubmit(user, pass, retryCount) {
                                  if (retryCount > 80) {
                                    Android.onLoginFailed('Login fields not found after retries');
                                    return;
                                  }
                                  var userField = document.querySelector('input[type="text"], input[name*="user"], input[id*="user"], input[placeholder*="User"]');
                                  var passField = document.querySelector('input[type="password"]');
                                  var submitBtn = document.querySelector('input[type="submit"], button[type="submit"], input[value*="Sign"], button');

                                  if (userField && passField) {
                                    userField.value = '';
                                    passField.value = '';
                                    setTimeout(function() {
                                      userField.value = user;
                                      passField.value = pass;
                                      if (submitBtn) {
                                        submitBtn.click();
                                        Android.onLoginSubmitted();
                                      } else {
                                        Android.onLoginFailed('Submit button not found');
                                      }
                                    }, 200);
                                  } else {
                                    setTimeout(function() { fillAndSubmit(user, pass, retryCount + 1); }, 100);
                                  }
                                }
                                fillAndSubmit('$escUser', '$escPass', 0);
                            """.trimIndent()
                            webView.evaluateJavascript(js, null)
                        }
                    },
                    onError = { message ->
                        if (message == "Page loaded but appears to be blank" && !hasRetriedBlank) {
                            hasRetriedBlank = true
                            isLoadingPage = true
                            webViewRef?.reload()
                        } else {
                            isError = true
                            errorMessage = message
                            isLoadingPage = false
                        }
                    },
                    onWebViewInstance = { webViewRef = it },
                    canTriggerLogin = initialLoginCanProceed
                )

                if (isLoadingPage) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                if (isError) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isError = false
                            hasRetriedBlank = false
                            isLoadingPage = true
                            webViewRef?.loadUrl(attendanceUrl)
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }

    private fun proceedToNextUser(activeUsersList: List<User>, current: Int, onIndex: (Int) -> Unit) {
        val next = current + 1
        if (next < activeUsersList.size) {
            onIndex(next)
        } else {
            Toast.makeText(this, getString(R.string.attendance_automation_completed), Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AttendanceWebView(
    url: String,
    onPageFinished: () -> Unit,
    onProvideWebView: (WebView) -> Unit,
    onEvaluateLogin: (WebView) -> Unit,
    onError: (String) -> Unit,
    onWebViewInstance: (WebView) -> Unit,
    canTriggerLogin: Boolean
) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript("(document.body?.innerHTML?.length || 0)", ValueCallback<String> { result ->
                        val length = result?.toIntOrNull() ?: 0
                        if (length < 100) {
                            onError("Page loaded but appears to be blank")
                        } else {
                            onPageFinished()
                        }
                    })
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        onError(error?.description?.toString() ?: "Unknown resource error")
                    }
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        onError("HTTP ${errorResponse?.statusCode ?: 0}: ${errorResponse?.reasonPhrase?.toString() ?: "Unknown HTTP error"}")
                    }
                }
            }
            onWebViewInstance(this)
            onProvideWebView(this)
            if (url.isNotEmpty()) {
                loadUrl(url)
            }
        }
    }, update = { webView ->
        if (url.isNotEmpty() && canTriggerLogin) {
            onEvaluateLogin(webView)
        }
    }, onRelease = { webView ->
        webView.destroy()
    })
}
