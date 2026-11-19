package com.example

import com.example.data.remote.AiEngineProvider
import com.example.data.remote.GeminiAiRepository
import com.example.data.remote.GeminiServiceClient
import com.example.data.remote.OpenAiNetworkClient
import com.example.data.repository.DataManager
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

  @Test
  fun geminiServiceClient_normalizesModelsAndInitializesLazily() {
    // Must instantiate safely on JVM without triggering eager FirebaseApp / Firebase.ai access
    val client = GeminiServiceClient()
    val adapted = client.asTextGenerationClient("gemini-4-argon")
    assertEquals(AiEngineProvider.GEMINI, adapted.provider)
    assertEquals(GeminiServiceClient.REASONING_MODEL, adapted.modelId)

    // Deprecated models must fall back safely to supported default model
    assertEquals(GeminiServiceClient.DEFAULT_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-1.5-flash"))
    assertEquals(GeminiServiceClient.DEFAULT_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-2.0-flash"))
    assertEquals(GeminiServiceClient.DEFAULT_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-2.5-flash"))
    assertEquals(GeminiServiceClient.DEFAULT_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-3.1-flash-image"))
    assertEquals(GeminiServiceClient.FAST_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-3.5-flash-lite"))
    assertEquals(GeminiServiceClient.REASONING_MODEL, GeminiServiceClient.resolveSupportedModelName("gemini-4-argon"))
  }

  @Test
  fun dataManager_validatesDocumentIdsAndSanitizesPayloads() {
    DataManager.validateDocumentId("proj_abc123-XYZ", "Project ID")
    assertThrows(IllegalArgumentException::class.java) {
      DataManager.validateDocumentId("bad/path/id", "Project ID")
    }
    assertThrows(IllegalArgumentException::class.java) {
      DataManager.validateDocumentId("", "Project ID")
    }

    val createPayload = DataManager.sanitizeCreatePayload(
      userId = "user_42",
      data = mapOf("id" to "should_strip", "name" to "Demo", "nullableField" to null)
    )
    assertFalse(createPayload.containsKey("id"))
    assertFalse(createPayload.containsKey("nullableField"))
    assertEquals("user_42", createPayload["userId"])
    assertEquals("Demo", createPayload["name"])
    assertNotNull(createPayload["createdAt"])
    assertNotNull(createPayload["updatedAt"])

    val updatePayload = DataManager.sanitizeUpdatePayload(
      mapOf("id" to "strip", "userId" to "hijack", "createdAt" to 123, "name" to "Updated", "empty" to null)
    )
    assertFalse(updatePayload.containsKey("id"))
    assertFalse(updatePayload.containsKey("userId"))
    assertFalse(updatePayload.containsKey("createdAt"))
    assertFalse(updatePayload.containsKey("empty"))
    assertEquals("Updated", updatePayload["name"])
    assertNotNull(updatePayload["updatedAt"])
  }

  @Test
  fun dataManager_enforcesNamedDatabaseIdAndRejectsDefault() {
    assertEquals(
      "ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0",
      DataManager.NAMED_DATABASE_ID
    )
    assertEquals(
      DataManager.NAMED_DATABASE_ID,
      DataManager.validateDatabaseId(DataManager.NAMED_DATABASE_ID)
    )
    assertThrows(IllegalArgumentException::class.java) {
      DataManager.validateDatabaseId("(default)")
    }
    assertThrows(IllegalArgumentException::class.java) {
      DataManager.validateDatabaseId("   ")
    }
  }
}
