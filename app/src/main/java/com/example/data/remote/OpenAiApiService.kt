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
import java.util.concurrent.TimeUnit

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
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatCompletionRequest
    ): OpenAiChatCompletionResponse
}

object OpenAiNetworkClient {
    private const val BASE_URL = "https://api.openai.com/"
    const val PLACEHOLDER_KEY = "YOUR_OPENAI_API_KEY"
    private const val DEFAULT_PLUGIN_PLACEHOLDER = "OPENAI_API_KEY_DEFAULT_VALUE"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
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
        return runCatching { BuildConfig.OPENAI_API_KEY.trim() }.getOrDefault("")
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getConfiguredApiKey()
        return key.isNotBlank() &&
            key != PLACEHOLDER_KEY &&
            key != DEFAULT_PLUGIN_PLACEHOLDER &&
            !key.startsWith("YOUR_")
    }

    fun getKeyStatusSummaryAr(): String {
        val key = getConfiguredApiKey()
        return if (isApiKeyConfigured()) {
            val tail = if (key.length >= 4) key.takeLast(4) else "****"
            "متصل ونشط (•••$tail)"
        } else {
            "بانتظار ضبط OPENAI_API_KEY في لوحة Secrets"
        }
    }
}
