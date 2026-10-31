package com.example

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.MainScreenLayout
import com.example.ui.components.SKILLS_HUB_TAB_INDEX
import com.example.ui.components.StudioBottomTab
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NovaForge", appName)
    }

    @Test
    fun `main screen layout displays five bottom navigation tabs and top bar skills action button`() {
        var activeTab by mutableIntStateOf(StudioBottomTab.DASHBOARD.index)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainScreenLayout(
                    selectedTab = activeTab,
                    onSelectTab = { activeTab = it },
                    userEmail = "dev@novaforge.ai",
                    activeBranch = "main",
                    skillsCount = 25,
                    pinnedSkillsCount = 3
                ) { tabIndex ->
                    Text(
                        text = "ActiveTab:$tabIndex",
                        modifier = Modifier.testTag("active_tab_indicator_$tabIndex")
                    )
                }
            }
        }

        // Verify Top Bar and 'Skills' action button
        composeTestRule.onNodeWithTag("main_top_app_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_skills_memory_button").assertIsDisplayed()

        // Verify all 5 bottom navigation tabs are displayed
        composeTestRule.onNodeWithTag("nav_tab_dashboard").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithTag("nav_tab_ide").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_db_vcs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_bioscience").assertIsDisplayed()

        // Switch through the 5 bottom tabs
        composeTestRule.onNodeWithTag("nav_tab_ide").performClick()
        assertEquals(StudioBottomTab.IDE.index, activeTab)

        composeTestRule.onNodeWithTag("nav_tab_db_vcs").performClick()
        assertEquals(StudioBottomTab.DB_VCS.index, activeTab)

        composeTestRule.onNodeWithTag("nav_tab_ai_media").performClick()
        assertEquals(StudioBottomTab.AI_MEDIA.index, activeTab)

        composeTestRule.onNodeWithTag("nav_tab_bioscience").performClick()
        assertEquals(StudioBottomTab.BIOSCIENCE.index, activeTab)

        // Click the Top Bar 'Skills' action button
        composeTestRule.onNodeWithTag("top_skills_memory_button").performClick()
        assertEquals(SKILLS_HUB_TAB_INDEX, activeTab)
    }
}
