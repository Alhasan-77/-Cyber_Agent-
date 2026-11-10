package com.example.data.agent

import java.util.Locale

enum class AgentTaskType {
    SAVE_PROJECT,
    CREATE_SNAPSHOT,
    REVIEW_CODE,
    EDIT_ACTIVE_CODE
}

/**
 * Routes only explicit, bounded workspace commands to local executors.
 * General questions remain normal AI chat; ambiguous requests are never executed.
 */
object AgentTaskRouter {
    fun route(prompt: String): AgentTaskType? {
        val normalized = normalize(prompt)
        if (normalized.isBlank()) return null

        return when {
            containsAny(
                normalized,
                "احفظ المشروع",
                "حفظ المشروع",
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
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsAny(value: String, vararg phrases: String): Boolean {
        return phrases.any { phrase ->
            val normalizedPhrase = normalize(phrase)
            Regex("(^|\\\\s)" + Regex.escape(normalizedPhrase) + "($|\\\\s)")
                .containsMatchIn(value)
        }
    }
}
