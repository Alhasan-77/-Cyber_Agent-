package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberViolet
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
    onSpeakText: (String) -> Unit
) {
    // 0 = Multi-Turn Chatbot, 1 = Image & Video Studio, 2 = Voice, Audio & Music Lab
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("محادثة Gemini الذكية") },
                leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                modifier = Modifier.testTag("ai_subtab_chat")
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("استوديو الصور والفيديو") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                modifier = Modifier.testTag("ai_subtab_visual")
            )
            FilterChip(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                label = { Text("الصوت الحـي والموسيقى") },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                modifier = Modifier.testTag("ai_subtab_audio")
            )
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
                onSpeakText = onSpeakText
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
    onSpeakText: (String) -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "تخصيص دور المساعد والنموذج (Role System Instruction & Model Routing):",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AiRolePersona.entries.forEach { persona ->
                        FilterChip(
                            selected = selectedPersona == persona,
                            onClick = { onSelectPersona(persona) },
                            label = { Text(persona.titleAr) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        GeminiAiRepository.MODEL_ARGON to "Complex (gemini-4-argon)",
                        GeminiAiRepository.MODEL_FLASH to "General (gemini-3.8-flash)",
                        GeminiAiRepository.MODEL_FLASH_LITE to "Fast (gemini-3.1-flash-lite)"
                    ).forEach { (modelId, label) ->
                        FilterChip(
                            selected = selectedChatModel == modelId,
                            onClick = { onSelectChatModel(modelId) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("chat_model_$modelId")
                        )
                    }
                    FilterChip(
                        selected = useHighThinking,
                        onClick = { onToggleHighThinking(!useHighThinking) },
                        label = { Text("ThinkingLevel.HIGH") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        modifier = Modifier.testTag("toggle_high_thinking")
                    )
                    FilterChip(
                        selected = useSearchGrounding,
                        onClick = { onToggleSearch(!useSearchGrounding) },
                        label = { Text("Google Search") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        modifier = Modifier.testTag("toggle_search_grounding")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) CyberSurfaceElevated else CyberSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (msg.isUser) CyberViolet.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (msg.isUser) "👤 أنت" else "🤖 ${msg.modelUsed}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (msg.isUser) CyberAmber else CyberCyan
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = msg.timestampLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!msg.isUser) {
                                    IconButton(
                                        onClick = { onSpeakText(msg.text) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "استماع للرد عبر TTS",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (msg.groundingSources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🌐 مصادر Google Search Grounding:",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("اكتب سؤالك البرمجي أو المعماري...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chatbot_message_input")
            )
            Button(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        onSendChatMessage(inputPrompt)
                        inputPrompt = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("chatbot_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = CyberBackground)
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
        mutableStateOf("تصميم واجهة لوحة تحكم سحابية مستقبلية بألوان السيان والبنفسجي")
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎨 توليد وتعديل الصور والفيديو وتحليل الوسائط",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )

                    OutlinedTextField(
                        value = visualPrompt,
                        onValueChange = { visualPrompt = it },
                        label = { Text("وصف الصورة أو الفيديو أو سؤال التحليل (Prompt)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("visual_studio_prompt_input")
                    )

                    // Image Model Selection: gemini-nano-banana-2.1 vs gemini-3-pro-image-preview
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useStudioProImage,
                            onClick = { useStudioProImage = false },
                            label = { Text("gemini-nano-banana-2.1 (إنشاء وتعديل)") },
                            modifier = Modifier.testTag("model_nano_banana")
                        )
                        FilterChip(
                            selected = useStudioProImage,
                            onClick = { useStudioProImage = true },
                            label = { Text("gemini-3-pro-image-preview (جودة استوديو)") },
                            modifier = Modifier.testTag("model_pro_image")
                        )
                    }

                    // Image Size Selection (1K, 2K, 4K)
                    Text(
                        text = "حجم ودقة الصورة (Image Size - 1K / 2K / 4K):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        imageSizes.forEach { size ->
                            FilterChip(
                                selected = selectedImageSize == size,
                                onClick = { selectedImageSize = size },
                                label = { Text(size) },
                                modifier = Modifier.testTag("image_size_$size")
                            )
                        }
                    }

                    // Aspect Ratio Selection (1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 21:9)
                    Text(
                        text = "نسبة العرض إلى الارتفاع للصور (Aspect Ratio):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                modifier = Modifier.testTag("aspect_ratio_$ratio")
                            )
                        }
                    }

                    // Video Aspect Ratio (16:9 or 9:16) for gemini-omni-1.1-flash
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "أبعاد الفيديو (gemini-omni-1.1-flash):",
                            style = MaterialTheme.typography.labelMedium
                        )
                        FilterChip(
                            selected = selectedVideoRatio == "16:9",
                            onClick = { selectedVideoRatio = "16:9" },
                            label = { Text("16:9 أفقي") },
                            modifier = Modifier.testTag("video_ratio_16_9")
                        )
                        FilterChip(
                            selected = selectedVideoRatio == "9:16",
                            onClick = { selectedVideoRatio = "9:16" },
                            label = { Text("9:16 عمودي") },
                            modifier = Modifier.testTag("video_ratio_9_16")
                        )
                    }

                    // Upload Photo or Video via PickVisualMedia
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_media_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uploadedBase64 != null) "تم إرفاق ملف (${uploadedMime})" else "رفع صورة أو فيديو",
                                color = CyberCyan
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
                                modifier = Modifier.testTag("analyze_media_argon_button")
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CyberEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحليل بـ Argon", color = CyberEmerald)
                            }
                        }
                    }

                    // Action Buttons: Generate/Edit Image & Generate/Animate Video
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
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uploadedBase64 != null && !uploadedIsVideo) "تعديل/توليد صورة" else "توليد صورة",
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                onGenerateVideo(
                                    visualPrompt,
                                    selectedVideoRatio,
                                    if (!uploadedIsVideo) uploadedBase64 else null,
                                    if (!uploadedIsVideo) uploadedMime else null
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_video_button")
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uploadedBase64 != null && !uploadedIsVideo) "تحريك الصورة لفيديو" else "توليد فيديو"
                            )
                        }
                    }
                }
            }
        }

        if (mediaAnalysisOutput != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔍 نتيجة تحليل الوسائط عبر gemini-4-argon:",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mediaAnalysisOutput,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        items(generatedAssets.filter { it.type == "IMAGE" || it.type == "VIDEO" }, key = { it.id }) { asset ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (asset.type == "VIDEO") "🎬 فيديو" else "🖼️ صورة"} • ${asset.modelUsed}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan
                        )
                        Text(
                            text = asset.metadataLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val decodedBitmap = remember(asset.base64Data) {
                        if (asset.type == "IMAGE") AudioAndMediaHelper.decodeBase64ToBitmap(asset.base64Data) else null
                    }
                    if (decodedBitmap != null) {
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = asset.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = asset.textSummary ?: asset.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    var ttsText by remember { mutableStateOf("تم بناء تطبيقك المتكامل بنجاح وتفعيل قاعدة البيانات السحابية.") }
    var selectedVoice by remember { mutableStateOf("Kore") }
    var liveVoicePrompt by remember { mutableStateOf("كيف أحسّن أداء استعلامات قاعدة البيانات في مشروعي؟") }
    var musicPrompt by remember { mutableStateOf("موسيقى إلكترونية سينمائية ملهمة لبيئة تطوير مستقبلية") }
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
        // 1. Microphone Audio Transcription (gemini-3.5-transcribe) & Live API Voice (gemini-3.8-live)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎙️ المحادثة الصوتية الحية (gemini-3.8-live) ونسخ الصوت (gemini-3.5-transcribe)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )

                    OutlinedTextField(
                        value = liveVoicePrompt,
                        onValueChange = { liveVoicePrompt = it },
                        label = { Text("رسالتك الصوتية أو النص المنسوخ من الميكروفون") },
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("record_transcribe_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = CyberAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRecordingMic) "جاري التسجيل..." else "تسجيل ونسخ صوتي",
                                color = CyberAmber
                            )
                        }

                        Button(
                            onClick = {
                                onSendLiveVoiceTurn(liveVoicePrompt, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("live_voice_send_button")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحدث مع Live API", color = CyberBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastTranscribedText.isNotBlank()) {
                        Text(
                            text = "📝 آخر نص منسوخ (gemini-3.5-transcribe): $lastTranscribedText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberEmerald
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBackground)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        liveVoiceHistory.takeLast(4).forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Text-to-Speech (gemini-3.8-flash-tts)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔊 تحويل النص إلى كلام طبيعي (gemini-3.8-flash-tts)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberViolet
                    )
                    OutlinedTextField(
                        value = ttsText,
                        onValueChange = { ttsText = it },
                        label = { Text("النص المراد نطقه") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_text_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Kore", "Puck", "Aoede", "Charon").forEach { voice ->
                            FilterChip(
                                selected = selectedVoice == voice,
                                onClick = { selectedVoice = voice },
                                label = { Text(voice) }
                            )
                        }
                    }
                    Button(
                        onClick = { onSynthesizeSpeech(ttsText, selectedVoice) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_tts_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد وتشغيل النطق الصوتي (TTS)")
                    }
                }
            }
        }

        // 3. Music Generation (lyria-3-clip-preview & lyria-3-pro-preview)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎵 توليد الموسيقى والمؤثرات الصوتية (Lyria 3 Clip & Pro)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberEmerald
                    )
                    OutlinedTextField(
                        value = musicPrompt,
                        onValueChange = { musicPrompt = it },
                        label = { Text("وصف المقطع الموسيقي المطلوب") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_prompt_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useProFullTrack,
                            onClick = { useProFullTrack = false },
                            label = { Text("lyria-3-clip-preview (مقطع 30 ثانية)") },
                            modifier = Modifier.testTag("music_model_clip")
                        )
                        FilterChip(
                            selected = useProFullTrack,
                            onClick = { useProFullTrack = true },
                            label = { Text("lyria-3-pro-preview (مسار كامل)") },
                            modifier = Modifier.testTag("music_model_pro")
                        )
                    }
                    Button(
                        onClick = { onGenerateMusic(musicPrompt, useProFullTrack) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_music_button")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد وتشغيل الموسيقى الآن", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(generatedAssets.filter { it.type.startsWith("AUDIO") }, key = { it.id }) { asset ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🎧 ${asset.modelUsed} • ${asset.metadataLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = asset.textSummary ?: asset.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
