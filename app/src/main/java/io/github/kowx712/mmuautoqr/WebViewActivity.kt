package io.github.kowx712.mmuautoqr

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
            val darkTheme = isSystemInDarkTheme()
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                ) { !darkTheme }
            )
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

    @Composable
    private fun WebViewScreen() {
        val context = LocalContext.current
        val userManager = remember { UserManager(this@WebViewActivity) }
        val activeUsers = remember { userManager.activeUsers }
        var currentUserIndex by remember { mutableIntStateOf(0) }
        var statusText by remember { mutableStateOf(getString(R.string.loading_attendance_page)) }
        var isLoading by remember { mutableStateOf(true) }
        val attendanceUrl = remember { intent.getStringExtra("url") ?: "" }

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
                        isLoading = false
                        // Delay to ensure page ready
                        mainHandler.postDelayed({ startAutoLogin(activeUsers, currentUserIndex) }, 2000)
                    },
                    onProvideWebView = { webView ->
                        webView.addJavascriptInterface(object : Any() {
                            val currentUser = activeUsers[currentUserIndex]
                            @JavascriptInterface
                            fun onLoginSubmitted() {
                                mainHandler.postDelayed({
                                    statusText = getString(R.string.login_submitted, currentUser.name)
                                    mainHandler.postDelayed({
                                        proceedToNextUser(activeUsers) { nextIndex ->
                                            currentUserIndex = nextIndex
                                        }
                                    }, 3000)
                                }, 100)
                            }

                            @JavascriptInterface
                            fun onLoginFailed(reason: String) {
                                mainHandler.post {
                                    Toast.makeText(this@WebViewActivity, getString(R.string.login_failed, reason), Toast.LENGTH_SHORT).show()
                                    statusText = getString(R.string.login_failed, currentUser.name)
                                    mainHandler.postDelayed({
                                        proceedToNextUser(activeUsers) { nextIndex ->
                                            currentUserIndex = nextIndex
                                        }
                                    }, 2000)
                                }
                            }
                        }, "Android")
                    },
                    onEvaluateLogin = { webView ->
                        val user = activeUsers[currentUserIndex]
                        val js = "javascript:" +
                                "function fillAndSubmit(user, pass) {" +
                                "  var userField = document.querySelector('input[type=\"text\"], input[name*=\"user\"], input[id*=\"user\"], input[placeholder*=\"User\"]');" +
                                "  var passField = document.querySelector('input[type=\"password\"]');" +
                                "  var submitBtn = document.querySelector('input[type=\"submit\"], button[type=\"submit\"], input[value*=\"Sign\"], button');" +
                                "  if (userField && passField) {" +
                                "    userField.value = ''; passField.value = '';" +
                                "    setTimeout(function() {" +
                                "      userField.value = '" + user.userId + "';" +
                                "      passField.value = '" + user.password + "';" +
                                "      if (submitBtn) { submitBtn.click(); Android.onLoginSubmitted(); }" +
                                "      else { Android.onLoginFailed('Submit button not found'); }" +
                                "    }, 500);" +
                                "  } else { Android.onLoginFailed('Login fields not found'); }" +
                                "} fillAndSubmit('" + user.userId + "', '" + user.password + "');"
                        webView.evaluateJavascript(js, null)
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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

    private fun startAutoLogin(activeUsers: List<User>, currentIndex: Int) {
        // Triggered via onPageFinished -> onEvaluateLogin in the WebView composable
    }

    private fun proceedToNextUser(activeUsers: List<User>, onIndex: (Int) -> Unit) {
        val next = activeUsers.indexOfFirst { it == activeUsers.lastOrNull() } + 1
        if (next < activeUsers.size) {
            onIndex(next)
        } else {
            Toast.makeText(this, getString(R.string.attendance_automation_completed), Toast.LENGTH_LONG).show()
            mainHandler.postDelayed({ finish() }, 3000)
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
) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    onPageFinished()
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }
            }
            onProvideWebView(this)
            loadUrl(url)
        }
    }, update = { webView ->
        onEvaluateLogin(webView)
    })
}


