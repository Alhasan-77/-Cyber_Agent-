package com.example.data.remote

import com.example.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * High-level AI Engine Provider allowing users to switch between Gemini, ChatGPT, or Dual Engine.
 */
enum class AiEngineProvider(
    val id: String,
    val titleAr: String,
    val shortBadge: String,
    val defaultModelId: String
) {
    GEMINI(
        id = "GEMINI",
        titleAr = "🔷 Gemini (Firebase AI)",
        shortBadge = "Gemini",
        defaultModelId = "gemini-3.8-flash"
    ),
    CHATGPT(
        id = "CHATGPT",
        titleAr = "🟢 ChatGPT (OpenAI API)",
        shortBadge = "ChatGPT",
        defaultModelId = "gpt-4o"
    ),
    DUAL_ENGINE(
        id = "DUAL_ENGINE",
        titleAr = "🤝 مزدوج (Gemini + ChatGPT)",
        shortBadge = "Dual AI",
        defaultModelId = "gemini+chatgpt-dual"
    );

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

/**
 * Common interface for AI text generation clients injected into the prompt service.
 */
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

/**
 * Retrofit service interface for the OpenAI Chat Completions API.
 */
interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: OpenAiChatCompletionRequest
    ): OpenAiChatCompletionResponse
}

object OpenAiNetworkClient {
    private const val BASE_URL = "https://api.openai.com/"
    const val PLACEHOLDER_KEY = ""
    private const val DEFAULT_PLUGIN_PLACEHOLDER = "OPENAI_API_KEY_DEFAULT_VALUE"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val existingAuth = original.header("Authorization")
        val configuredKey = getConfiguredApiKey()
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
        if (existingAuth.isNullOrBlank() && !isApiKeyConfigured()) {
            throw IllegalStateException("مفتاح OpenAI غير مضبوط. أضف OPENAI_API_KEY في إعدادات الأسرار.")
        }
        if (existingAuth.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $configuredKey")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    val service: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiApiService::class.java)
    }

    fun getConfiguredApiKey(): String {
        return runCatching {
            BuildConfig.OPENAI_API_KEY.trim().removeSurrounding("\"").trim()
        }.getOrDefault("")
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getConfiguredApiKey()
        return key.isNotBlank() &&
            key != DEFAULT_PLUGIN_PLACEHOLDER &&
            !key.startsWith("YOUR_")
    }

    /**
     * A locally present key is not proof that the remote API accepts it.
     * Never expose even a partial API key in UI status text.
     */
    fun getKeyStatusSummaryAr(): String {
        return if (isApiKeyConfigured()) {
            "تم ضبط المفتاح محلياً؛ لم يتم التحقق من صلاحيته بعد"
        } else {
            "مفتاح OpenAI غير مضبوط"
        }
    }
}
