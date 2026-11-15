package com.example

import com.example.data.remote.AiEngineProvider
import com.example.data.remote.GeminiAiRepository
import com.example.data.remote.OpenAiNetworkClient
import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests for core utilities, AI routing, and safe key fallbacks.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun aiEngineProvider_mapsModelsAccurately() {
    assertEquals(AiEngineProvider.GEMINI, AiEngineProvider.fromModelId(GeminiAiRepository.MODEL_FLASH))
    assertEquals(AiEngineProvider.GEMINI, AiEngineProvider.fromModelId(GeminiAiRepository.MODEL_ARGON))
    assertEquals(AiEngineProvider.CHATGPT, AiEngineProvider.fromModelId(GeminiAiRepository.MODEL_GPT_4O))
    assertEquals(AiEngineProvider.CHATGPT, AiEngineProvider.fromModelId(GeminiAiRepository.MODEL_GPT_4O_MINI))
    assertEquals(AiEngineProvider.DUAL_ENGINE, AiEngineProvider.fromModelId(GeminiAiRepository.MODEL_DUAL_ENGINE))
  }

  @Test
  fun geminiAiRepository_normalizesPreferredModelsSafely() {
    assertEquals(GeminiAiRepository.MODEL_DUAL_ENGINE, GeminiAiRepository.normalizePreferredModel(null))
    assertEquals(GeminiAiRepository.MODEL_DUAL_ENGINE, GeminiAiRepository.normalizePreferredModel("   "))
    assertEquals(GeminiAiRepository.MODEL_DUAL_ENGINE, GeminiAiRepository.normalizePreferredModel("unknown-model"))
    assertEquals(GeminiAiRepository.MODEL_GPT_4O, GeminiAiRepository.normalizePreferredModel("gpt-4o"))
    assertEquals(GeminiAiRepository.MODEL_FLASH, GeminiAiRepository.normalizePreferredModel("gemini-3.8-flash"))
    assertTrue(GeminiAiRepository.isOpenAiModel("gpt-4o"))
    assertFalse(GeminiAiRepository.isOpenAiModel("gemini-3.8-flash"))
    assertTrue(GeminiAiRepository.isDualEngineModel(GeminiAiRepository.MODEL_DUAL_ENGINE))
  }

  @Test
  fun openAiNetworkClient_returnsNonBlockingKeyStatus() {
    val status = OpenAiNetworkClient.getKeyStatusSummaryAr()
    assertTrue(status.isNotBlank())
    // Ensure calling key check never throws regardless of secret presence
    val configured = OpenAiNetworkClient.isApiKeyConfigured()
    if (!configured) {
      assertTrue(status.contains("OPENAI_API_KEY"))
    }
  }
}
