package io.github.kowx712.mmuautoqr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import io.github.kowx712.mmuautoqr.utils.WebViewHtmlSnapshotStore
import io.github.kowx712.mmuautoqr.utils.decodeEvaluateJavascriptResult
import java.io.File
import kotlinx.coroutines.delay

private const val AUTOMATION_ASSET_FILE_NAME = "automation.js"
private const val AUTOMATION_USER_ID_PLACEHOLDER = "__USER_ID__"
private const val AUTOMATION_PASSWORD_PLACEHOLDER = "__PASSWORD__"
private const val AUTOMATION_RUN_ID_PLACEHOLDER = "__RUN_ID__"
private const val HTML_CAPTURE_SCRIPT = "(function(){return document.documentElement.outerHTML;})()"
private const val WEBVIEW_SNAPSHOT_LOG_TAG = "WebViewSnapshots"
private const val INTERMEDIATE_LOGIN_QUERY_MARKER = "cmd=login"
private const val INTERMEDIATE_LOGIN_ERROR_MARKER = "errorCode=105"
private const val EXPIRED_QR_MESSAGE_MARKER = "Please signin from proper QR Code URL"
private const val ATTENDANCE_LOGIN_USER_ID_FIELD = "N_QRCODE_DRV_USERID"
private const val ATTENDANCE_LOGIN_PASSWORD_FIELD = "N_QRCODE_DRV_PASSWORD"
private const val POST_SUBMIT_LOAD_TIMEOUT_MS = 3000L

internal fun renderAutomationScriptTemplate(
    template: String,
    userId: String,
    password: String,
    automationRunId: Int
): String {
    return template
        .replace(AUTOMATION_USER_ID_PLACEHOLDER, escapeForSingleQuotedJsString(userId))
        .replace(AUTOMATION_PASSWORD_PLACEHOLDER, escapeForSingleQuotedJsString(password))
        .replace(AUTOMATION_RUN_ID_PLACEHOLDER, automationRunId.toString())
}

private fun escapeForSingleQuotedJsString(value: String): String {
    return value.replace("\\", "\\\\").replace("'", "\\'")
}

internal fun isIntermediateAttendanceLoginUrl(requestedUrl: String, renderedUrl: String?): Boolean {
    if (requestedUrl.isBlank()) {
        return false
    }

    val normalizedUrl = renderedUrl.orEmpty()
    val hasLoginRedirectMarkers = normalizedUrl.contains(INTERMEDIATE_LOGIN_QUERY_MARKER, ignoreCase = true) &&
        normalizedUrl.contains(INTERMEDIATE_LOGIN_ERROR_MARKER, ignoreCase = true)

    if (!hasLoginRedirectMarkers) {
        return false
    }

    return normalizedUrl.startsWith(requestedUrl, ignoreCase = true)
}

internal fun isExpiredAttendancePage(renderedHtml: String): Boolean {
    if (renderedHtml.isBlank()) {
        return false
    }

    return renderedHtml.contains(EXPIRED_QR_MESSAGE_MARKER, ignoreCase = true)
}

internal fun hasAttendanceLoginForm(renderedHtml: String): Boolean {
    if (renderedHtml.isBlank()) {
        return false
    }

    return renderedHtml.contains(ATTENDANCE_LOGIN_USER_ID_FIELD) &&
        renderedHtml.contains(ATTENDANCE_LOGIN_PASSWORD_FIELD)
}

internal fun shouldReopenQrScannerForAttendancePage(
    requestedUrl: String,
    renderedUrl: String?,
    renderedHtml: String
): Boolean {
    if (requestedUrl.isBlank() || renderedHtml.isBlank()) {
        return false
    }

    if (isExpiredAttendancePage(renderedHtml)) {
        return true
    }

    return isIntermediateAttendanceLoginUrl(requestedUrl, renderedUrl) && !hasAttendanceLoginForm(renderedHtml)
}

