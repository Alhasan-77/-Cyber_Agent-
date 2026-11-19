package com.example.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * UI-friendly state representation for streaming or unary AI content generation.
 */
sealed interface GeminiGenerationState {
    data object Idle : GeminiGenerationState
    data object Loading : GeminiGenerationState
    data class Streaming(val accumulatedText: String, val delta: String) : GeminiGenerationState
    data class Success(val text: String, val modelId: String) : GeminiGenerationState
    data class Error(val message: String, val cause: Throwable? = null) : GeminiGenerationState
}

/**
 * Genkit-style flow configuration defining the target Gemini model, temperature, timeout,
 * and optional system instruction.
 */
data class GenkitFlowConfig(
    val flowName: String,
    val modelName: String = GeminiServiceClient.DEFAULT_MODEL,
    val systemInstruction: String? = null,
    val temperature: Float = 0.7f,
    val timeoutMs: Long = GeminiServiceClient.DEFAULT_TIMEOUT_MS
)

/**
 * Standard Genkit flow input envelope (`{"data": ...}`).
 */
data class GenkitFlowRequest(
    val prompt: String,
    val contextMetadata: Map<String, String> = emptyMap(),
    val config: GenkitFlowConfig = GenkitFlowConfig(flowName = "defaultTextGenerationFlow")
)

/**
 * Standard Genkit flow response envelope (`{"result": ...}`).
 */
data class GenkitFlowResponse(
    val flowName: String,
    val modelName: String,
    val outputText: String,
    val durationMs: Long
)

/**
 * Secure Firebase AI Logic & Genkit-compatible client for Gemini content generation on Android.
 *
 * On Android, Firebase secures Gemini model calls using **Firebase AI Logic (`Firebase.ai`)**
 * paired with **Firebase App Check** attestation (zero client-side API keys required).
 * This client also supports invoking deployed **Firebase Genkit** HTTP flows (Cloud Functions /
 * Cloud Run endpoints) with automatic `X-Firebase-AppCheck` and `Authorization: Bearer` headers.
 */
