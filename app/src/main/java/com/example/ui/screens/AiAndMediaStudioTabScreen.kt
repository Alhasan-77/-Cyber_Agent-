package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AiRolePersona
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedMediaAsset
import com.example.data.remote.GeminiAiRepository
import com.example.data.remote.OpenAiNetworkClient
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.GeminiBorderGradient
import com.example.ui.theme.GeminiOnPillSelectedContainer
import com.example.ui.theme.GeminiPillSelectedContainer
import com.example.ui.theme.GeminiSparkleGradient
import com.example.util.AudioAndMediaHelper
import kotlinx.coroutines.launch

@Composable
fun AiAndMediaStudioTabScreen(
    chatMessages: List<ChatMessage>,
    selectedChatModel: String,
    selectedPersona: AiRolePersona,
    useSearchGrounding: Boolean,
    useHighThinking: Boolean,
    generatedAssets: List<GeneratedMediaAsset>,
    mediaAnalysisOutput: String?,
    liveVoiceHistory: List<String>,
    lastTranscribedText: String,
    onSelectChatModel: (String) -> Unit,
    onSelectPersona: (AiRolePersona) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onToggleHighThinking: (Boolean) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onGenerateImage: (String, Boolean, String, String, String?, String?) -> Unit,
    onGenerateVideo: (String, String, String?, String?) -> Unit,
    onAnalyzeMedia: (String, String, String, Boolean) -> Unit,
    onSynthesizeSpeech: (String, String) -> Unit,
    onTranscribeAudio: (String, (String) -> Unit) -> Unit,
    onSendLiveVoiceTurn: (String, String?) -> Unit,
    onGenerateMusic: (String, Boolean) -> Unit,
    onSpeakText: (String) -> Unit,
    isBusy: Boolean = false
) {
    // 0 = Multi-Turn Chatbot, 1 = Image & Video Studio, 2 = Voice, Audio & Music Lab
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Gemini Pill Segmented Mode Switcher
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("محادثة الوكيل") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = CyberTextSecondary,
                        selectedContainerColor = GeminiPillSelectedContainer,
                        selectedLabelColor = GeminiOnPillSelectedContainer,
                        selectedLeadingIconColor = GeminiOnPillSelectedContainer
                    ),
                    border = null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_subtab_chat")
                )
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("الصور والفيديو") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = CyberTextSecondary,
                        selectedContainerColor = GeminiPillSelectedContainer,
                        selectedLabelColor = GeminiOnPillSelectedContainer,
                        selectedLeadingIconColor = GeminiOnPillSelectedContainer
                    ),
                    border = null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_subtab_visual")
                )
                FilterChip(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    label = { Text("الصوت والموسيقى") },
                    leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = CyberTextSecondary,
                        selectedContainerColor = GeminiPillSelectedContainer,
                        selectedLabelColor = GeminiOnPillSelectedContainer,
                        selectedLeadingIconColor = GeminiOnPillSelectedContainer
                    ),
                    border = null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_subtab_audio")
                )
            }
        }

        when (subTab) {
            0 -> MultiTurnChatbotSection(
                chatMessages = chatMessages,
                selectedChatModel = selectedChatModel,
                selectedPersona = selectedPersona,
                useSearchGrounding = useSearchGrounding,
                useHighThinking = useHighThinking,
                onSelectChatModel = onSelectChatModel,
                onSelectPersona = onSelectPersona,
                onToggleSearch = onToggleSearch,
                onToggleHighThinking = onToggleHighThinking,
                onSendChatMessage = onSendChatMessage,
                onSpeakText = onSpeakText,
                isBusy = isBusy
            )
            1 -> VisualMediaStudioSection(
                generatedAssets = generatedAssets,
                mediaAnalysisOutput = mediaAnalysisOutput,
                onGenerateImage = onGenerateImage,
                onGenerateVideo = onGenerateVideo,
                onAnalyzeMedia = onAnalyzeMedia
            )
            else -> VoiceAndMusicStudioSection(
                liveVoiceHistory = liveVoiceHistory,
                lastTranscribedText = lastTranscribedText,
                generatedAssets = generatedAssets,
                onSynthesizeSpeech = onSynthesizeSpeech,
                onTranscribeAudio = onTranscribeAudio,
                onSendLiveVoiceTurn = onSendLiveVoiceTurn,
                onGenerateMusic = onGenerateMusic
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiTurnChatbotSection(
    chatMessages: List<ChatMessage>,
    selectedChatModel: String,
    selectedPersona: AiRolePersona,
    useSearchGrounding: Boolean,
    useHighThinking: Boolean,
    onSelectChatModel: (String) -> Unit,
    onSelectPersona: (AiRolePersona) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onToggleHighThinking: (Boolean) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    isBusy: Boolean
) {
    var inputPrompt by remember { mutableStateOf("") }
    var showPersonaSelector by remember { mutableStateOf(false) }
    val isOpenAiReady = remember { OpenAiNetworkClient.isApiKeyConfigured() }
    val openAiStatusLabel = remember { OpenAiNetworkClient.getKeyStatusSummaryAr() }

    val quickSuggestions = remember {
        listOf(
            "أنشئ مشروع متجر إلكتروني بـ Kotlin و Firestore",
            "احفظ المشروع الحالي وأنشئ لقطة إصدار",
            "راجع الكود الحالي وحدد المخاطر الأمنية",
            "أضف ميزة تسجيل الدخول إلى الملف المفتوح"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sleek Gemini Model & Capability Strip
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✦ Gemini عبر Firebase AI",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🟢 ChatGPT: $openAiStatusLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOpenAiReady) CyberEmerald else CyberAmber,
                        modifier = Modifier.testTag("openai_api_status_badge")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "☁️ النموذج المفضل: $selectedChatModel • ${selectedPersona.titleAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("saved_preferred_ai_model_label")
                    )
                    Surface(
                        onClick = { showPersonaSelector = !showPersonaSelector },
                        color = CyberSurfaceElevated,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (showPersonaSelector) "إخفاء الأدوار" else "تخصيص الدور",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }
                }

                // Horizontal scrollable Gemini Model & Grounding Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        GeminiAiRepository.MODEL_DUAL_ENGINE to "✦ Dual (Gemini + ChatGPT)",
                        GeminiAiRepository.MODEL_GPT_4O to "🟢 GPT-4o",
                        GeminiAiRepository.MODEL_GPT_4O_MINI to "🟢 GPT-4o Mini",
                        GeminiAiRepository.MODEL_ARGON to "✦ Gemini 4 Argon",
                        GeminiAiRepository.MODEL_FLASH to "✦ Gemini 3.8 Flash",
                        GeminiAiRepository.MODEL_FLASH_LITE to "✦ Flash Lite"
                    ).forEach { (modelId, label) ->
                        val isSelected = selectedChatModel == modelId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChatModel(modelId) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CyberSurfaceElevated,
                                labelColor = CyberTextSecondary,
                                selectedContainerColor = GeminiPillSelectedContainer,
                                selectedLabelColor = GeminiOnPillSelectedContainer
                            ),
                            border = null,
                            modifier = Modifier.testTag("chat_model_$modelId")
                        )
                    }
                    FilterChip(
                        selected = useHighThinking,
                        onClick = { onToggleHighThinking(!useHighThinking) },
                        label = { Text("تفكير عميق") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CyberSurfaceElevated,
                            selectedContainerColor = GeminiPillSelectedContainer,
                            selectedLabelColor = GeminiOnPillSelectedContainer
                        ),
                        border = null,
                        modifier = Modifier.testTag("toggle_high_thinking")
                    )
                    FilterChip(
                        selected = useSearchGrounding,
                        onClick = { onToggleSearch(!useSearchGrounding) },
                        label = { Text("بحث Google") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CyberSurfaceElevated,
                            selectedContainerColor = GeminiPillSelectedContainer,
                            selectedLabelColor = GeminiOnPillSelectedContainer
                        ),
                        border = null,
                        modifier = Modifier.testTag("toggle_search_grounding")
                    )
                }

                AnimatedVisibility(visible = showPersonaSelector) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AiRolePersona.entries.forEach { persona ->
                            FilterChip(
                                selected = selectedPersona == persona,
                                onClick = { onSelectPersona(persona) },
                                label = { Text(persona.titleAr) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            }
        }

        // Authentic Gemini Conversation Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gemini Welcome Header + Suggestion Pills at top of chat
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "اطلب من Cyber Agent تنفيذ مهمة أو تحليل مشروعك",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = GeminiSparkleGradient
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSuggestions.forEach { suggestion ->
                            Surface(
                                color = CyberSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.clickable {
                                    onSendChatMessage(suggestion)
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(chatMessages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    // Authentic Gemini User Message Pill (Start/Right aligned in RTL)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = 6.dp,
                                bottomEnd = 24.dp
                            ),
                            modifier = Modifier.widthIn(max = 340.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CyberTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "أنت • ${msg.timestampLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Authentic Gemini AI Response Card with 4-point Sparkle Avatar
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(GeminiSparkleGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = msg.modelUsed,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = msg.timestampLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberTextSecondary
                                    )
                                    IconButton(
                                        onClick = { onSpeakText(msg.text) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "استماع للرد عبر TTS",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = CyberTextPrimary
                            )

                            if (msg.groundingSources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = CyberSurfaceElevated,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "مصادر التنفيذ والمعلومات:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyberEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                        msg.groundingSources.forEach { src ->
                                            Text(
                                                text = "• $src",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = CyberCyan
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

        // Iconic Google Gemini Floating Pill Prompt Bar
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, GeminiBorderGradient),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    enabled = !isBusy,
                    placeholder = {
                        Text(
                            text = "اكتب طلباً أو مهمة إلى Cyber Agent...",
                            color = CyberTextSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chatbot_message_input")
                )

                Button(
                    enabled = !isBusy && inputPrompt.isNotBlank(),
                    onClick = {
                        if (inputPrompt.isNotBlank() && !isBusy) {
                            onSendChatMessage(inputPrompt)
                            inputPrompt = ""
                        }
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = CyberBackground
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("chatbot_send_button")
                ) {
                    if (isBusy) {
                        Text("...", color = CyberBackground, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = CyberBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisualMediaStudioSection(
    generatedAssets: List<GeneratedMediaAsset>,
    mediaAnalysisOutput: String?,
    onGenerateImage: (String, Boolean, String, String, String?, String?) -> Unit,
    onGenerateVideo: (String, String, String?, String?) -> Unit,
    onAnalyzeMedia: (String, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var visualPrompt by remember {
        mutableStateOf("تصميم واجهة لوحة تحكم سحابية مستقبلية بألوان Gemini الزرقاء والبنفسجية")
    }
    var useStudioProImage by remember { mutableStateOf(true) }
    val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    val imageSizes = listOf("1K", "2K", "4K")
    var selectedImageSize by remember { mutableStateOf("2K") }
    var selectedVideoRatio by remember { mutableStateOf("16:9") }

    var uploadedBase64 by remember { mutableStateOf<String?>(null) }
    var uploadedMime by remember { mutableStateOf<String?>(null) }
    var uploadedIsVideo by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val pair = AudioAndMediaHelper.uriToBase64(context, uri)
                if (pair != null) {
                    uploadedBase64 = pair.first
                    uploadedMime = pair.second
                    uploadedIsVideo = pair.second.startsWith("video/")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeminiBorderGradient, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "استوديو Gemini لتوليد الصور والفيديو وتحليل الوسائط",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = visualPrompt,
                        onValueChange = { visualPrompt = it },
                        label = { Text("وصف الصورة أو الفيديو أو سؤال التحليل (Prompt)") },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("visual_studio_prompt_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useStudioProImage,
                            onClick = { useStudioProImage = false },
                            label = { Text("gemini-nano-banana-2.1") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("model_nano_banana")
                        )
                        FilterChip(
                            selected = useStudioProImage,
                            onClick = { useStudioProImage = true },
                            label = { Text("gemini-3-pro-image") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("model_pro_image")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الدقة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberTextSecondary
                        )
                        imageSizes.forEach { size ->
                            FilterChip(
                                selected = selectedImageSize == size,
                                onClick = { selectedImageSize = size },
                                label = { Text(size) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("image_size_$size")
                            )
                        }
                    }

                    Text(
                        text = "نسبة العرض إلى الارتفاع للصور (Aspect Ratio):",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberTextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        aspectRatios.forEach { ratio ->
                            FilterChip(
                                selected = selectedAspectRatio == ratio,
                                onClick = { selectedAspectRatio = ratio },
                                label = { Text(ratio) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("aspect_ratio_$ratio")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "أبعاد الفيديو:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberTextSecondary
                        )
                        FilterChip(
                            selected = selectedVideoRatio == "16:9",
                            onClick = { selectedVideoRatio = "16:9" },
                            label = { Text("16:9 أفقي") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("video_ratio_16_9")
                        )
                        FilterChip(
                            selected = selectedVideoRatio == "9:16",
                            onClick = { selectedVideoRatio = "9:16" },
                            label = { Text("9:16 عمودي") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("video_ratio_9_16")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_media_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uploadedBase64 != null) "تم إرفاق (${uploadedMime})" else "إرفاق صورة أو فيديو",
                                color = CyberTextPrimary
                            )
                        }
                        if (uploadedBase64 != null) {
                            OutlinedButton(
                                onClick = {
                                    onAnalyzeMedia(
                                        visualPrompt,
                                        uploadedBase64!!,
                                        uploadedMime ?: "image/jpeg",
                                        uploadedIsVideo
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("analyze_media_argon_button")
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحليل بـ Argon", color = CyberEmerald)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onGenerateImage(
                                    visualPrompt,
                                    useStudioProImage,
                                    selectedAspectRatio,
                                    selectedImageSize,
                                    uploadedBase64,
                                    uploadedMime
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uploadedBase64 != null && !uploadedIsVideo) "تعديل/توليد صورة" else "توليد صورة",
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "توليد الفيديو غير متاح حالياً. يمكنك استخدام تحليل الصورة أو الفيديو وقراءة وصف المشاهد.",
                            color = CyberTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (mediaAnalysisOutput != null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔍 نتيجة تحليل الوسائط عبر gemini-4-argon:",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mediaAnalysisOutput,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextPrimary
                        )
                    }
                }
            }
        }

        items(generatedAssets.filter { it.type == "IMAGE" || it.type == "VIDEO" }, key = { it.id }) { asset ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (asset.type == "VIDEO") "🎬 فيديو" else "🖼️ صورة"} • ${asset.modelUsed}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = asset.metadataLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val decodedBitmap = remember(asset.base64Data) {
                        if (asset.type == "IMAGE") AudioAndMediaHelper.decodeBase64ToBitmap(asset.base64Data) else null
                    }
                    if (decodedBitmap != null) {
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = asset.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = asset.textSummary ?: asset.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceAndMusicStudioSection(
    liveVoiceHistory: List<String>,
    lastTranscribedText: String,
    generatedAssets: List<GeneratedMediaAsset>,
    onSynthesizeSpeech: (String, String) -> Unit,
    onTranscribeAudio: (String, (String) -> Unit) -> Unit,
    onSendLiveVoiceTurn: (String, String?) -> Unit,
    onGenerateMusic: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ttsText by remember { mutableStateOf("") }
    var selectedVoice by remember { mutableStateOf("صوت النظام") }
    var liveVoicePrompt by remember { mutableStateOf("كيف أحسّن أداء استعلامات قاعدة البيانات في مشروعي؟") }
    var musicPrompt by remember { mutableStateOf("") }
    var useProFullTrack by remember { mutableStateOf(false) }
    var isRecordingMic by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                isRecordingMic = true
                val wavBase64 = AudioAndMediaHelper.recordShortWavBase64()
                isRecordingMic = false
                onTranscribeAudio(wavBase64) { transcribed ->
                    liveVoicePrompt = transcribed
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Turn-based Gemini Voice Conversation & Transcription
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeminiBorderGradient, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎙️ محادثة صوتية مع Gemini ونسخ الميكروفون",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = liveVoicePrompt,
                        onValueChange = { liveVoicePrompt = it },
                        label = { Text("رسالتك الصوتية أو النص المنسوخ") },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_voice_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    scope.launch {
                                        isRecordingMic = true
                                        val wavBase64 = AudioAndMediaHelper.recordShortWavBase64()
                                        isRecordingMic = false
                                        onTranscribeAudio(wavBase64) { transcribed ->
                                            liveVoicePrompt = transcribed
                                        }
                                    }
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("record_transcribe_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = CyberAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecordingMic) "جاري التسجيل..." else "تسجيل ونسخ",
                                color = CyberTextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                onSendLiveVoiceTurn(liveVoicePrompt, null)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("live_voice_send_button")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أرسل دوراً صوتياً إلى Gemini", color = CyberBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastTranscribedText.isNotBlank()) {
                        Text(
                            text = "📝 النص المنسوخ: $lastTranscribedText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberEmerald
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        liveVoiceHistory.takeLast(4).forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Local Android text-to-speech
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔊 تحويل النص إلى كلام (قارئ Android)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberViolet,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = ttsText,
                        onValueChange = { ttsText = it },
                        label = { Text("النص المراد نطقه") },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_text_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("صوت النظام").forEach { voice ->
                            FilterChip(
                                selected = selectedVoice == voice,
                                onClick = { selectedVoice = voice },
                                label = { Text(voice) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                    Button(
                        onClick = { if (ttsText.isNotBlank()) onSynthesizeSpeech(ttsText, selectedVoice) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeminiPillSelectedContainer,
                            contentColor = GeminiOnPillSelectedContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_tts_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد وتشغيل النطق الصوتي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Music Generation (Lyria 3)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎵 توليد الموسيقى (غير متاح حالياً)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = musicPrompt,
                        onValueChange = { musicPrompt = it },
                        label = { Text("وصف المقطع الموسيقي المطلوب") },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_prompt_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useProFullTrack,
                            onClick = { useProFullTrack = false },
                            label = { Text("مقطع قصير") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("music_model_clip")
                        )
                        FilterChip(
                            selected = useProFullTrack,
                            onClick = { useProFullTrack = true },
                            label = { Text("مسار كامل") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("music_model_pro")
                        )
                    }
                    Button(
                        onClick = { /* Music generation provider is not configured in this build. */ },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_music_button")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("غير متاح حالياً", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(generatedAssets.filter { it.type.startsWith("AUDIO") }, key = { it.id }) { asset ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🎧 ${asset.modelUsed} • ${asset.metadataLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = asset.textSummary ?: asset.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextSecondary
                    )
                }
            }
        }
    }
}
