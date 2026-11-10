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
    fun doesNotExecuteAmbiguousOrOrdinaryChat() {
        assertNull(AgentTaskRouter.route(""))
        assertNull(AgentTaskRouter.route("كيف أتعلم Kotlin؟"))
        assertNull(AgentTaskRouter.route("هل يمكنك شرح كيفية حفظ المشروع؟"))
    }
}
