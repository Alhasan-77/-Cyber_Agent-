package com.example

import android.content.Context
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.skills.BioscienceSimulationEngine
import com.example.data.skills.BioscienceWorkflowMode
import com.example.ui.components.MainScreenLayout
import com.example.ui.components.SKILLS_HUB_TAB_INDEX
import com.example.ui.components.StudioBottomTab
import com.example.ui.screens.BioscienceLabTabScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `bioscience simulation engine computes accurate metrics and generates 3d biogame html`() {
        val crisprMetrics = BioscienceSimulationEngine.analyzeSequenceMetrics(
            rawInput = BioscienceWorkflowMode.CRISPR_EDITING.defaultSequence,
            workflowMode = BioscienceWorkflowMode.CRISPR_EDITING
        )
        assertEquals("DNA", crisprMetrics.sequenceType)
        assertTrue(crisprMetrics.sequenceLength > 100)
        assertTrue(crisprMetrics.gcContentPct in 30.0..80.0)
        assertTrue(crisprMetrics.pamSites.isNotEmpty())
        assertEquals(100, crisprMetrics.alphaHelixPct + crisprMetrics.betaSheetPct + crisprMetrics.randomCoilPct)

        val html3d = BioscienceSimulationEngine.buildInteractive3dBioGameHtml("GAME", "DNA_HELIX")
        assertTrue(html3d.contains("bio3d-canvas"))
        assertTrue(html3d.contains("CELLULAR DEFENSE 3D"))
        assertTrue(html3d.contains("CRISPR_CAS9"))
    }

    @Test
    fun `bioscience lab screen displays 3d simulation and four interactive workflows`() {
        var exportedTemplate = false

        composeTestRule.setContent {
            MyApplicationTheme {
                BioscienceLabTabScreen(
                    bioscienceOutput = "Sample AI Bio Output",
                    onRunBioscienceAnalysis = { _, _ -> },
                    onExportPipelineToIde = {},
                    onLoad3dBioGameTemplateInIde = { exportedTemplate = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("bio_section_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_3d_simulation_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio3d_mode_game").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio3d_mode_inspector").assertIsDisplayed()

        // Switch to Bio Tools filter to inspect the 4 workflow chips directly
        composeTestRule.onNodeWithTag("bio_section_tools").performClick()
        composeTestRule.onNodeWithTag("bio_workflow_PROTEIN_FOLDING").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_workflow_CRISPR_EDITING").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_workflow_GENOMIC_ALIGNMENT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_workflow_CLINICAL_TRIALS").assertIsDisplayed()

        // Switch back to Game 3D and trigger IDE export
        composeTestRule.onNodeWithTag("bio_section_game3d").performClick()
        composeTestRule.onNodeWithTag("export_3d_biogame_to_ide_button").performClick()
        assertTrue(exportedTemplate)
    }

    @Test
    fun `protein folding visualizer renders simulated structure and switches interactive views`() {
        val sampleMetrics = BioscienceSimulationEngine.analyzeSequenceMetrics(
            rawInput = BioscienceWorkflowMode.PROTEIN_FOLDING.defaultSequence,
            workflowMode = BioscienceWorkflowMode.PROTEIN_FOLDING
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    com.example.ui.components.ProteinFoldingVisualizer(metrics = sampleMetrics)
                }
            }
        }

        composeTestRule.onNodeWithTag("protein_folding_visualizer_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folding_3d_backbone_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folding_progress_slider").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("folding_residue_inspector").performScrollTo().assertIsDisplayed()

        // Switch visualization mode to Ramachandran Plot and Contact/Energy Map
        composeTestRule.onNodeWithTag("folding_view_RAMACHANDRAN").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("folding_ramachandran_canvas").assertIsDisplayed()

        composeTestRule.onNodeWithTag("folding_view_CONTACT_ENERGY").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("folding_contact_energy_canvas").assertIsDisplayed()

        // Switch coloring mode and step through residues
        composeTestRule.onNodeWithTag("folding_color_PLDDT").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("folding_selected_residue_detail").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("folding_next_residue_button").performClick()
        composeTestRule.onNodeWithTag("folding_residue_chip_2").assertIsDisplayed()
    }

    @Test
    fun `ai engine control bar toggles between Gemini and ChatGPT and opens model dropdown`() {
        var selectedModel = "gemini-3.8-flash"
        var toggledToChatGpt = false

        composeTestRule.setContent {
            MyApplicationTheme {
                MainScreenLayout(
                    selectedTab = StudioBottomTab.DASHBOARD.index,
                    onSelectTab = {},
                    userEmail = "dev@novaforge.ai",
                    activeBranch = "main",
                    skillsCount = 25,
                    pinnedSkillsCount = 3,
                    selectedAiModel = selectedModel,
                    onSelectAiModel = { selectedModel = it },
                    onToggleAiProvider = { useChatGpt ->
                        toggledToChatGpt = useChatGpt
                        selectedModel = if (useChatGpt) "gpt-4o" else "gemini-3.8-flash"
                    }
                ) {
                    Text("Content")
                }
            }
        }

        composeTestRule.onNodeWithTag("ai_engine_control_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_provider_toggle").assertIsDisplayed().performClick()
        assertTrue(toggledToChatGpt)
        assertEquals("gpt-4o", selectedModel)

        composeTestRule.onNodeWithTag("ai_model_dropdown_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("ai_dropdown_option_gemini-4-argon").assertIsDisplayed().performClick()
        assertEquals("gemini-4-argon", selectedModel)
    }
}

