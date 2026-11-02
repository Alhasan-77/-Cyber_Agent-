package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.GeminiBorderGradient
import com.example.ui.theme.GeminiOnPillSelectedContainer
import com.example.ui.theme.GeminiPillSelectedContainer
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IdePreviewTabScreen(
    projectName: String,
    projectLanguage: String,
    activeCodeTab: String,
    htmlBuffer: String,
    cssBuffer: String,
    jsBuffer: String,
    backendBuffer: String,
    combinedPreviewHtml: String,
    consoleLogs: List<String>,
    onSelectCodeTab: (String) -> Unit,
    onUpdateCodeBuffer: (String) -> Unit,
    onTriggerInlineSuggestion: (String) -> Unit,
    onGenerateFullStackHighThinking: (String) -> Unit,
    onSaveAndBuildPreview: () -> Unit,
    onLoadTemplate: (String) -> Unit,
    onRunSecurityAudit: () -> Unit,
    onAppendConsoleLog: (String) -> Unit,
    onClearConsoleLogs: () -> Unit
) {
    // 0 = Split (Code + Live Preview), 1 = Code Only, 2 = Live Preview Full
    var viewMode by remember { mutableIntStateOf(0) }
    var showConsoleDrawer by remember { mutableStateOf(true) }
    var aiPromptText by remember { mutableStateOf("") }
    var previewReloadTick by remember { mutableIntStateOf(0) }

    val activeBufferText = when (activeCodeTab) {
        "HTML" -> htmlBuffer
        "CSS" -> cssBuffer
        "JS" -> jsBuffer
        else -> backendBuffer
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Gemini Canvas & IDE Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = projectName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$projectLanguage • ${activeBufferText.lines().size} سطر",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberTextSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onRunSecurityAudit,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ide_security_audit_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فحص", color = CyberTextPrimary)
                        }
                        Button(
                            onClick = {
                                onSaveAndBuildPreview()
                                previewReloadTick++
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("run_build_preview_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل ومعاينة", color = CyberBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Horizontal Scrollable Starter Templates Pill Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قوالب Gemini الجاهزة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberTextSecondary
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onLoadTemplate("ECOMMERCE")
                            previewReloadTick++
                        },
                        label = { Text("🛒 متجر إلكتروني") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("template_ecommerce")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onLoadTemplate("FINTECH")
                            previewReloadTick++
                        },
                        label = { Text("💹 لوحة FinTech") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("template_fintech")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onLoadTemplate("BIOMED")
                            previewReloadTick++
                        },
                        label = { Text("🧬 مختبر BioMed") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("template_biomed")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            onLoadTemplate("THREEJS_BIOGAME")
                            previewReloadTick++
                        },
                        label = { Text("🎮 3D Three.js & BioGame") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("template_threejs_game")
                    )
                }

                // Gemini Copilot Pill Prompt Bar inside IDE
                Surface(
                    color = CyberSurfaceElevated,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, GeminiBorderGradient),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiPromptText,
                            onValueChange = { aiPromptText = it },
                            placeholder = { Text("اطلب من Gemini تعديل الكود أو بناء ميزة...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ide_ai_prompt_input")
                        )
                        OutlinedButton(
                            onClick = {
                                onTriggerInlineSuggestion(
                                    aiPromptText.ifBlank { "تحسين الكود وإضافة مراقبة أداء فورية" }
                                )
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_inline_suggest_button")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إكمال ذكي", color = CyberCyan)
                        }
                        Button(
                            onClick = {
                                onGenerateFullStackHighThinking(
                                    aiPromptText.ifBlank { projectName }
                                )
                                previewReloadTick++
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GeminiPillSelectedContainer,
                                contentColor = GeminiOnPillSelectedContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_high_thinking_build_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بناء بـ Gemini", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // File Tabs + Layout Switcher
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "HTML" to "index.html",
                            "CSS" to "styles.css",
                            "JS" to "app.js",
                            "Backend" to "Server.kt"
                        ).forEach { (tabKey, tabLabel) ->
                            val selected = activeCodeTab == tabKey
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCodeTab(tabKey) },
                                label = { Text(tabLabel) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CyberSurfaceElevated,
                                    selectedContainerColor = GeminiPillSelectedContainer,
                                    selectedLabelColor = GeminiOnPillSelectedContainer
                                ),
                                border = null,
                                modifier = Modifier.testTag("code_tab_$tabKey")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            label = { Text("مقسم") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("view_mode_split")
                        )
                        FilterChip(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            label = { Text("الكود") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("view_mode_code")
                        )
                        FilterChip(
                            selected = viewMode == 2,
                            onClick = { viewMode = 2 },
                            label = { Text("المعاينة") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("view_mode_preview")
                        )
                        FilterChip(
                            selected = showConsoleDrawer,
                            onClick = { showConsoleDrawer = !showConsoleDrawer },
                            label = { Text("Console") },
                            leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("toggle_console_drawer")
                        )
                    }
                }
            }
        }

        // Main Workspace Area (Editor & Live WebView Preview)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (viewMode == 0 || viewMode == 1) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = activeBufferText,
                            onValueChange = onUpdateCodeBuffer,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = CyberTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .testTag("code_editor_input")
                        )
                    }
                }
            }

            if (viewMode == 0 || viewMode == 2) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 المعاينة التفاعلية الحية (Gemini Canvas Preview)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحديث تلقائي • DOM Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                                .testTag("live_webview_preview")
                        ) {
                            val loadKey = remember(combinedPreviewHtml, previewReloadTick) {
                                "${previewReloadTick}_${combinedPreviewHtml.hashCode()}"
                            }
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                val msg = consoleMessage?.message()
                                                if (!msg.isNullOrBlank()) {
                                                    onAppendConsoleLog("[WebView JS] $msg")
                                                }
                                                return true
                                            }
                                        }
                                    }
                                },
                                update = { webView ->
                                    if (webView.tag != loadKey) {
                                        webView.tag = loadKey
                                        webView.loadDataWithBaseURL(
                                            "https://novaforge.local/",
                                            combinedPreviewHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Live Console & Runtime Telemetry Drawer
            AnimatedVisibility(visible = showConsoleDrawer) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🖥️ سجل التشغيل الفوري (${consoleLogs.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onClearConsoleLogs) {
                                Text("مسح السجل", style = MaterialTheme.typography.labelMedium, color = CyberAmber)
                            }
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberBackground)
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                consoleLogs.takeLast(8).forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp
                                        ),
                                        color = CyberEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
