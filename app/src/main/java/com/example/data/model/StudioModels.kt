package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val language: String = "Full-Stack Web",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val backendCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VersionSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val commitMessage: String = "",
    val versionTag: String = "v1.0.0",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val backendCode: String = "",
    val addedLines: Long = 0L,
    val deletedLines: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DbCollectionSchema(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val collectionName: String = "",
    val fieldsSpec: String = "",
    val sampleRecordsJson: String = "[]",
    val indexConfig: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BuildExecutionLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val stage: String = "",
    val status: String = "SUCCESS",
    val durationMs: Long = 0L,
    val reportSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class DiffType {
    UNCHANGED, ADDED, DELETED
}

data class DiffLine(
    val lineNumber: Int,
    val content: String,
    val type: DiffType
)

enum class AiRolePersona(
    val titleAr: String,
    val subtitleAr: String,
    val systemPrompt: String
) {
    FULLSTACK_ARCHITECT(
        titleAr = "مهندس معماري Full-Stack",
        subtitleAr = "تصميم الأنظمة الشاملة وتوليد الأكواد المتعددة اللغات",
        systemPrompt = "أنت مهندس برمجيات معماري خبير في بناء تطبيقات Full-Stack المتكاملة (HTML/CSS/JS, TypeScript, Python, Kotlin, SQL). قدم كوداً نظيفاً، آمناً، وقابلاً للتوسع مع شرح معماري واضح باللغة العربية."
    ),
    SECURITY_AUDITOR(
        titleAr = "مدقق أمني وحماية بيانات",
        subtitleAr = "فحص الثغرات، قواعد Firestore، وتأمين الإصدارات",
        systemPrompt = "أنت خبير أمن سيبراني ومدقق كود. قم بتحليل الكود وقواعد البيانات لاكتشاف الثغرات الأمنية (XSS, SQLi, Broken Auth) وقدم حلولاً فورية."
    ),
    DATABASE_ENGINEER(
        titleAr = "خبير قواعد البيانات السحابية",
        subtitleAr = "تصميم المخططات المرنة، الفهارس، وتحسين الاستعلامات",
        systemPrompt = "أنت مهندس قواعد بيانات خبير في Firestore و SQL. صمم مخططات بيانات مرنة وعالية الأداء مع استعلامات محسنة."
    ),
    BIOSCIENCE_SPECIALIST(
        titleAr = "خبير العلوم الحيوية والمعلوماتية",
        subtitleAr = "تحليل الجينوم، البروتينات (FASTA)، وخوارزميات Biopython",
        systemPrompt = "أنت عالم معلوماتية حيوية وباحث في البيولوجيا الحاسوبية. قم بتحليل تسلسلات الحمض النووي والبروتينات، وتوليد خطوط أنابيب برمجية بلغة Python/Biopython بدقة علمية عالية."
    )
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelUsed: String,
    val timestampLabel: String,
    val groundingSources: List<String> = emptyList()
)

data class GeneratedMediaAsset(
    val id: String,
    val type: String, // "IMAGE", "VIDEO", "AUDIO_TTS", "AUDIO_MUSIC"
    val prompt: String,
    val modelUsed: String,
    val metadataLabel: String,
    val base64Data: String? = null,
    val mimeType: String? = null,
    val textSummary: String? = null
)
