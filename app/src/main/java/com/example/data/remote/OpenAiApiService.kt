package com.example.data.remote

import com.example.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.net.URI
import java.util.concurrent.TimeUnit

enum class AiEngineProvider(
    val id: String,
    val titleAr: String,
    val shortBadge: String,
    val defaultModelId: String
) {
    GEMINI("GEMINI", "🔷 Gemini (Firebase AI)", "Gemini", "gemini-3.8-flash"),
    CHATGPT("CHATGPT", "🟢 ChatGPT (Secure Proxy)", "ChatGPT", "gpt-4o"),
    DUAL_ENGINE("DUAL_ENGINE", "🤝 مزدوج (Gemini + ChatGPT)", "Dual AI", "gemini+chatgpt-dual");

    companion object {
        fun fromModelId(modelId: String): AiEngineProvider {
            return when {
                modelId == GeminiAiRepository.MODEL_DUAL_ENGINE -> DUAL_ENGINE
                GeminiAiRepository.isOpenAiModel(modelId) -> CHATGPT
                else -> GEMINI
            }
        }
    }
}

interface AiTextGenerationClient {
    val provider: AiEngineProvider
    val modelId: String
    suspend fun generateText(
        systemInstruction: String,
        userPrompt: String,
        useHighThinking: Boolean = false
    ): Result<String>
}

@Serializable
data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Float? = 0.7f,
    @SerialName("max_tokens")
    val maxTokens: Int? = 2048
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenAiChatChoice> = emptyList(),
    val error: OpenAiErrorDetail? = null
)

@Serializable
data class OpenAiChatChoice(
    val index: Int = 0,
    val message: OpenAiChatMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

interface OpenAiApiService {
    @POST("openaiChatCompletion")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body request: OpenAiChatCompletionRequest
    ): OpenAiChatCompletionResponse
}

/**
 * The Android client talks only to the Firebase HTTPS proxy.
 * OpenAI provider credentials are never read from or packaged in BuildConfig.
 */
object OpenAiNetworkClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun configuredBaseUrl(): String {
        val value = BuildConfig.OPENAI_PROXY_BASE_URL.trim()
        if (value.isBlank()) return "https://localhost/"
        return if (value.endsWith("/")) value else "$value/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(configuredBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiApiService::class.java)
    }

    fun isProxyConfigured(): Boolean {
        val value = BuildConfig.OPENAI_PROXY_BASE_URL.trim()
        if (value.isBlank()) return false
        return runCatching {
            val uri = URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }

    fun getProxyStatusSummaryAr(): String {
        return if (isProxyConfigured()) {
            "عنوان خادم OpenAI مضبوط؛ لم يتم التحقق من الاتصال بعد"
        } else {
            "خادم OpenAI الوسيط غير مضبوط"
        }
    }

    @Deprecated("Use isProxyConfigured()")
    fun isApiKeyConfigured(): Boolean = isProxyConfigured()

    @Deprecated("Use getProxyStatusSummaryAr()")
    fun getKeyStatusSummaryAr(): String = getProxyStatusSummaryAr()

    @Deprecated("Provider keys must never be stored in the Android app.")
    fun getConfiguredApiKey(): String = ""
}
