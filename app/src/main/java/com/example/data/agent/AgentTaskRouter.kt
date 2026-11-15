package com.example.data.agent

import java.util.Locale

enum class AgentTaskType {
    CREATE_PROJECT,
    SAVE_PROJECT,
    CREATE_SNAPSHOT,
    REVIEW_CODE,
    EDIT_ACTIVE_CODE
}

data class AgentProjectRequest(
    val name: String,
    val language: String
)

/**
 * Routes only explicit, bounded workspace commands to local executors.
 * General questions remain normal AI chat; ambiguous requests are never executed.
 */
object AgentTaskRouter {
    fun parseProjectRequest(prompt: String, fallbackLanguage: String = "Full-Stack Web"): AgentProjectRequest {
        val cleanPrompt = prompt.trim()
        val commandPrefix = Regex(
            "^\\s*(?:من فضلك\\s*)?(?:أنشئ|انشئ)\\s+(?:لي\\s+)?مشروع\\s*|^\\s*(?:please\\s+)?(?:create|build)\\s+(?:a\\s+)?project\\s*",
            RegexOption.IGNORE_CASE
        )
        val withoutPrefix = cleanPrompt.replaceFirst(commandPrefix, "").trim()
        val stackMatch = Regex(
            "\\s+(?:باستخدام|مع|using|with)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(withoutPrefix)
        val requestedLanguage = stackMatch?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }
        val name = if (stackMatch != null) {
            withoutPrefix.substring(0, stackMatch.range.first).trim()
        } else {
            withoutPrefix
        }.take(80)

        require(name.isNotBlank()) {
            "حدد اسم المشروع بعد أمر الإنشاء."
        }

        return AgentProjectRequest(
            name = name,
            language = (requestedLanguage ?: fallbackLanguage).ifBlank { "Full-Stack Web" }.take(50)
        )
    }

    fun route(prompt: String): AgentTaskType? {
        val normalized = normalize(prompt)
        if (normalized.isBlank()) return null

        return when {
            containsAny(
                normalized,
                "انشئ مشروع",
                "انشئ لي مشروع",
                "create a project",
                "create project",
                "build a project"
            ) -> AgentTaskType.CREATE_PROJECT

            containsAny(
                normalized,
                "احفظ المشروع",
                "احفظ مشروعي",
                "save project",
                "save current project"
            ) -> AgentTaskType.SAVE_PROJECT

            containsAny(
                normalized,
                "انشئ لقطة",
                "انشئ نسخه",
                "احفظ نسخه",
                "لقطه اصدار",
                "create snapshot",
                "save snapshot"
            ) -> AgentTaskType.CREATE_SNAPSHOT

            containsAny(
                normalized,
                "راجع الكود",
                "افحص الكود",
                "حلل الكود امنيا",
                "مراجعه امنيه للكود",
                "review code",
                "audit this code",
                "security review"
            ) -> AgentTaskType.REVIEW_CODE

            containsAny(
                normalized,
                "عدل الكود",
                "نفذ التعديل",
                "طبق التعديل",
                "اضف ميزه",
                "حسن الكود",
                "اكتب الكود",
                "implement this",
                "modify the code",
                "edit the code",
                "add a feature"
            ) -> AgentTaskType.EDIT_ACTIVE_CODE

            else -> null
        }
    }

    private fun normalize(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsAny(value: String, vararg phrases: String): Boolean {
        return phrases.any { phrase ->
            val normalizedPhrase = normalize(phrase)
            Regex("(^|\\s)" + Regex.escape(normalizedPhrase) + "($|\\s)")
                .containsMatchIn(value)
        }
    }
}