class GeminiServiceClient(
    private val httpClient: OkHttpClient = defaultHttpClient
) {
    companion object {
        private const val TAG = "GeminiServiceClient"

        const val DEFAULT_MODEL = "gemini-3.8-flash"
        const val FAST_MODEL = "gemini-3.5-flash-lite"
        const val REASONING_MODEL = "gemini-4-argon"
        const val IMAGE_MODEL = "gemini-nano-banana-2.1"
        const val DEFAULT_TIMEOUT_MS = 60_000L

        private val SUPPORTED_GEMINI_MODELS = setOf(
            DEFAULT_MODEL,
            FAST_MODEL,
            REASONING_MODEL,
            IMAGE_MODEL,
            "gemini-3-pro-image",
            "gemini-3.1-flash-lite-image",
            "gemini-3.1-flash-tts-preview"
        )

        private val DEPRECATED_MODELS = setOf(
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-pro",
            "gemini-2.0-flash",
            "gemini-2.0-pro",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.1-flash-image"
        )

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Ensures only currently supported Gemini 3.x / 4.x models are requested, preventing
         * calls to deprecated or shut down models.
         */
        fun resolveSupportedModelName(requestedModel: String?): String {
            val trimmed = requestedModel?.trim().orEmpty()
            if (trimmed.isEmpty() || trimmed in DEPRECATED_MODELS) {
                return DEFAULT_MODEL
            }
            return if (trimmed in SUPPORTED_GEMINI_MODELS || trimmed.startsWith("gemini-3") || trimmed.startsWith("gemini-4")) {
                trimmed
            } else {
                DEFAULT_MODEL
            }
        }

        /**
         * Wraps an input payload into the canonical Firebase Genkit HTTP flow request envelope:
         * `{"data": ...}`.
         */
        fun buildGenkitEnvelopeJson(data: JSONObject): String {
            return JSONObject().put("data", data).toString()
        }

        /**
         * Extracts the `"result"` payload from a canonical Firebase Genkit HTTP flow response.
         */
        fun parseGenkitResultText(rawResponseJson: String): String {
            val root = JSONObject(rawResponseJson)
            if (root.has("error") && !root.isNull("error")) {
                val errObj = root.optJSONObject("error")
                val errMsg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: root.optString("error")
                throw IllegalStateException("Genkit flow error: $errMsg")
            }
            val resultObj = root.opt("result")
            return when (resultObj) {
                is String -> resultObj
                is JSONObject -> resultObj.optString("text").takeIf { it.isNotBlank() }
                    ?: resultObj.optString("output").takeIf { it.isNotBlank() }
                    ?: resultObj.toString()
                null -> root.optString("text").takeIf { it.isNotBlank() } ?: rawResponseJson
                else -> resultObj.toString()
            }
        }
    }

    // Strictly lazy Firebase.ai model instances so FirebaseApp and App Check initialize first
    private val defaultFlashModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig { temperature = 0.7f }
        )
    }

    private val fastFlashLiteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL,
            generationConfig = generationConfig { temperature = 0.3f }
        )
    }

    private val reasoningArgonModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = REASONING_MODEL,
            generationConfig = generationConfig { temperature = 0.5f }
        )
    }

    private fun resolveModel(
        modelName: String,
        temperature: Float? = null,
        systemInstruction: String? = null
    ): GenerativeModel {
        val safeModelName = resolveSupportedModelName(modelName)
        if (systemInstruction.isNullOrBlank() && temperature == null) {
            return when (safeModelName) {
                DEFAULT_MODEL -> defaultFlashModel
                FAST_MODEL -> fastFlashLiteModel
                REASONING_MODEL -> reasoningArgonModel
                else -> Firebase.ai.generativeModel(modelName = safeModelName)
            }
        }
        return Firebase.ai.generativeModel(
            modelName = safeModelName,
            generationConfig = generationConfig {
                if (temperature != null) {
                    this.temperature = temperature
                }
            },
            systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { sys ->
                content { text(sys) }
            }
        )
    }

    /**
     * Generates text content securely through Firebase AI Logic (`Firebase.ai`) with App Check
     * attestation and a realistic coroutine timeout.
     */
    suspend fun generateContent(
        prompt: String,
        modelName: String = DEFAULT_MODEL,
        systemInstruction: String? = null,
        temperature: Float? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<String> {
        require(prompt.isNotBlank()) { "Prompt must not be blank." }
        val safeModelName = resolveSupportedModelName(modelName)
        return try {
            val model = resolveModel(
                modelName = safeModelName,
                temperature = temperature,
                systemInstruction = systemInstruction
            )
            val response = withTimeout(timeoutMs) {
                model.generateContent(prompt)
            }
            val output = response.text?.trim()
            if (output.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini model ($safeModelName) returned an empty response."))
            } else {
                Result.success(output)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out after ${timeoutMs}ms ($safeModelName)", e)
            Result.failure(
                IllegalStateException("Generation timed out after ${timeoutMs / 1000} seconds. Please try again.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed ($safeModelName)", e)
            Result.failure(e)
        }
    }

    /**
     * Streams content tokens progressively using `generateContentStream()` to minimize
     * Time-to-First-Token (TTFT).
     */
    fun generateContentStream(
        prompt: String,
        modelName: String = DEFAULT_MODEL,
        systemInstruction: String? = null,
        temperature: Float? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Flow<GeminiGenerationState> = flow {
        if (prompt.isBlank()) {
            emit(GeminiGenerationState.Error("Prompt must not be blank."))
            return@flow
        }
        val safeModelName = resolveSupportedModelName(modelName)
        emit(GeminiGenerationState.Loading)
        val accumulated = StringBuilder()
        try {
            val model = resolveModel(
                modelName = safeModelName,
                temperature = temperature,
                systemInstruction = systemInstruction
            )
            withTimeout(timeoutMs) {
                model.generateContentStream(prompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(
                            GeminiGenerationState.Streaming(
                                accumulatedText = accumulated.toString(),
                                delta = delta
                            )
                        )
                    }
                }
            }
            val finalOutput = accumulated.toString().trim()
            if (finalOutput.isNotEmpty()) {
                emit(GeminiGenerationState.Success(text = finalOutput, modelId = safeModelName))
            } else {
                emit(GeminiGenerationState.Error("Gemini model ($safeModelName) returned an empty stream."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming timed out after ${timeoutMs}ms ($safeModelName)", e)
            emit(
                GeminiGenerationState.Error(
                    message = "Streaming timed out after ${timeoutMs / 1000} seconds. Please try again.",
                    cause = e
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI streaming failed ($safeModelName)", e)
            emit(
                GeminiGenerationState.Error(
                    message = e.localizedMessage ?: "Gemini streaming failed.",
                    cause = e
                )
            )
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        Log.e(TAG, "Unexpected stream error", error)
        emit(GeminiGenerationState.Error(error.localizedMessage ?: "Streaming error", error))
    }

    /**
     * Generates raw structured JSON guaranteed to conform to [responseSchema] using
     * `Schema.obj(...)` and `responseMimeType = "application/json"`.
     */
    suspend fun generateStructuredJson(
        prompt: String,
        responseSchema: Schema,
        modelName: String = DEFAULT_MODEL,
        systemInstruction: String? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<JSONObject> {
        require(prompt.isNotBlank()) { "Prompt must not be blank." }
        val safeModelName = resolveSupportedModelName(modelName)
        return try {
            val structuredModel = Firebase.ai.generativeModel(
                modelName = safeModelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    this.responseSchema = responseSchema
                },
                systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { sys ->
                    content { text(sys) }
                }
            )
            val response = withTimeout(timeoutMs) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim()
                ?: throw IllegalStateException("Structured generation returned empty JSON.")
            Result.success(JSONObject(rawJson))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Structured JSON generation timed out ($safeModelName)", e)
            Result.failure(
                IllegalStateException("Structured generation timed out after ${timeoutMs / 1000} seconds.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Structured JSON generation failed ($safeModelName)", e)
            Result.failure(e)
        }
    }

    /**
     * Generates multimodal content from text paired with one or more [Bitmap] images.
     */
    suspend fun generateMultimodalContent(
        prompt: String,
        images: List<Bitmap>,
        modelName: String = DEFAULT_MODEL,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<String> {
        require(prompt.isNotBlank()) { "Prompt must not be blank." }
        val safeModelName = resolveSupportedModelName(modelName)
        return try {
            val model = resolveModel(modelName = safeModelName)
            val response = withTimeout(timeoutMs) {
                model.generateContent(
                    content {
                        images.forEach { bitmap -> image(bitmap) }
                        text(prompt)
                    }
                )
            }
            val output = response.text?.trim()
            if (output.isNullOrBlank()) {
                Result.failure(IllegalStateException("Multimodal generation returned an empty response."))
            } else {
                Result.success(output)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Multimodal generation timed out ($safeModelName)", e)
            Result.failure(
                IllegalStateException("Multimodal generation timed out after ${timeoutMs / 1000} seconds.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal generation failed ($safeModelName)", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a multi-turn [Chat] session backed by Firebase AI Logic.
     */
    fun startChatSession(
        history: List<Content> = emptyList(),
        modelName: String = DEFAULT_MODEL,
        systemInstruction: String? = null
    ): Chat {
        val model = resolveModel(
            modelName = modelName,
            systemInstruction = systemInstruction
        )
        return model.startChat(history = history)
    }

    /**
     * Executes a Genkit-style structured flow request directly via Firebase AI Logic + App Check.
     */
    suspend fun executeFlow(request: GenkitFlowRequest): Result<GenkitFlowResponse> {
        val startMs = System.currentTimeMillis()
        val effectivePrompt = buildString {
            if (request.contextMetadata.isNotEmpty()) {
                appendLine("[Flow Context: ${request.config.flowName}]")
                request.contextMetadata.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
                appendLine()
            }
            append(request.prompt)
        }
        val safeModel = resolveSupportedModelName(request.config.modelName)
        return generateContent(
            prompt = effectivePrompt,
            modelName = safeModel,
            systemInstruction = request.config.systemInstruction,
            temperature = request.config.temperature,
            timeoutMs = request.config.timeoutMs
        ).map { output ->
            GenkitFlowResponse(
                flowName = request.config.flowName,
                modelName = safeModel,
                outputText = output,
                durationMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            )
        }
    }

    /**
     * Streams a Genkit-style flow request via Firebase AI Logic + App Check.
     */
    fun streamFlow(request: GenkitFlowRequest): Flow<GeminiGenerationState> {
        val effectivePrompt = buildString {
            if (request.contextMetadata.isNotEmpty()) {
                appendLine("[Flow Context: ${request.config.flowName}]")
                request.contextMetadata.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
                appendLine()
            }
            append(request.prompt)
        }
        return generateContentStream(
            prompt = effectivePrompt,
            modelName = request.config.modelName,
            systemInstruction = request.config.systemInstruction,
            temperature = request.config.temperature,
            timeoutMs = request.config.timeoutMs
        )
    }

    /**
     * Invokes a deployed remote Firebase Genkit HTTP flow endpoint (e.g., Cloud Functions for
     * Firebase or Cloud Run) with automatic Firebase App Check (`X-Firebase-AppCheck`) and
     * Firebase Auth ID token (`Authorization: Bearer`) headers.
     */
    suspend fun invokeRemoteGenkitFlow(
        flowEndpointUrl: String,
        inputData: JSONObject,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(flowEndpointUrl.startsWith("https://")) {
                "Remote Genkit flow endpoint must use HTTPS."
            }
            val appCheckToken = fetchAppCheckToken()
            val idToken = fetchFirebaseIdToken()
            val bodyJson = buildGenkitEnvelopeJson(inputData)

            val responseText = withTimeout(timeoutMs) {
                val requestBuilder = Request.Builder()
                    .url(flowEndpointUrl)
                    .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Content-Type", "application/json")
                if (!appCheckToken.isNullOrBlank()) {
                    requestBuilder.header("X-Firebase-AppCheck", appCheckToken)
                }
                if (!idToken.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $idToken")
                }

                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IllegalStateException(
                            "Genkit flow HTTP ${response.code}: ${responseBody.take(300)}"
                        )
                    }
                    parseGenkitResultText(responseBody)
                }
            }
            Result.success(responseText)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Remote Genkit flow timed out: $flowEndpointUrl", e)
            Result.failure(
                IllegalStateException("Genkit flow timed out after ${timeoutMs / 1000} seconds.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Remote Genkit flow failed: $flowEndpointUrl", e)
            Result.failure(e)
        }
    }

    /**
     * Adapts this [GeminiServiceClient] to the app's [AiTextGenerationClient] prompt-service interface.
     */
    fun asTextGenerationClient(modelName: String = DEFAULT_MODEL): AiTextGenerationClient {
        val resolvedModel = resolveSupportedModelName(modelName)
        return object : AiTextGenerationClient {
            override val provider: AiEngineProvider = AiEngineProvider.GEMINI
            override val modelId: String = resolvedModel

            override suspend fun generateText(
                systemInstruction: String,
                userPrompt: String,
                useHighThinking: Boolean
            ): Result<String> {
                val targetModel = if (useHighThinking) REASONING_MODEL else resolvedModel
                return generateContent(
                    prompt = userPrompt,
                    modelName = targetModel,
                    systemInstruction = systemInstruction,
                    temperature = if (useHighThinking) 0.4f else 0.7f
                )
            }
        }
    }

    private suspend fun fetchAppCheckToken(): String? {
        return try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to attach Firebase App Check token: ${e.message}")
            null
        }
    }

    private suspend fun fetchFirebaseIdToken(): String? {
        return try {
            Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to attach Firebase Auth ID token: ${e.message}")
            null
        }
    }
}
