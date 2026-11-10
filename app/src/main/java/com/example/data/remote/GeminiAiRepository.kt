package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiRolePersona
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedMediaAsset
import com.example.ui.components.GlobalExceptionHandler
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.coroutines.resume

class GeminiAiRepository(
    @Suppress("UNUSED_PARAMETER")
    private val apiService: GeminiApiService = GeminiNetworkClient.service,
    private val openAiService: OpenAiApiService = OpenAiNetworkClient.service
) {
    companion object {
        private const val TAG = "GeminiAiRepository"
        private const val AI_TIMEOUT_MS = 60_000L

        const val MODEL_ARGON = "gemini-4-argon"
        const val MODEL_FLASH = "gemini-3.8-flash"
        const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite"
        const val MODEL_GPT_4O = "gpt-4o"
        const val MODEL_GPT_4O_MINI = "gpt-4o-mini"
        const val MODEL_DUAL_ENGINE = "gemini+chatgpt-dual"
        const val MODEL_NANO_BANANA = "gemini-nano-banana-2.1"
        const val MODEL_PRO_IMAGE = "gemini-3-pro-image"

        val SUPPORTED_TEXT_MODELS: List<Pair<String, String>> = listOf(
            MODEL_DUAL_ENGINE to "🤝 مزدوج: Gemini + ChatGPT معاً",
            MODEL_GPT_4O to "🟢 ChatGPT (gpt-4o)",
            MODEL_GPT_4O_MINI to "🟢 ChatGPT السريع (gpt-4o-mini)",
            MODEL_ARGON to "🔷 Gemini العميق (gemini-4-argon)",
            MODEL_FLASH to "🔷 Gemini المتوازن (gemini-3.8-flash)",
            MODEL_FLASH_LITE to "🔷 Gemini الفوري (gemini-3.5-flash-lite)"
        )

        fun isOpenAiModel(modelName: String): Boolean =
            modelName == MODEL_GPT_4O || modelName == MODEL_GPT_4O_MINI

        fun isDualEngineModel(modelName: String): Boolean =
            modelName == MODEL_DUAL_ENGINE

        fun normalizePreferredModel(modelName: String?): String {
            if (modelName.isNullOrBlank()) return MODEL_DUAL_ENGINE
            val validIds = SUPPORTED_TEXT_MODELS.map { it.first }.toSet()
            return if (modelName in validIds) modelName else MODEL_DUAL_ENGINE
        }

        fun verifyInitialization(): Result<Unit> = runCatching {
            // Verify FirebaseApp is ready while keeping Firebase.ai.generativeModel strictly lazy
            FirebaseApp.getInstance()
            Unit
        }
    }

    private fun isAppCheckTokenError(throwable: Throwable): Boolean {
        val fullMsg = buildString {
            var curr: Throwable? = throwable
            while (curr != null) {
                append(curr.message.orEmpty()).append(" ")
                curr = curr.cause
            }
        }
        return fullMsg.contains("App Check", ignoreCase = true) ||
            fullMsg.contains("AppCheck", ignoreCase = true) ||
            fullMsg.contains("token is invalid", ignoreCase = true)
    }

    private suspend fun forceRefreshAppCheckToken() {
        runCatching {
            val app = FirebaseApp.getInstance()
            val appContext = app.applicationContext
            val persistenceKey = app.persistenceKey
            // Clear any stale cached JWT token so FirebaseAppCheck mints a fresh one
            appContext.getSharedPreferences(
                "com.google.firebase.appcheck.store.$persistenceKey",
                Context.MODE_PRIVATE
            ).edit().clear().commit()

            suspendCancellableCoroutine { cont ->
                FirebaseAppCheck.getInstance().getAppCheckToken(true)
                    .addOnCompleteListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
            }
        }.onFailure { err ->
            Log.w(TAG, "App Check token force-refresh attempt warning", err)
        }
    }

    private suspend fun <T> executeWithAppCheckRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isAppCheckTokenError(e)) {
                Log.w(TAG, "App Check token rejected; refreshing token and retrying once...", e)
                forceRefreshAppCheckToken()
                block()
            } else {
                throw e
            }
        }
    }

    @Volatile
    private var activePreferredModel: String = MODEL_DUAL_ENGINE

    @Volatile
    private var injectedClient: AiTextGenerationClient = createClientForModel(MODEL_DUAL_ENGINE)

    fun setActiveModel(modelName: String) {
        val normalized = normalizePreferredModel(modelName)
        activePreferredModel = normalized
        injectedClient = createClientForModel(normalized)
    }

    fun getActiveModel(): String = activePreferredModel

    fun getActiveClient(): AiTextGenerationClient = injectedClient

    private fun createClientForModel(targetModel: String): AiTextGenerationClient {
        val provider = AiEngineProvider.fromModelId(targetModel)
        return object : AiTextGenerationClient {
            override val provider: AiEngineProvider = provider
            override val modelId: String = targetModel

            override suspend fun generateText(
                systemInstruction: String,
                userPrompt: String,
                useHighThinking: Boolean
            ): Result<String> = runCatching {
                when (provider) {
                    AiEngineProvider.CHATGPT -> {
                        val openAiModel = if (isOpenAiModel(targetModel)) targetModel else MODEL_GPT_4O
                        callOpenAiChat(
                            userPrompt = "$systemInstruction\n\n$userPrompt",
                            openAiModel = openAiModel,
                            useHighThinking = useHighThinking
                        ).getOrThrow()
                    }
                    AiEngineProvider.DUAL_ENGINE -> {
                        val (reply, _) = sendDualEngineMultiTurnChat(
                            history = emptyList(),
                            userPrompt = userPrompt,
                            persona = AiRolePersona.FULLSTACK_ARCHITECT,
                            useSearchGrounding = false,
                            useHighThinking = useHighThinking
                        )
                        reply
                    }
                    AiEngineProvider.GEMINI -> {
                        val geminiModel = resolveTextModel(targetModel)
                        withTimeout(AI_TIMEOUT_MS) {
                            executeWithAppCheckRetry {
                                geminiModel.generateContent("$systemInstruction\n\n$userPrompt").text
                            } ?: throw IllegalStateException("لم يُرجع مزود Gemini نصاً.")
                        }
                    }
                }
            }
        }
    }

    // Lazy model initialization so FirebaseApp and FirebaseAppCheck are configured first
    private val agentFunctionDeclarations: List<FunctionDeclaration> by lazy {
        listOf(
            FunctionDeclaration(
                "save_current_project",
                "Save the current editor files to the signed-in user's Firestore project. Use when the user asks to save or persist the current project.",
                emptyMap()
            ),
            FunctionDeclaration(
                "create_version_snapshot",
                "Create an immutable Firestore version snapshot of the current editor files. Use when the user asks to create a version, checkpoint, or snapshot.",
                mapOf(
                    "commit_message" to Schema.string("A short, meaningful description for this version snapshot.")
                )
            ),
            FunctionDeclaration(
                "review_current_code",
                "Ask the security auditor AI to review the current HTML, CSS, JavaScript, and backend code. This is a code review only, not a dynamic scan or executed test.",
                emptyMap()
            ),
            FunctionDeclaration(
                "edit_active_code",
                "Generate a code change for the active editor tab, apply it to the editor, and save the project. Only use when the user explicitly asks to change or implement something.",
                mapOf(
                    "instruction" to Schema.string("The exact requested code change, including requirements and constraints.")
                )
            ),
            FunctionDeclaration(
                "get_workspace_status",
                "Read the current project name, active editor tab, selected project state, and code lengths. Use to answer questions about the current workspace.",
                emptyMap()
            )
        )
    }

    private fun createAgentModel(preferredModel: String): GenerativeModel {
        val modelName = when {
            isDualEngineModel(preferredModel) || isOpenAiModel(preferredModel) -> MODEL_FLASH
            else -> normalizePreferredModel(preferredModel)
        }
        return Firebase.ai.generativeModel(
            modelName = modelName,
            tools = listOf(Tool.functionDeclarations(agentFunctionDeclarations))
        )
    }

    suspend fun reviseCodeFile(
        currentCode: String,
        activeTab: String,
        instruction: String,
        preferredModel: String
    ): String = withContext(Dispatchers.IO) {
        require(instruction.isNotBlank()) { "اكتب التعديل المطلوب." }
        val model = when (preferredModel) {
            MODEL_ARGON -> argonModel
            MODEL_FLASH_LITE -> flashLiteModel
            else -> flashModel
        }
        val prompt = """
            You are editing an existing source file in the Cyber Agent IDE.
            Language or file type: $activeTab
            User change request: $instruction

            Return the COMPLETE updated file, not a patch and not a snippet.
            Preserve all existing behavior unrelated to the requested change.
            Keep imports, declarations, and required surrounding code.
            Do not add markdown fences, explanations, or placeholder implementations.
            Do not claim the code was compiled or tested.
            If the request is ambiguous or cannot be safely implemented from the supplied file,
            return the original file unchanged.

            Current complete file:
            $currentCode
        """.trimIndent()
        val response = withTimeout(AI_TIMEOUT_MS) {
            executeWithAppCheckRetry { model.generateContent(prompt).text }
        }
        val revised = response?.trim().orEmpty()
        require(revised.isNotBlank()) { "لم يُرجع النموذج ملفاً معدلاً." }
        revised.removeSurrounding("```$activeTab\n", "\n```").trim()
    }
    suspend fun sendAgentTurn(
        history: List<ChatMessage>,
        userPrompt: String,
        preferredModel: String,
        systemInstruction: String,
        executeTool: suspend (String, Map<String, JsonElement>) -> JsonObject
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        require(userPrompt.isNotBlank()) { "أدخل طلباً للوكيل أولاً." }
        val conversationContext = history.takeLast(8).joinToString("\n") { message ->
            val role = if (message.isUser) "User" else "Assistant"
            "$role: ${message.text.take(4000)}"
        }
        val agentPrompt = buildString {
            appendLine("You are Cyber Agent, an in-app task execution assistant.")
            appendLine(systemInstruction)
            appendLine("Use the declared tools whenever the user asks you to inspect or change the current workspace.")
            appendLine("Never claim a task was completed unless a tool returned ok=true.")
            appendLine("Never claim code was compiled, deployed, or security-scanned unless an actual tool did that.")
            appendLine("For destructive or unavailable actions, explain the limitation and do not invent success.")
            if (conversationContext.isNotBlank()) {
                appendLine("Recent conversation:")
                appendLine(conversationContext)
            }
            appendLine("Current user request:")
            appendLine(userPrompt)
        }

        val chat = createAgentModel(preferredModel).startChat()
        var response = withTimeout(AI_TIMEOUT_MS) { chat.sendMessage(agentPrompt) }
        val executedTools = mutableListOf<String>()
        var rounds = 0

        while (response.functionCalls.isNotEmpty()) {
            if (rounds >= 4) {
                throw IllegalStateException("توقف تنفيذ الوكيل بعد أربع جولات أدوات لحماية التطبيق من حلقات التنفيذ.")
            }
            val functionResponses = response.functionCalls.map { call ->
                val result = try {
                    executeTool(call.name, call.args)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    JsonObject(
                        mapOf(
                            "ok" to JsonPrimitive(false),
                            "error" to JsonPrimitive(error.localizedMessage ?: "Tool execution failed")
                        )
                    )
                }
                if (result["ok"] == JsonPrimitive(true)) {
                    executedTools += call.name
                }
                FunctionResponsePart(call.name, result)
            }
            response = withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessage(content("user") {
                    functionResponses.forEach { functionResponse -> part(functionResponse) }
                })
            }
            rounds++
        }

        val finalText = response.text?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("لم يُرجع وكيل Gemini استجابة نهائية.")
        finalText to executedTools.distinct()
    }

    private val flashModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH,
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    private val flashLiteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FLASH_LITE,
            generationConfig = generationConfig {
                temperature = 0.3f
            }
        )
    }

    private val argonModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_ARGON,
            generationConfig = generationConfig {
                temperature = 0.6f
            }
        )
    }

    private val nanoBananaImageModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NANO_BANANA,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val proImageModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_PRO_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun resolveTextModel(modelName: String): GenerativeModel {
        return when (modelName) {
            MODEL_ARGON -> argonModel
            MODEL_FLASH_LITE -> flashLiteModel
            MODEL_FLASH -> flashModel
            else -> Firebase.ai.generativeModel(modelName)
        }
    }

    suspend fun callOpenAiChat(
        history: List<ChatMessage> = emptyList(),
        userPrompt: String,
        openAiModel: String = MODEL_GPT_4O,
        persona: AiRolePersona = AiRolePersona.FULLSTACK_ARCHITECT,
        useHighThinking: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!OpenAiNetworkClient.isApiKeyConfigured()) {
                throw IllegalStateException(
                    "مفتاح OPENAI_API_KEY غير مضبوط بعد في لوحة الأسرار (Secrets Panel) في AI Studio. " +
                        "أضف المفتاح باسم OPENAI_API_KEY في لوحة Secrets ليتم حقنه تلقائياً عبر BuildConfig.OPENAI_API_KEY."
                )
            }

            val systemContent = buildString {
                appendLine("[System Role: ${persona.titleAr}]")
                appendLine(persona.systemPrompt)
                if (useHighThinking) {
                    appendLine("[Mode: Deep Architectural & Algorithmic Reasoning]")
                }
            }.trim()

            val messages = buildList {
                add(OpenAiChatMessage(role = "system", content = systemContent))
                history.takeLast(8).forEach { msg ->
                    add(
                        OpenAiChatMessage(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        )
                    )
                }
                add(OpenAiChatMessage(role = "user", content = userPrompt))
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                openAiService.createChatCompletion(
                    request = OpenAiChatCompletionRequest(
                        model = openAiModel,
                        messages = messages,
                        temperature = if (useHighThinking) 0.4f else 0.7f
                    )
                )
            }

            val errorMsg = response.error?.message
            if (!errorMsg.isNullOrBlank()) {
                throw IllegalStateException("خطأ من خادم OpenAI: $errorMsg")
            }

            val reply = response.choices.firstOrNull()?.message?.content?.trim()
            if (reply.isNullOrBlank()) {
                throw IllegalStateException("لم يُرجع نموذج ChatGPT ($openAiModel) نصاً في الاستجابة.")
            }
            reply
        }
    }

    // 1. Multi-turn Chatbot supporting Gemini (Firebase AI Logic), ChatGPT (OpenAI API),
    // and Side-by-Side Dual Engine (Gemini + ChatGPT simultaneously)
    suspend fun sendMultiTurnChat(
        history: List<ChatMessage>,
        userPrompt: String,
        selectedModel: String,
        persona: AiRolePersona,
        useSearchGrounding: Boolean,
        useHighThinking: Boolean
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        if (isDualEngineModel(selectedModel)) {
            return@withContext sendDualEngineMultiTurnChat(
                history = history,
                userPrompt = userPrompt,
                persona = persona,
                useSearchGrounding = useSearchGrounding,
                useHighThinking = useHighThinking
            )
        }

        if (isOpenAiModel(selectedModel)) {
            return@withContext sendOpenAiMultiTurnChatWithFallback(
                history = history,
                userPrompt = userPrompt,
                openAiModel = selectedModel,
                persona = persona,
                useSearchGrounding = useSearchGrounding,
                useHighThinking = useHighThinking
            )
        }

        sendGeminiSingleEngineChat(
            history = history,
            userPrompt = userPrompt,
            selectedModel = selectedModel,
            persona = persona,
            useSearchGrounding = useSearchGrounding,
            useHighThinking = useHighThinking
        )
    }

    private suspend fun sendDualEngineMultiTurnChat(
        history: List<ChatMessage>,
        userPrompt: String,
        persona: AiRolePersona,
        useSearchGrounding: Boolean,
        useHighThinking: Boolean
    ): Pair<String, List<String>> = coroutineScope {
        val geminiModel = if (useHighThinking) MODEL_ARGON else MODEL_FLASH
        val openAiModel = MODEL_GPT_4O

        val geminiDeferred = async {
            sendGeminiSingleEngineChat(
                history = history,
                userPrompt = userPrompt,
                selectedModel = geminiModel,
                persona = persona,
                useSearchGrounding = useSearchGrounding,
                useHighThinking = useHighThinking
            )
        }

        val openAiDeferred = async {
            callOpenAiChat(
                history = history,
                userPrompt = userPrompt,
                openAiModel = openAiModel,
                persona = persona,
                useHighThinking = useHighThinking
            )
        }

        val (geminiText, geminiSources) = geminiDeferred.await()
        val openAiResult = openAiDeferred.await()

        val combinedText = buildString {
            appendLine("🤝 **وضع المحرك المزدوج المتزامن (Gemini API + ChatGPT API)**")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔷 **[1] استجابة محرك Gemini ($geminiModel — Firebase AI Logic):**")
            appendLine(geminiText)
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🟢 **[2] استجابة محرك ChatGPT ($openAiModel — OpenAI API):**")
            openAiResult.onSuccess { gptReply ->
                appendLine(gptReply)
            }.onFailure { err ->
                appendLine("⚠️ تعذر إكمال اتصال ChatGPT المباشر (${err.localizedMessage}).")
                appendLine("💡 للتفعيل الفوري: افتح **لوحة الأسرار (Secrets Panel)** في AI Studio وأضف مفتاحك تحت الاسم `OPENAI_API_KEY` ليتم قراءته عبر `BuildConfig.OPENAI_API_KEY`.")
            }
        }.trim()

        val sources = buildList {
            addAll(geminiSources)
            add("Firebase AI Logic ($geminiModel)")
            if (openAiResult.isSuccess) {
                add("OpenAI Chat Completions API ($openAiModel)")
            } else {
                add("OpenAI API Status: ${OpenAiNetworkClient.getKeyStatusSummaryAr()}")
            }
        }.distinct()

        Pair(combinedText, sources)
    }

    private suspend fun sendOpenAiMultiTurnChatWithFallback(
        history: List<ChatMessage>,
        userPrompt: String,
        openAiModel: String,
        persona: AiRolePersona,
        useSearchGrounding: Boolean,
        useHighThinking: Boolean
    ): Pair<String, List<String>> {
        val openAiResult = callOpenAiChat(
            history = history,
            userPrompt = userPrompt,
            openAiModel = openAiModel,
            persona = persona,
            useHighThinking = useHighThinking
        )

        openAiResult.onSuccess { reply ->
            return Pair(
                reply,
                listOf("OpenAI ChatGPT API ($openAiModel • BuildConfig.OPENAI_API_KEY)")
            )
        }

        val error = openAiResult.exceptionOrNull()
        if (error != null && OpenAiNetworkClient.isApiKeyConfigured()) {
            GlobalExceptionHandler.reportError(
                phase = "OpenAI Chat ($openAiModel)",
                throwable = error,
                isFatal = false,
                serviceName = "OpenAI / ChatGPT"
            )
        }

        // Seamless fallback to Gemini alongside clear instructions for OPENAI_API_KEY
        val (geminiBackup, geminiSources) = sendGeminiSingleEngineChat(
            history = history,
            userPrompt = userPrompt,
            selectedModel = if (useHighThinking) MODEL_ARGON else MODEL_FLASH,
            persona = persona,
            useSearchGrounding = useSearchGrounding,
            useHighThinking = useHighThinking
        )

        val combined = buildString {
            appendLine("ℹ️ **ملاحظة محرك ChatGPT ($openAiModel):** ${error?.localizedMessage ?: "تعذر الاتصال بـ OpenAI API"}")
            if (!OpenAiNetworkClient.isApiKeyConfigured()) {
                appendLine("🔑 **طريقة ربط مفتاح ChatGPT بأمان:** افتح لوحة **Secrets** في AI Studio وأدخل مفتاحك في الحقل `OPENAI_API_KEY` (يُقرأ تلقائياً عبر `BuildConfig.OPENAI_API_KEY`).")
            }
            appendLine()
            appendLine("🔄 **الاستجابة المساندة الفورية عبر محرك Gemini المدمج:**")
            appendLine(geminiBackup)
        }.trim()

        return Pair(
            combined,
            (geminiSources + listOf("OpenAI Fallback → Gemini (${if (useHighThinking) MODEL_ARGON else MODEL_FLASH})")).distinct()
        )
    }

    private suspend fun sendGeminiSingleEngineChat(
        history: List<ChatMessage>,
        userPrompt: String,
        selectedModel: String,
        persona: AiRolePersona,
        useSearchGrounding: Boolean,
        useHighThinking: Boolean
    ): Pair<String, List<String>> {
        val effectiveModel = when {
            useHighThinking -> MODEL_ARGON
            useSearchGrounding -> MODEL_FLASH
            else -> selectedModel
        }

        return try {
            val chatHistory = history.takeLast(10).map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

            val fullUserPrompt = buildString {
                appendLine("[System Role: ${persona.titleAr}]")
                appendLine(persona.systemPrompt)
                if (useHighThinking) {
                    appendLine("[Mode: Deep Architectural Reasoning]")
                }
                appendLine()
                append(userPrompt)
            }

            val model = resolveTextModel(effectiveModel)
            val responseText = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    val chat = model.startChat(history = chatHistory)
                    val response = chat.sendMessage(fullUserPrompt)
                    response.text
                }
            }

            val text = responseText?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("لم يُرجع نموذج Gemini أي محتوى.");
            val sources = if (useSearchGrounding) {
                listOf("Firebase AI Logic ($effectiveModel)")
            } else {
                emptyList()
            }
            Pair(text, sources)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat timed out ($effectiveModel)", e)
            GlobalExceptionHandler.reportError(
                phase = "Gemini Chat Timeout ($effectiveModel)",
                throwable = e,
                isFatal = false,
                serviceName = "Gemini"
            )
            throw IllegalStateException("انتهت مهلة استجابة Gemini. لم يتم إنشاء رد بديل.", e)
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quota exceeded ($effectiveModel)", e)
            GlobalExceptionHandler.reportError(
                phase = "Gemini Chat Quota ($effectiveModel)",
                throwable = e,
                isFatal = false,
                serviceName = "Gemini"
            )
            throw IllegalStateException("تم تجاوز حصة Gemini المتاحة. لم يتم إنشاء رد بديل.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Chat error ($effectiveModel)", e)
            if (OpenAiNetworkClient.isApiKeyConfigured()) {
                val backupGpt = callOpenAiChat(
                    history = history,
                    userPrompt = userPrompt,
                    openAiModel = if (useHighThinking) MODEL_GPT_4O else MODEL_GPT_4O_MINI,
                    persona = persona,
                    useHighThinking = useHighThinking
                ).getOrNull()
                if (!backupGpt.isNullOrBlank()) {
                    return Pair(
                        backupGpt,
                        listOf("Auto-Failover Engine (${if (useHighThinking) MODEL_GPT_4O else MODEL_GPT_4O_MINI})")
                    )
                }
            }
            if (!isAppCheckTokenError(e)) {
                GlobalExceptionHandler.reportError(
                    phase = "Gemini Chat ($effectiveModel)",
                    throwable = e,
                    isFatal = false,
                    serviceName = "Gemini"
                )
            }
            throw IllegalStateException("تعذر الحصول على استجابة من Gemini أو مزود النسخ الاحتياطي.", e)
        }
    }

    // Streaming variant using generateContentStream for real-time incremental UI updates
    suspend fun streamMultiTurnChat(
        userPrompt: String,
        selectedModel: String = MODEL_FLASH,
        persona: AiRolePersona = AiRolePersona.FULLSTACK_ARCHITECT,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val model = resolveTextModel(selectedModel)
        val fullPrompt = "${persona.systemPrompt}\n\n$userPrompt"
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    chunk.text?.let { part ->
                        accumulated.append(part)
                        onChunk(accumulated.toString())
                    }
                }
            }
            accumulated.toString().also { result ->
                if (result.isBlank()) {
                    throw IllegalStateException("لم يُرجع بث Gemini أي محتوى.", null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GlobalExceptionHandler.reportError(
                phase = "Gemini Stream ($selectedModel)",
                throwable = e,
                isFatal = false,
                serviceName = "Gemini"
            )
            if (accumulated.isNotBlank()) {
                accumulated.toString()
            } else {
                throw IllegalStateException("تعذر الحصول على محتوى من بث Gemini.", e)
            }
        }
    }

    // 2. Low-Latency Inline Code Autocomplete & Refactoring (Gemini 3.5 Flash-Lite + ChatGPT gpt-4o-mini)
    suspend fun generateLowLatencyCodeSuggestion(
        currentCode: String,
        activeTab: String,
        instruction: String,
        preferredModel: String = MODEL_FLASH_LITE
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            Language/Layer: $activeTab
            Instruction: $instruction
            Current Code:
            $currentCode
            Provide a concise, high-performance code completion or enhancement snippet ready to insert.
        """.trimIndent()

        if (isOpenAiModel(preferredModel) || (isDualEngineModel(preferredModel) && OpenAiNetworkClient.isApiKeyConfigured())) {
            val openAiRes = callOpenAiChat(
                userPrompt = prompt,
                openAiModel = MODEL_GPT_4O_MINI,
                persona = AiRolePersona.FULLSTACK_ARCHITECT,
                useHighThinking = false
            )
            val gptText = openAiRes.getOrNull()
            if (!gptText.isNullOrBlank() && !isDualEngineModel(preferredModel)) {
                return@withContext gptText
            }
            if (!gptText.isNullOrBlank() && isDualEngineModel(preferredModel)) {
                val geminiText = runCatching {
                    withTimeout(AI_TIMEOUT_MS) {
                        executeWithAppCheckRetry { flashLiteModel.generateContent(prompt).text }
                    }
                }.getOrNull()
                return@withContext buildString {
                    if (!geminiText.isNullOrBlank()) {
                        appendLine("// --- [Gemini $MODEL_FLASH_LITE Suggestion] ---")
                        appendLine(geminiText)
                        appendLine()
                    }
                    appendLine("// --- [ChatGPT $MODEL_GPT_4O_MINI Suggestion] ---")
                    appendLine(gptText)
                }.trim()
            }
        }

        try {
            val text = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    val response = flashLiteModel.generateContent(prompt)
                    response.text
                }
            }
            if (!text.isNullOrBlank()) return@withContext text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isAppCheckTokenError(e)) {
                GlobalExceptionHandler.reportError(
                    phase = "Gemini Code Autocomplete ($MODEL_FLASH_LITE)",
                    throwable = e,
                    isFatal = false,
                    serviceName = "Gemini"
                )
            }
        }

        throw IllegalStateException("لم يتمكن نموذج الاقتراح البرمجي من إنشاء نتيجة. لم تتم إضافة كود تجريبي.")
    }


    // 3. Full-Stack Architecture Generator with High Thinking (Gemini 4 Argon + ChatGPT gpt-4o)
    suspend fun generateFullStackCodebase(
        projectBrief: String,
        stackLanguage: String,
        preferredModel: String = MODEL_ARGON
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val prompt = """
            Build a complete interactive Full-Stack web preview (HTML, CSS, JS) and Backend code ($stackLanguage) for: $projectBrief.
            Return fenced code blocks labelled html, css, javascript, and $stackLanguage. Do not claim any service is deployed or connected. Do not invent runtime metrics or backend status.
        """.trimIndent()

        if (isOpenAiModel(preferredModel) && OpenAiNetworkClient.isApiKeyConfigured()) {
            val gptOut = callOpenAiChat(
                userPrompt = prompt,
                openAiModel = MODEL_GPT_4O,
                persona = AiRolePersona.FULLSTACK_ARCHITECT,
                useHighThinking = true
            ).getOrNull()
            if (!gptOut.isNullOrBlank()) {
                return@withContext buildGeneratedStackFromAiText(projectBrief, stackLanguage, gptOut)
            }
        }

        try {
            val out = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    val response = argonModel.generateContent(prompt)
                    response.text
                }
            }
            val openAiSupplement = if (OpenAiNetworkClient.isApiKeyConfigured()) {
                callOpenAiChat(
                    userPrompt = "Provide a concise production-ready backend controller ($stackLanguage) for: $projectBrief",
                    openAiModel = MODEL_GPT_4O_MINI,
                    persona = AiRolePersona.FULLSTACK_ARCHITECT,
                    useHighThinking = true
                ).getOrNull()
            } else {
                null
            }
            val mergedOut = listOfNotNull(out, openAiSupplement).joinToString("\n\n// --- [OpenAI ChatGPT Co-Architect] ---\n")
            if (mergedOut.isNotBlank()) {
                return@withContext buildGeneratedStackFromAiText(projectBrief, stackLanguage, mergedOut)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isAppCheckTokenError(e)) {
                GlobalExceptionHandler.reportError(
                    phase = "Gemini Full-Stack Generator ($MODEL_ARGON)",
                    throwable = e,
                    isFatal = false,
                    serviceName = "Gemini"
                )
            }
        }
        throw IllegalStateException("تعذر توليد المشروع من مزود الذكاء الاصطناعي. لم يتم إنشاء مشروع بديل وهمي.")
    }

    // 4. Image Creation & Editing (gemini-nano-banana-2.1 and gemini-3-pro-image) via Firebase AI Logic
    suspend fun generateOrEditImage(
        prompt: String,
        useStudioProModel: Boolean,
        aspectRatio: String,
        imageSize: String,
        sourceImageBase64: String? = null,
        sourceMimeType: String? = null
    ): GeneratedMediaAsset = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "اكتب وصفاً للصورة أولاً." }
        val modelName = if (useStudioProModel) MODEL_PRO_IMAGE else MODEL_NANO_BANANA
        val imageModel = if (useStudioProModel) proImageModel else nanoBananaImageModel
        try {
            val inputBitmap = decodeBase64Bitmap(sourceImageBase64)
            val response = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    if (inputBitmap != null) {
                        imageModel.generateContent(
                            content {
                                image(inputBitmap)
                                text(prompt)
                            }
                        )
                    } else {
                        imageModel.generateContent(prompt)
                    }
                }
            }
            val outputBitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
                ?: throw IllegalStateException("لم يُرجع النموذج بيانات صورة فعلية.")
            GeneratedMediaAsset(
                id = UUID.randomUUID().toString(),
                type = "IMAGE",
                prompt = prompt,
                modelUsed = modelName,
                metadataLabel = "الأبعاد المطلوبة: $aspectRatio | الدقة المطلوبة: $imageSize",
                base64Data = encodeBitmapToBase64Png(outputBitmap),
                mimeType = "image/png",
                textSummary = response.text ?: "تم استلام صورة من مزود الذكاء الاصطناعي."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GlobalExceptionHandler.reportError(
                phase = "Image Generation ($modelName)",
                throwable = e,
                isFatal = false,
                serviceName = "Gemini"
            )
            throw IllegalStateException("تعذر توليد الصورة: ${e.localizedMessage ?: "تحقق من إعدادات Firebase AI والحصة المتاحة."}", e)
        }
    }

    // 5. Video Storyboard & Animation Specification via Firebase AI Logic
    suspend fun generateOrAnimateVideo(
        prompt: String,
        aspectRatio: String,
        sourcePhotoBase64: String? = null,
        sourcePhotoMime: String? = null
    ): GeneratedMediaAsset = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(
            "توليد الفيديو غير مهيأ حالياً. لا ينتج هذا الإصدار ملف MP4؛ استخدم وصف المشاهد النصي فقط بعد تفعيل مزود فيديو فعلي."
        )
    }

    // 6. Analyze Images & Video Content using gemini-4-argon via Firebase AI Logic
    suspend fun analyzeVisualMediaWithArgon(
        prompt: String,
        mediaBase64: String,
        mimeType: String,
        isVideo: Boolean
    ): String = withContext(Dispatchers.IO) {
        val mediaTypeAr = if (isVideo) "الفيديو" else "الصورة"
        require(prompt.isNotBlank()) { "اكتب وصفاً للتحليل أولاً." }
        require(mediaBase64.isNotBlank()) { "لم يتم إرفاق ملف وسائط للتحليل." }
        require(
            mimeType.startsWith(if (isVideo) "video/" else "image/", ignoreCase = true)
        ) { "نوع الملف لا يتوافق مع نوع الوسائط المحدد." }

        try {
            val cleanBase64 = mediaBase64.substringAfter("base64,", mediaBase64)
            val rawBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            require(rawBytes.isNotEmpty()) { "ملف الوسائط فارغ." }
            val bitmap = if (!isVideo) decodeBase64Bitmap(cleanBase64) else null

            val responseText = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    if (bitmap != null) {
                        argonModel.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        ).text
                    } else {
                        argonModel.generateContent(
                            content {
                                inlineData(rawBytes, mimeType)
                                text(prompt)
                            }
                        ).text
                    }
                }
            }
            if (!responseText.isNullOrBlank()) return@withContext responseText
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isAppCheckTokenError(e)) {
                GlobalExceptionHandler.reportError(
                    phase = "Gemini Media Analysis ($MODEL_ARGON)",
                    throwable = e,
                    isFatal = false,
                    serviceName = "Gemini"
                )
            }
            throw IllegalStateException("تعذر تحليل $mediaTypeAr عبر Gemini.", e)
        }

        throw IllegalStateException("لم يُرجع النموذج نتيجة لتحليل الوسائط.")

    }

    // 8. Transcribe Microphone Audio using Firebase AI Logic multimodal inlineData
    suspend fun transcribeMicrophoneAudio(
        audioBase64: String,
        mimeType: String = "audio/wav"
    ): String = withContext(Dispatchers.IO) {
        require(audioBase64.isNotBlank()) { "لم يتم توفير تسجيل صوتي." }
        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
        require(audioBytes.isNotEmpty()) { "ملف التسجيل الصوتي فارغ." }
        val transcription = withTimeout(AI_TIMEOUT_MS) {
            executeWithAppCheckRetry {
                flashModel.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text("Transcribe this audio recording accurately in Arabic or English. Return only the transcription:")
                    }
                ).text
            }
        }
        transcription?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("لم يُرجع نموذج Gemini نصاً للتسجيل.")
    }

    // 9. Live Voice Conversation using Firebase AI Logic
    suspend fun sendLiveVoiceConversationTurn(
        spokenPrompt: String,
        audioBase64: String? = null
    ): String = withContext(Dispatchers.IO) {
        require(spokenPrompt.isNotBlank() || !audioBase64.isNullOrBlank()) {
            "أدخل نصاً أو أرفق تسجيلاً صوتياً."
        }
        val audioBytes = audioBase64?.takeIf { it.isNotBlank() }?.let {
            Base64.decode(it, Base64.DEFAULT)
        }
        val reply = withTimeout(AI_TIMEOUT_MS) {
            executeWithAppCheckRetry {
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    flashModel.generateContent(
                        content {
                            inlineData(audioBytes, "audio/wav")
                            text("Respond naturally and concisely in Arabic as a coding assistant. User context: $spokenPrompt")
                        }
                    ).text
                } else {
                    flashModel.generateContent(
                        "Respond naturally and concisely in Arabic as a coding assistant: $spokenPrompt"
                    ).text
                }
            }
        }
        reply?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("لم يُرجع نموذج Gemini استجابة.")
    }

    // 10. Generate Music Composition & Track Metadata
    suspend fun generateMusic(
        prompt: String,
        useFullLengthPro: Boolean
    ): GeneratedMediaAsset = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(
            "توليد الموسيقى غير متاح في هذا الإصدار؛ لم يتم إعداد نقطة API فعلية لإنتاج الملفات الصوتية."
        )
    }

    // 11. Bioscience Skills Engine (Sequence Analysis + Gemini 4 Argon + ChatGPT gpt-4o)
    suspend fun analyzeBioscienceSequence(
        fastaOrSequence: String,
        researchGoal: String,
        preferredModel: String = MODEL_ARGON
    ): String = withContext(Dispatchers.IO) {
        val cleanedSeq = fastaOrSequence.lines()
            .filterNot { it.trim().startsWith(">") }
            .joinToString("") { it.trim().uppercase() }
            .filter { it.isLetter() }

        val length = cleanedSeq.length
        val gCount = cleanedSeq.count { it == 'G' }
        val cCount = cleanedSeq.count { it == 'C' }
        val aCount = cleanedSeq.count { it == 'A' }
        val tOrUCount = cleanedSeq.count { it == 'T' || it == 'U' }
        val isNucleotide = length > 0 && (gCount + cCount + aCount + tOrUCount).toDouble() / length >= 0.85

        val gcPercent = if (length > 0) ((gCount + cCount) * 100.0 / length) else 0.0
        val approxMolWeightDa = if (isNucleotide) length * 330.0 else length * 110.0

        val deterministicHeader = buildString {
            appendLine("🧬 نتائج التحليل البيولوجي الحاسوبي (Bioscience Skills Lab):")
            appendLine("• نوع التسلسل المكتشف: ${if (isNucleotide) "حمض نووي (DNA/RNA)" else "تسلسل بروتيني (Amino Acid Sequence)"}")
            appendLine("• طول السلسلة: $length وحدة بنائية")
            if (isNucleotide) {
                appendLine("• نسبة GC Content: ${"%.2f".format(gcPercent)}% (A=$aCount, T/U=$tOrUCount, G=$gCount, C=$cCount)")
            }
            appendLine("• الوزن الجزيئي التقريبي: ${"%.1f".format(approxMolWeightDa / 1000.0)} kDa")
        }

        val prompt = """
            You are a Senior Bioinformatics & Computational Biology AI Scientist.
            Analyze the following biological sequence and research goal:
            Sequence metrics:
            $deterministicHeader
            Raw Sequence:
            ${cleanedSeq.take(1500)}
            Research Goal:
            $researchGoal
            Provide functional domain insights, structural folding predictions, and a complete Python (Biopython) analysis script in Arabic.
        """.trimIndent()

        if (isOpenAiModel(preferredModel) && OpenAiNetworkClient.isApiKeyConfigured()) {
            val gptBio = callOpenAiChat(
                userPrompt = prompt,
                openAiModel = MODEL_GPT_4O,
                persona = AiRolePersona.BIOSCIENCE_SPECIALIST,
                useHighThinking = true
            ).getOrNull()
            if (!gptBio.isNullOrBlank()) {
                return@withContext "$deterministicHeader\n\n🟢 **[تحليل ChatGPT ($MODEL_GPT_4O)]:**\n$gptBio"
            }
        }

        try {
            val aiAnalysis = withTimeout(AI_TIMEOUT_MS) {
                executeWithAppCheckRetry {
                    argonModel.generateContent(prompt).text
                }
            }
            val openAiBioNote = if (OpenAiNetworkClient.isApiKeyConfigured()) {
                callOpenAiChat(
                    userPrompt = "Provide a concise clinical & structural validation summary in Arabic for this sequence and goal: $researchGoal (Length=$length)",
                    openAiModel = MODEL_GPT_4O_MINI,
                    persona = AiRolePersona.BIOSCIENCE_SPECIALIST,
                    useHighThinking = false
                ).getOrNull()
            } else {
                null
            }
            if (!aiAnalysis.isNullOrBlank()) {
                val combinedBio = buildString {
                    appendLine(deterministicHeader)
                    appendLine()
                    appendLine("🔷 **[تحليل Gemini ($MODEL_ARGON)]:**")
                    appendLine(aiAnalysis)
                    if (!openAiBioNote.isNullOrBlank()) {
                        appendLine()
                        appendLine("🟢 **[المراجعة التكميلية عبر ChatGPT ($MODEL_GPT_4O_MINI)]:**")
                        appendLine(openAiBioNote)
                    }
                }.trim()
                return@withContext combinedBio
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isAppCheckTokenError(e)) {
                GlobalExceptionHandler.reportError(
                    phase = "Gemini Bioscience Analysis ($MODEL_ARGON)",
                    throwable = e,
                    isFatal = false,
                    serviceName = "Gemini"
                )
            }
        }

        throw IllegalStateException("تعذر الحصول على تحليل حيوي من نموذج متخصص. لم يتم إنشاء استنتاجات أو تسلسل افتراضي.")
    }

    private fun decodeBase64Bitmap(base64Data: String?): Bitmap? {
        if (base64Data.isNullOrBlank()) return null
        return runCatching {
            val clean = base64Data.substringAfter("base64,", base64Data)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun encodeBitmapToBase64Png(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun buildGeneratedStackFromAiText(
        brief: String,
        language: String,
        aiRawText: String?
    ): Map<String, String> {
        val raw = aiRawText?.trim().orEmpty()
        require(raw.isNotBlank()) { "لم يتم استلام كود من مزود الذكاء الاصطناعي." }

        val blockPattern = Regex("(?s)```([A-Za-z0-9_+#.-]*)\\s*\\n(.*?)```")
        val blocks = blockPattern.findAll(raw).map { match ->
            match.groupValues[1].lowercase() to match.groupValues[2].trim()
        }.filter { it.second.isNotBlank() }.toList()
        require(blocks.isNotEmpty()) {
            "لم يُرجع النموذج كتل كود محددة. لم يتم إنشاء ملفات افتراضية."
        }

        var html = ""
        var css = ""
        var js = ""
        var backend = ""
        blocks.forEach { (languageTag, code) ->
            when (languageTag) {
                "html", "htm", "xhtml" -> if (html.isBlank()) html = code
                "css", "scss", "sass" -> if (css.isBlank()) css = code
                "js", "javascript", "jsx", "ts", "typescript", "tsx" -> if (js.isBlank()) js = code
                else -> if (backend.isBlank()) backend = code
            }
        }

        if (html.isBlank() && css.isBlank() && js.isBlank() && backend.isBlank()) {
            throw IllegalStateException("لم يتم استخراج أي ملفات قابلة للاستخدام من استجابة النموذج.")
        }
        return mapOf(
            "html" to html,
            "css" to css,
            "js" to js,
            "backend" to backend
        )
    }
}