internal fun shouldInjectAutomationForUser(
    currentUserIndex: Int,
    lastInjectedUserIndex: Int,
    activeUsersCount: Int,
    hasWebView: Boolean,
    attendanceUrl: String,
    isLoadingPage: Boolean,
    isError: Boolean
): Boolean {
    if (!hasWebView || attendanceUrl.isBlank() || isLoadingPage || isError) {
        return false
    }

    if (currentUserIndex !in 0 until activeUsersCount) {
        return false
    }

    return currentUserIndex != lastInjectedUserIndex
}

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
        val htmlSnapshotStore = remember {
            WebViewHtmlSnapshotStore(
                snapshotDir = File(cacheDir, "webview-snapshots")
            )
        }
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
        var hasStartedInitialLoad by remember { mutableStateOf(false) }
        var lastInjectedUserIndex by remember { mutableIntStateOf(-1) }
        var waitingForPostSubmitLoad by remember { mutableStateOf(false) }
        var postSubmitLoadTimeoutRunnable by remember { mutableStateOf<Runnable?>(null) }
        val attendanceUrl = remember {
            if (intent.action == Intent.ACTION_VIEW) {
                intent.dataString ?: ""
            } else {
                intent.getStringExtra("url") ?: ""
            }
        }
        val automationScriptTemplate = remember {
            assets.open(AUTOMATION_ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        }

        fun restartAutomation() {
            mainHandler.removeCallbacksAndMessages(null)
            automationRunId += 1
            currentUserIndex = 0
            lastInjectedUserIndex = -1
            statusText = getString(R.string.loading_attendance_page)
            isError = false
            errorMessage = ""
            isLoadingPage = true
            isRefreshing = true
            hasStartedInitialLoad = true
            webViewRef?.stopLoading()
            webViewRef?.post {
                if (attendanceUrl.isNotEmpty()) {
                    webViewRef?.loadUrl(attendanceUrl)
                }
            }
        }

        fun reopenQrScanner() {
            mainHandler.removeCallbacksAndMessages(null)
            webViewRef?.stopLoading()
            startActivity(Intent(this@WebViewActivity, QRScannerActivity::class.java))
            finish()
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

        LaunchedEffect(webViewRef, attendanceUrl, hasStartedInitialLoad) {
            val webView = webViewRef ?: return@LaunchedEffect
            if (!hasStartedInitialLoad && attendanceUrl.isNotEmpty()) {
                hasStartedInitialLoad = true
                webView.post {
                    webView.loadUrl(attendanceUrl)
                }
            }
        }

        LaunchedEffect(
            webViewRef,
            attendanceUrl,
            currentUserIndex,
            activeUsers,
            isLoadingPage,
            isError,
            automationRunId
        ) {
            val webView = webViewRef ?: return@LaunchedEffect
            if (!shouldInjectAutomationForUser(
                    currentUserIndex = currentUserIndex,
                    lastInjectedUserIndex = lastInjectedUserIndex,
                    activeUsersCount = activeUsers.size,
                    hasWebView = true,
                    attendanceUrl = attendanceUrl,
                    isLoadingPage = isLoadingPage,
                    isError = isError
                )
            ) {
                return@LaunchedEffect
            }

            val user = activeUsers[currentUserIndex]
            val runId = automationRunId
            lastInjectedUserIndex = currentUserIndex
            delay(1000)
            if (runId != automationRunId) {
                return@LaunchedEffect
            }

            val renderedScript = renderAutomationScriptTemplate(
                template = automationScriptTemplate,
                userId = user.userId,
                password = user.password,
                automationRunId = runId
            )
            webView.evaluateJavascript(renderedScript) {
                webView.evaluateJavascript("automation.init();", null)
            }
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
                    isRefreshing = isRefreshing,
                    onRefresh = ::restartAutomation,
                    onPageFinished = { webView, renderedUrl ->
                        webView.evaluateJavascript(HTML_CAPTURE_SCRIPT) { serializedHtml ->
                            val renderedHtml = decodeEvaluateJavascriptResult(serializedHtml)
                            if (renderedHtml.isBlank()) {
                                return@evaluateJavascript
                            }

                            if (shouldReopenQrScannerForAttendancePage(
                                    requestedUrl = attendanceUrl,
                                    renderedUrl = renderedUrl ?: webView.url,
                                    renderedHtml = renderedHtml
                                )
                            ) {
                                statusText = getString(R.string.loading_attendance_page)
                                isLoadingPage = false
                                isRefreshing = false
                                reopenQrScanner()
                                return@evaluateJavascript
                            }

                            Thread {
                                runCatching {
                                    htmlSnapshotStore.saveSnapshot(
                                        url = renderedUrl ?: webView.url,
                                        html = renderedHtml
                                    )
                                }.onFailure { error ->
                                    Log.w(
                                        WEBVIEW_SNAPSHOT_LOG_TAG,
                                        "Failed to save HTML snapshot for ${renderedUrl ?: webView.url}",
                                        error
                                    )
                                }
                            }.start()
                        }
                        isLoadingPage = false
                        isRefreshing = false
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
                                        waitingForPostSubmitLoad = true

                                        val timeoutRunnable = Runnable {
                                            if (waitingForPostSubmitLoad) {
                                                waitingForPostSubmitLoad = false
                                                proceedToNextUser(activeUsers, submittedUserIndex) { nextIndex ->
                                                    currentUserIndex = nextIndex
                                                }
                                            }
                                        }
                                        postSubmitLoadTimeoutRunnable = timeoutRunnable
                                        mainHandler.postDelayed(timeoutRunnable, POST_SUBMIT_LOAD_TIMEOUT_MS)
                                    }, 100)
                                }
                            }

                            @JavascriptInterface
                            fun onLoginFailed(runId: Int, reason: String) {
                                if (runId != automationRunId) return

                                waitingForPostSubmitLoad = false
                                postSubmitLoadTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                                val failedUserIndex = currentUserIndex
                                if (failedUserIndex < activeUsers.size) {
                                    mainHandler.post {
                                        Toast.makeText(this@WebViewActivity, getString(R.string.login_failed, reason), Toast.LENGTH_SHORT).show()
                                        mainHandler.postDelayed({
                                            proceedToNextUser(activeUsers, failedUserIndex) { nextIndex ->
                                                currentUserIndex = nextIndex
                                            }
                                        }, 1000)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onLoadIndicatorHidden(runId: Int) {
                                if (runId != automationRunId) return
                                if (!waitingForPostSubmitLoad) return

                                waitingForPostSubmitLoad = false
                                postSubmitLoadTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                                val submittedUserIndex = currentUserIndex
                                if (submittedUserIndex < activeUsers.size) {
                                    proceedToNextUser(activeUsers, submittedUserIndex) { nextIndex ->
                                        currentUserIndex = nextIndex
                                    }
                                }
                            }
                        }, "Android")
                    },
                    onError = { message ->
                        isRefreshing = false
                        isError = true
                        errorMessage = message
                        isLoadingPage = false
                    },
                    onWebViewInstance = { webViewRef = it }
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPageFinished: (WebView, String?) -> Unit,
    onProvideWebView: (WebView) -> Unit,
    onError: (String) -> Unit,
    onWebViewInstance: (WebView?) -> Unit,
) {
    AndroidView(
        factory = { context ->
            var currentPageReadyUrl: String? = null
            var hasDispatchedPageReady = false
            val webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        currentPageReadyUrl = url ?: view?.url
                        hasDispatchedPageReady = false
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        view?.postVisualStateCallback(
                            System.nanoTime(),
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    val currentWebView = view
                                    val readyUrl = url ?: currentWebView.url
                                    if (!hasDispatchedPageReady && (currentPageReadyUrl == null || readyUrl == currentPageReadyUrl)) {
                                        hasDispatchedPageReady = true
                                        onPageFinished(currentWebView, readyUrl)
                                    }
                                }
                            }
                        )
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val currentWebView = view ?: return
                        val readyUrl = url ?: currentWebView.url
                        if (!hasDispatchedPageReady && (currentPageReadyUrl == null || readyUrl == currentPageReadyUrl)) {
                            hasDispatchedPageReady = true
                            onPageFinished(currentWebView, readyUrl)
                        }
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
