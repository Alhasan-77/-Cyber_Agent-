package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured representation of an initialization or runtime exception captured by
 * [GlobalExceptionHandler] or [ErrorBoundary].
 */
data class AppErrorReport(
    val id: String = System.currentTimeMillis().toString(),
    val phase: String,
    val title: String,
    val message: String,
    val exceptionType: String,
    val stackTrace: String,
    val recoverySuggestion: String,
    val threadName: String = Thread.currentThread().name,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
    val isFatal: Boolean = false
)

/**
 * CompositionLocal allowing any child Composable to report initialization or UI boundary
 * errors directly to the nearest [ErrorBoundary].
 */
val LocalErrorBoundaryReporter = compositionLocalOf<(String, Throwable, Boolean) -> Unit> {
    { phase, throwable, isFatal ->
        GlobalExceptionHandler.reportError(phase = phase, throwable = throwable, isFatal = isFatal)
    }
}

/**
 * Global exception handler and initialization validator that replaces silent failure states
 * with observable, actionable diagnostic reports in the UI.
 */
object GlobalExceptionHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "GlobalExceptionHandler"

    private var previousDefaultHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile
    private var isInstalled = false

    private val _latestError = MutableStateFlow<AppErrorReport?>(null)
    val latestError: StateFlow<AppErrorReport?> = _latestError.asStateFlow()

    private val _errorHistory = MutableStateFlow<List<AppErrorReport>>(emptyList())
    val errorHistory: StateFlow<List<AppErrorReport>> = _errorHistory.asStateFlow()

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        reportError(
            phase = "Coroutine Execution",
            throwable = throwable,
            isFatal = false
        )
    }

    /**
     * Installs the global uncaught exception handler on the process and main looper so
     * uncaught initialization or runtime crashes are captured and displayed in the UI.
     */
    fun install() {
        if (isInstalled) return
        synchronized(this) {
            if (isInstalled) return
            previousDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(this)
            isInstalled = true
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
        val report = buildErrorReport(
            phase = "UncaughtException (${thread.name})",
            throwable = throwable,
            isFatal = true
        )
        _latestError.value = report
        _errorHistory.value = (_errorHistory.value + report).takeLast(20)

        // If the exception happened on a background thread, keep the UI alive to display the error report.
        // If it happened on the main thread during a frame, post a new loop so the ErrorBoundary UI can render.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                Looper.loop()
            } catch (nested: Throwable) {
                Log.e(TAG, "Nested exception while recovering main looper", nested)
                val nestedReport = buildErrorReport(
                    phase = "MainLooper Recovery",
                    throwable = nested,
                    isFatal = true
                )
                _latestError.value = nestedReport
            }
        }
    }

    /**
     * Runs startup and Firebase/Firestore/Auth configuration checks and records any failure
     * into [latestError] so initialization problems are immediately visible in the UI.
     */
    fun verifyStartupInitialization(context: Context): Result<Unit> {
        return runCatching {
            val appContext = context.applicationContext ?: context

            // 1. Verify FirebaseApp initialization
            val firebaseApp = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(appContext)
                    ?: throw IllegalStateException(
                        "تعذر تهيئة FirebaseApp: تأكد من صلاحية ملف google-services.json",
                        e
                    )
            }
            check(firebaseApp.options.projectId.orEmpty().isNotBlank()) {
                "معرّف مشروع Firebase (projectId) فارغ في إعدادات التطبيق."
            }

            // 2. Verify Named Firestore Database ID resource
            val databaseId = runCatching {
                appContext.getString(R.string.firestore_database_id)
            }.getOrElse { e ->
                throw IllegalStateException(
                    "المورد R.string.firestore_database_id مفقود في firebase_applet_config.xml",
                    e
                )
            }
            check(databaseId.isNotBlank()) {
                "قيمة firestore_database_id فارغة؛ لا يمكن الاتصال بقاعدة بيانات Firestore المسماة."
            }

            // 3. Verify Google Sign-In Web Client ID resource
            val webClientId = runCatching {
                appContext.getString(R.string.default_web_client_id)
            }.getOrElse { e ->
                throw IllegalStateException(
                    "المورد R.string.default_web_client_id مفقود؛ يلزم لتسجيل الدخول عبر Google Sign-In.",
                    e
                )
            }
            check(webClientId.isNotBlank()) {
                "قيمة default_web_client_id فارغة في موارد التطبيق."
            }

            // 4. Verify FirebaseAuth & FirebaseFirestore instances can be resolved
            Firebase.auth
            FirebaseFirestore.getInstance(databaseId)
            Unit
        }.onFailure { throwable ->
            reportError(
                phase = "Startup & Firebase Initialization",
                throwable = throwable,
                isFatal = true
            )
        }
    }

    fun reportError(
        phase: String,
        throwable: Throwable,
        isFatal: Boolean = false,
        customTitle: String? = null
    ) {
        Log.e(TAG, "Error reported in phase [$phase]: ${throwable.message}", throwable)
        val report = buildErrorReport(
            phase = phase,
            throwable = throwable,
            isFatal = isFatal,
            customTitle = customTitle
        )
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _latestError.value = report
            _errorHistory.value = (_errorHistory.value + report).takeLast(20)
        } else {
            Handler(Looper.getMainLooper()).post {
                _latestError.value = report
                _errorHistory.value = (_errorHistory.value + report).takeLast(20)
            }
        }
    }

    fun clearError() {
        _latestError.value = null
    }

    private fun buildErrorReport(
        phase: String,
        throwable: Throwable,
        isFatal: Boolean,
        customTitle: String? = null
    ): AppErrorReport {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val fullStack = sw.toString()

        val rootCause = generateSequence(throwable) { it.cause }.last()
        val exceptionName = throwable::class.java.simpleName.ifBlank { "Exception" }
        val message = throwable.localizedMessage
            ?: rootCause.localizedMessage
            ?: "حدث خطأ غير متوقع أثناء تهيئة أو تشغيل التطبيق"

        val suggestion = when {
            message.contains("default_web_client_id", ignoreCase = true) ||
                message.contains("Credential", ignoreCase = true) ->
                "تحقق من إعداد حساب Google في الجهاز/المحاكي ومن تطابق بصمة شهادة SHA-1/SHA-256 في google-services.json."
            message.contains("firestore", ignoreCase = true) ||
                message.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "تحقق من تسجيل الدخول بحساب Google، ومن مطابقة firestore_database_id وقواعد الأمان firestore.rules."
            message.contains("WebView", ignoreCase = true) ->
                "تحقق من توفر مكون Android System WebView في النظام وإعادة تحميل نافذة المعاينة."
            else ->
                "يمكنك الضغط على «إعادة تهيئة التطبيق» لإعادة فحص المكونات السحابية أو نسخ التقرير التشخيصي لمراجعته."
        }

        return AppErrorReport(
            phase = phase,
            title = customTitle ?: "تنبيه تشخيصي في مرحلة: $phase",
            message = message,
            exceptionType = "${throwable::class.java.name} (Root: ${rootCause::class.java.simpleName})",
            stackTrace = fullStack,
            recoverySuggestion = suggestion,
            isFatal = isFatal
        )
    }
}

