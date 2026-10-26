package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberViolet
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
    onSelectCodeTab: (String) -> Unit,
    onUpdateCodeBuffer: (String) -> Unit,
    onTriggerInlineSuggestion: (String) -> Unit,
    onGenerateFullStackHighThinking: (String) -> Unit,
    onSaveAndBuildPreview: () -> Unit
) {
    // 0 = Split (Code + Live Preview), 1 = Code Only, 2 = Live Preview Full
    var viewMode by remember { mutableIntStateOf(0) }
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top IDE Control Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan
                        )
                        Text(
                            text = "اللغة النشطة: $projectLanguage • الأسطر: ${activeBufferText.lines().size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onSaveAndBuildPreview()
                            previewReloadTick++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier.testTag("run_build_preview_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بناء ومعاينة حية", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }

                // AI Copilot Quick Bar (Low-Latency gemini-3.1-flash-lite + High Thinking gemini-4-argon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiPromptText,
                        onValueChange = { aiPromptText = it },
                        placeholder = { Text("اطلب تعديل الواجهة أو توليد ميزة Full-Stack...") },
                        singleLine = true,
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
                        modifier = Modifier.testTag("ai_inline_suggest_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اقتراح سريع", color = CyberCyan)
                    }
                    Button(
                        onClick = {
                            onGenerateFullStackHighThinking(
                                aiPromptText.ifBlank { projectName }
                            )
                            previewReloadTick++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                        modifier = Modifier.testTag("ai_high_thinking_build_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تفكير عميق")
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
                            "Backend" to "Server.kt / .py"
                        ).forEach { (tabKey, tabLabel) ->
                            FilterChip(
                                selected = activeCodeTab == tabKey,
                                onClick = { onSelectCodeTab(tabKey) },
                                label = { Text(tabLabel) },
                                modifier = Modifier.testTag("code_tab_$tabKey")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            label = { Text("مقسم") },
                            modifier = Modifier.testTag("view_mode_split")
                        )
                        FilterChip(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            label = { Text("الكود") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            modifier = Modifier.testTag("view_mode_code")
                        )
                        FilterChip(
                            selected = viewMode == 2,
                            onClick = { viewMode = 2 },
                            label = { Text("Preview") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            modifier = Modifier.testTag("view_mode_preview")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (viewMode == 0 || viewMode == 1) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBackground),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = activeBufferText,
                            onValueChange = onUpdateCodeBuffer,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberBackground,
                                unfocusedContainerColor = CyberBackground
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 نافذة المعاينة الحية التفاعلية (Live Interactive Preview)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
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
                                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                                .testTag("live_webview_preview")
                        ) {
                            val htmlSnapshot = remember(combinedPreviewHtml, previewReloadTick) {
                                combinedPreviewHtml
                            }
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(
                                        "https://novaforge.local/",
                                        htmlSnapshot,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
