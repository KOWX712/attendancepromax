package io.github.kowx712.mmuautoqr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
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
                            val js = "javascript:" +
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
                                    "fillAndSubmit('" + user.userId + "', '" + user.password + "');";
                            webView.evaluateJavascript(js, null)
                        }
                    },
                    canTriggerLogin = initialLoginCanProceed
                )

                if (isLoadingPage) {
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
    canTriggerLogin: Boolean
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
            }
            onProvideWebView(this)
            if (url.isNotEmpty()) {
                loadUrl(url)
            }
        }
    }, update = { webView ->
        if (url.isNotEmpty() && canTriggerLogin) {
            onEvaluateLogin(webView)
        }
    })
}