/**
 * Root Compose ErrorBoundary that catches initialization failures, monitors [GlobalExceptionHandler],
 * and renders either a full diagnostic recovery screen (for fatal initialization errors) or a rich
 * interactive diagnostic banner (for non-fatal errors) instead of failing silently.
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    onRetryInitialization: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activeError by GlobalExceptionHandler.latestError.collectAsState()
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTick) {
        GlobalExceptionHandler.verifyStartupInitialization(context)
    }

    DisposableEffect(Unit) {
        GlobalExceptionHandler.install()
        onDispose { }
    }

    val reporter: (String, Throwable, Boolean) -> Unit = remember {
        { phase, throwable, isFatal ->
            GlobalExceptionHandler.reportError(
                phase = phase,
                throwable = throwable,
                isFatal = isFatal
            )
        }
    }

    CompositionLocalProvider(LocalErrorBoundaryReporter provides reporter) {
        Box(modifier = modifier.fillMaxSize()) {
            val currentError = activeError
            if (currentError != null && currentError.isFatal) {
                ErrorBoundaryDiagnosticScreen(
                    errorReport = currentError,
                    onRetry = {
                        GlobalExceptionHandler.clearError()
                        retryTick++
                        onRetryInitialization()
                    },
                    onDismissAndContinue = {
                        GlobalExceptionHandler.clearError()
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = currentError != null) {
                        if (currentError != null) {
                            ErrorBoundaryBanner(
                                errorReport = currentError,
                                onRetry = {
                                    GlobalExceptionHandler.clearError()
                                    retryTick++
                                    onRetryInitialization()
                                },
                                onDismiss = {
                                    GlobalExceptionHandler.clearError()
                                }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorBoundaryBanner(
    errorReport: AppErrorReport,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showStackTrace by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.5.dp, CyberCoral, RoundedCornerShape(14.dp))
            .testTag("error_boundary_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = CyberCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = errorReport.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = CyberCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${errorReport.exceptionType} • ${errorReport.timestamp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberAmber
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("error_boundary_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق التنبيه",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = errorReport.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "💡 المقترح: ${errorReport.recoverySuggestion}",
                style = MaterialTheme.typography.labelMedium,
                color = CyberCyan
            )

            AnimatedVisibility(visible = showStackTrace) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBackground)
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorReport.stackTrace,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp
                            ),
                            color = CyberAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showStackTrace = !showStackTrace },
                    modifier = Modifier.testTag("error_boundary_toggle_stack_button")
                ) {
                    Icon(
                        imageVector = if (showStackTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showStackTrace) "إخفاء التفاصيل التقنية" else "عرض Stack Trace",
                        color = CyberCyan
                    )
                }

                TextButton(
                    onClick = {
                        copyReportToClipboard(context, errorReport)
                        copiedFeedback = true
                    },
                    modifier = Modifier.testTag("error_boundary_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copiedFeedback) "تم النسخ ✓" else "نسخ التقرير",
                        color = CyberEmerald
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("error_boundary_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CyberBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إعادة المحاولة",
                        color = CyberBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorBoundaryDiagnosticScreen(
    errorReport: AppErrorReport,
    onRetry: () -> Unit,
    onDismissAndContinue: () -> Unit
) {
    val context = LocalContext.current
    var copiedFeedback by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CyberBackground, CyberSurface, CyberBackground)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("error_boundary_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CyberCoral.copy(alpha = 0.16f))
                    .border(2.dp, CyberCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "System Initialization Error",
                    tint = CyberCoral,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "نظام الحماية والتشخيص الفوري (ErrorBoundary)",
                style = MaterialTheme.typography.titleLarge,
                color = CyberCoral,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = CyberSurface,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, CyberAmber.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Text(
                    text = "المرحلة: ${errorReport.phase} • الخيط: ${errorReport.threadName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberAmber,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCoral.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = errorReport.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = errorReport.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "نوع الاستثناء: ${errorReport.exceptionType}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = CyberAmber
                    )

                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "💡 خطوات الحل المقترحة:\n${errorReport.recoverySuggestion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberEmerald,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "سجل التتبع التقني (Stack Trace):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberBackground)
                                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorReport.stackTrace,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = CyberAmber
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("error_boundary_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CyberBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعادة تهيئة التطبيق",
                        color = CyberBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        copyReportToClipboard(context, errorReport)
                        copiedFeedback = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("error_boundary_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = CyberEmerald
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copiedFeedback) "تم النسخ ✓" else "نسخ التقرير",
                        color = CyberEmerald
                    )
                }
            }

            TextButton(
                onClick = onDismissAndContinue,
                modifier = Modifier.testTag("error_boundary_dismiss_button")
            ) {
                Text(
                    text = "تجاهل الخطأ والمتابعة إلى واجهة التطبيق",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun copyReportToClipboard(context: Context, report: AppErrorReport) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val formatted = buildString {
            appendLine("=== NovaForge AI Studio Diagnostic Report ===")
            appendLine("Timestamp: ${report.timestamp}")
            appendLine("Phase: ${report.phase}")
            appendLine("Thread: ${report.threadName}")
            appendLine("Exception: ${report.exceptionType}")
            appendLine("Message: ${report.message}")
            appendLine("Suggestion: ${report.recoverySuggestion}")
            appendLine("--- Stack Trace ---")
            appendLine(report.stackTrace)
        }
        clipboard?.setPrimaryClip(ClipData.newPlainText("NovaForge Error Report", formatted))
    }
}
