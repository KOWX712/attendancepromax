package io.github.kowx712.mmuautoqr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.JavascriptInterface
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
import androidx.compose.material3.Scaffold
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.kowx712.mmuautoqr.models.User
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.UserManager

class WebViewActivity : ComponentActivity() {
    private lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
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

        LaunchedEffect(userManager) {
            isLoadingUsers = true
            activeUsers = userManager.getUsers().filter { it.isActive }
            isLoadingUsers = false
        }

        var currentUserIndex by remember { mutableIntStateOf(0) }
        var statusText by remember { mutableStateOf(getString(R.string.loading_attendance_page)) }
        var isLoadingPage by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var isRefreshing by remember { mutableStateOf(false) }
        var automationRunId by remember { mutableIntStateOf(0) }
        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        val attendanceUrl = remember {
            if (intent.action == Intent.ACTION_VIEW) {
                intent.dataString ?: ""
            } else {
                intent.getStringExtra("url") ?: ""
            }
        }
        var initialLoginCanProceed by remember { mutableStateOf(false) }

        fun restartAutomation() {
            mainHandler.removeCallbacksAndMessages(null)
            automationRunId += 1
            currentUserIndex = 0
            statusText = getString(R.string.loading_attendance_page)
            isError = false
            errorMessage = ""
            isLoadingPage = true
            isRefreshing = true
            initialLoginCanProceed = false
            webViewRef?.stopLoading()
            webViewRef?.post {
                if (attendanceUrl.isNotEmpty()) {
                    webViewRef?.loadUrl(attendanceUrl)
                }
            }
        }

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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(12.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AttendanceWebView(
                    url = attendanceUrl,
                    isRefreshing = isRefreshing,
                    onRefresh = ::restartAutomation,
                    onPageFinished = {
                        isLoadingPage = false
                        isRefreshing = false
                        mainHandler.postDelayed({
                            initialLoginCanProceed = true
                        }, 2000)
                    },
                    onProvideWebView = { webView ->
                        webView.addJavascriptInterface(object : Any() {
                            @JavascriptInterface
                            fun onLoginSubmitted(runId: Int) {
                                if (runId != automationRunId) return

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
                            fun onLoginFailed(runId: Int, reason: String) {
                                if (runId != automationRunId) return

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
                            webView.evaluateJavascript(buildLoginAutomationScript(user.userId, user.password, automationRunId), null)
                        }
                    },
                    onError = { message ->
                        isRefreshing = false
                        isError = true
                        errorMessage = message
                        isLoadingPage = false
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
                        Button(onClick = ::restartAutomation) {
                            Text("Retry")
                        }
                    }
                }
            }
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPageFinished: () -> Unit,
    onProvideWebView: (WebView) -> Unit,
    onEvaluateLogin: (WebView) -> Unit,
    onError: (String) -> Unit,
    onWebViewInstance: (WebView?) -> Unit,
    canTriggerLogin: Boolean
) {
    AndroidView(
        factory = { context ->
            val webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        view?.postVisualStateCallback(
                            SystemClock.uptimeMillis(),
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    onPageFinished()
                                }
                            }
                        )
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
                            onError("HTTP ${errorResponse?.statusCode ?: 0}: ${errorResponse?.reasonPhrase ?: "Unknown HTTP error"}")
                        }
                    }
                }
                onWebViewInstance(this)
                onProvideWebView(this)
                if (url.isNotEmpty()) {
                    post {
                        loadUrl(url)
                    }
                }
            }

            SwipeRefreshLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnRefreshListener(onRefresh)
                setOnChildScrollUpCallback { _, _ -> false }
                addView(webView)
            }
        },
        update = { swipeRefreshLayout ->
            swipeRefreshLayout.isRefreshing = isRefreshing
            val webView = swipeRefreshLayout.getChildAt(0) as? WebView ?: return@AndroidView
            if (url.isNotEmpty() && canTriggerLogin) {
                onEvaluateLogin(webView)
            }
        },
        onRelease = { swipeRefreshLayout ->
            val webView = swipeRefreshLayout.getChildAt(0) as? WebView
            swipeRefreshLayout.setOnRefreshListener(null)
            swipeRefreshLayout.removeAllViews()
            onWebViewInstance(null)
            webView?.destroy()
        }
    )
}
