package com.example.data.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentTaskRouterTest {
    @Test
    fun routesExplicitArabicWorkspaceCommands() {
        assertEquals(AgentTaskType.SAVE_PROJECT, AgentTaskRouter.route("احفظ المشروع"))
        assertEquals(AgentTaskType.CREATE_SNAPSHOT, AgentTaskRouter.route("أنشئ لقطة إصدار"))
        assertEquals(AgentTaskType.REVIEW_CODE, AgentTaskRouter.route("راجع الكود أمنياً"))
        assertEquals(AgentTaskType.EDIT_ACTIVE_CODE, AgentTaskRouter.route("أضف ميزة تسجيل دخول إلى الكود"))
    }

    @Test
    fun routesExplicitEnglishWorkspaceCommands() {
        assertEquals(AgentTaskType.SAVE_PROJECT, AgentTaskRouter.route("Please save current project"))
        assertEquals(AgentTaskType.CREATE_SNAPSHOT, AgentTaskRouter.route("Create snapshot now"))
        assertEquals(AgentTaskType.REVIEW_CODE, AgentTaskRouter.route("Please review code"))
        assertEquals(AgentTaskType.EDIT_ACTIVE_CODE, AgentTaskRouter.route("Implement this feature"))
    }

    @Test
    fun parsesArabicAndEnglishProjectTechnologyWithoutDroppingTheStack() {
        val arabic = AgentTaskRouter.parseProjectRequest(
            "أنشئ مشروع متجر إلكتروني باستخدام React و Firebase",
            fallbackLanguage = "Full-Stack Web"
        )
        assertEquals("متجر إلكتروني", arabic.name)
        assertEquals("React و Firebase", arabic.language)

        val english = AgentTaskRouter.parseProjectRequest(
            "Create a mobile app with Kotlin and Jetpack Compose"
        )
        assertEquals("mobile app", english.name)
        assertEquals("Kotlin and Jetpack Compose", english.language)
    }

    @Test
    fun projectRequestUsesConfiguredFallbackWhenNoStackWasProvided() {
        val request = AgentTaskRouter.parseProjectRequest(
            "أنشئ مشروع إدارة المخزون",
            fallbackLanguage = "Python + FastAPI"
        )
        assertEquals("إدارة المخزون", request.name)
        assertEquals("Python + FastAPI", request.language)
    }

    @Test
    fun doesNotExecuteAmbiguousOrOrdinaryChat() {
        assertNull(AgentTaskRouter.route(""))
        assertNull(AgentTaskRouter.route("كيف أتعلم Kotlin؟"))
        assertNull(AgentTaskRouter.route("هل يمكنك شرح كيفية حفظ المشروع؟"))
    }
}
