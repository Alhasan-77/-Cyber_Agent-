package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.remote.AiEngineProvider
import com.example.data.remote.GeminiAiRepository
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.GeminiBorderGradient
import com.example.ui.theme.GeminiOnPillSelectedContainer
import com.example.ui.theme.GeminiPillSelectedContainer
import com.example.ui.theme.GeminiSparkleGradient
import kotlinx.coroutines.launch

const val SKILLS_HUB_TAB_INDEX = 5

enum class StudioBottomTab(
    val index: Int,
    val titleRes: Int,
    val icon: ImageVector,
    val testTag: String,
    val subtitle: String
) {
    DASHBOARD(
        index = 0,
        titleRes = R.string.tab_dashboard,
        icon = Icons.Default.Dashboard,
        testTag = "nav_tab_dashboard",
        subtitle = "نظرة عامة والمشاريع السحابية"
    ),
    IDE(
        index = 1,
        titleRes = R.string.tab_ide,
        icon = Icons.Default.Code,
        testTag = "nav_tab_ide",
        subtitle = "محرر الأكواد والمعاينة الحية"
    ),
    DB_VCS(
        index = 2,
        titleRes = R.string.tab_db_vcs,
        icon = Icons.Default.Storage,
        testTag = "nav_tab_db_vcs",
        subtitle = "قواعد البيانات وإدارة الإصدارات"
    ),
    AI_MEDIA(
        index = 3,
        titleRes = R.string.tab_ai_media,
        icon = Icons.Default.AutoAwesome,
        testTag = "nav_tab_ai_media",
        subtitle = "محادثة Gemini وتوليد الوسائط"
    ),
    BIOSCIENCE(
        index = 4,
        titleRes = R.string.tab_bioscience,
        icon = Icons.Default.Biotech,
        testTag = "nav_tab_bioscience",
        subtitle = "طي البروتينات والمختبر الحيوي"
    )
}

@Composable
fun MainScreenLayout(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    userEmail: String,
    activeBranch: String,
    skillsCount: Int,
    pinnedSkillsCount: Int,
    isBusy: Boolean = false,
    statusBanner: String? = null,
    onDismissStatusBanner: () -> Unit = {},
    onSignOut: () -> Unit = {},
    userDisplayName: String = "NovaForge Developer",
    userId: String = "",
    syncedProjectsCount: Int = 0,
    syncedBioExperimentsCount: Int = 0,
    syncLatencyMs: Long = 18L,
    lastSyncTimestampLabel: String = "متزامن الآن",
    selectedAiModel: String = GeminiAiRepository.MODEL_FLASH,
    onSelectAiModel: (String) -> Unit = {},
    onToggleAiProvider: (Boolean) -> Unit = {},
    onForceCloudSync: () -> Unit = {},
    onSwitchGoogleAccount: () -> Unit = {},
    onSubmitPrompt: (String) -> Unit = {},
    initialDrawerOpen: Boolean = false,
    content: @Composable (Int) -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(
        initialValue = if (initialDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            GeminiMinimalNavigationDrawer(
                selectedTab = selectedTab,
                onSelectTab = { tabIndex ->
                    onSelectTab(tabIndex)
                    coroutineScope.launch { drawerState.close() }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                onStartNewPrompt = {
                    onSelectTab(StudioBottomTab.AI_MEDIA.index)
                    coroutineScope.launch { drawerState.close() }
                },
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                activeBranch = activeBranch,
                skillsCount = skillsCount,
                pinnedSkillsCount = pinnedSkillsCount,
                syncedProjectsCount = syncedProjectsCount,
                syncedBioExperimentsCount = syncedBioExperimentsCount,
                syncLatencyMs = syncLatencyMs,
                selectedAiModel = selectedAiModel,
                onOpenProfileDialog = {
                    coroutineScope.launch { drawerState.close() }
                    showProfileDialog = true
                },
                onForceCloudSync = onForceCloudSync
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground),
            containerColor = CyberBackground,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBackground)
                        .statusBarsPadding()
                ) {
                    StudioTopAppBar(
                        userEmail = userEmail,
                        userDisplayName = userDisplayName,
                        activeBranch = activeBranch,
                        skillsCount = skillsCount,
                        pinnedSkillsCount = pinnedSkillsCount,
                        isSkillsHubSelected = selectedTab == SKILLS_HUB_TAB_INDEX,
                        isCloudSynced = true,
                        selectedAiModel = selectedAiModel,
                        onOpenNavigationDrawer = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onOpenSkillsHub = { onSelectTab(SKILLS_HUB_TAB_INDEX) },
                        onOpenProfileDialog = { showProfileDialog = true },
                        onSignOut = onSignOut
                    )

                    AiEngineControlBar(
                        selectedAiModel = selectedAiModel,
                        onSelectAiModel = onSelectAiModel,
                        onToggleAiProvider = onToggleAiProvider
                    )

                    if (isBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .testTag("global_busy_indicator"),
                            color = CyberCyan,
                            trackColor = CyberSurfaceElevated
                        )
                    }

                    AnimatedVisibility(
                        visible = !statusBanner.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        StatusBannerBar(
                            message = statusBanner.orEmpty(),
                            onDismiss = onDismissStatusBanner
                        )
                    }
                }
            },
            bottomBar = {
                GeminiBottomPromptDock(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    selectedAiModel = selectedAiModel,
                    isBusy = isBusy,
                    onSubmitPrompt = { promptText ->
                        onSubmitPrompt(promptText)
                        if (selectedTab != StudioBottomTab.AI_MEDIA.index) {
                            onSelectTab(StudioBottomTab.AI_MEDIA.index)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBackground)
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                ) {
                    content(selectedTab)
                }
            }
        }
    }

    if (showProfileDialog) {
        CloudProfileAndSyncDialog(
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            userId = userId,
            activeBranch = activeBranch,
            syncedProjectsCount = syncedProjectsCount,
            syncedBioExperimentsCount = syncedBioExperimentsCount,
            pinnedSkillsCount = pinnedSkillsCount,
            syncLatencyMs = syncLatencyMs,
            lastSyncTimestampLabel = lastSyncTimestampLabel,
            selectedAiModel = selectedAiModel,
            onSelectAiModel = onSelectAiModel,
            onToggleAiProvider = onToggleAiProvider,
            onForceCloudSync = onForceCloudSync,
            onSwitchGoogleAccount = {
                showProfileDialog = false
                onSwitchGoogleAccount()
            },
            onSignOut = {
                showProfileDialog = false
                onSignOut()
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
private fun GeminiMinimalNavigationDrawer(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onCloseDrawer: () -> Unit,
    onStartNewPrompt: () -> Unit,
    userDisplayName: String,
    userEmail: String,
    activeBranch: String,
    skillsCount: Int,
    pinnedSkillsCount: Int,
    syncedProjectsCount: Int,
    syncedBioExperimentsCount: Int,
    syncLatencyMs: Long,
    selectedAiModel: String,
    onOpenProfileDialog: () -> Unit,
    onForceCloudSync: () -> Unit
) {
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)

    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .fillMaxHeight()
            .testTag("gemini_navigation_drawer"),
        drawerContainerColor = CyberSurface,
        drawerContentColor = CyberTextPrimary,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Minimalist Gemini Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.studio_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${activeProvider.shortBadge} • $activeBranch",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("drawer_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_navigation_drawer),
                            tint = CyberTextSecondary
                        )
                    }
                }

                // Gemini "+ New Chat / Prompt" pill button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GeminiPillSelectedContainer,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onStartNewPrompt)
                        .minimumInteractiveComponentSize()
                        .testTag("drawer_new_prompt_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiOnPillSelectedContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.drawer_new_chat),
                            style = MaterialTheme.typography.labelLarge,
                            color = GeminiOnPillSelectedContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    color = CyberBorder.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = stringResource(R.string.drawer_section_workspaces),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberTextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // Primary Workspaces
                StudioBottomTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab.index
                    val badgeCount = when (tab) {
                        StudioBottomTab.DASHBOARD -> syncedProjectsCount
                        StudioBottomTab.BIOSCIENCE -> syncedBioExperimentsCount
                        else -> 0
                    }

                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = tab.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        GeminiOnPillSelectedContainer.copy(alpha = 0.8f)
                                    } else {
                                        CyberTextSecondary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectTab(tab.index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.titleRes),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        badge = if (badgeCount > 0) {
                            {
                                Badge(
                                    containerColor = if (isSelected) CyberCyan else CyberSurfaceHigh,
                                    contentColor = if (isSelected) Color(0xFF002D4B) else CyberTextSecondary
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(50),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GeminiPillSelectedContainer,
                            selectedIconColor = GeminiOnPillSelectedContainer,
                            selectedTextColor = GeminiOnPillSelectedContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = CyberTextSecondary,
                            unselectedTextColor = CyberTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_${tab.testTag}")
                    )
                }

                HorizontalDivider(
                    color = CyberBorder.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = stringResource(R.string.drawer_section_memory_and_cloud),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberTextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                val isSkillsSelected = selectedTab == SKILLS_HUB_TAB_INDEX
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(
                                text = stringResource(R.string.action_skills),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSkillsSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = stringResource(R.string.skills_action_content_description),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSkillsSelected) {
                                    GeminiOnPillSelectedContainer.copy(alpha = 0.8f)
                                } else {
                                    CyberTextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    selected = isSkillsSelected,
                    onClick = { onSelectTab(SKILLS_HUB_TAB_INDEX) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.skills_action_content_description),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    badge = {
                        Badge(
                            containerColor = CyberViolet.copy(alpha = 0.25f),
                            contentColor = CyberViolet
                        ) {
                            Text(
                                text = "$pinnedSkillsCount/$skillsCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = GeminiPillSelectedContainer,
                        selectedIconColor = GeminiOnPillSelectedContainer,
                        selectedTextColor = GeminiOnPillSelectedContainer,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = CyberViolet,
                        unselectedTextColor = CyberTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_nav_tab_skills")
                )
            }

            // Minimalist Drawer Footer: Cloud Sync & Account Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CyberSurfaceElevated,
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenProfileDialog)
                    .testTag("drawer_profile_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userDisplayName.trim().take(1).uppercase().ifBlank { "G" },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = userDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$userEmail • ${syncLatencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onForceCloudSync,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("drawer_force_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.force_cloud_sync_action),
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiBottomPromptDock(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    selectedAiModel: String,
    isBusy: Boolean,
    onSubmitPrompt: (String) -> Unit
) {
    var promptText by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)

    Surface(
        color = CyberBackground.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minimalist Workspace Quick-Switch Pills (keeps 1-tap navigation + testTag compatibility)
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("bottom_nav_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioBottomTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab.index
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) GeminiPillSelectedContainer else CyberSurface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                CyberCyan.copy(alpha = 0.5f)
                            } else {
                                CyberBorder.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelectTab(tab.index) }
                            )
                            .minimumInteractiveComponentSize()
                            .testTag(tab.testTag)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.titleRes),
                                tint = if (isSelected) GeminiOnPillSelectedContainer else CyberTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) GeminiOnPillSelectedContainer else CyberTextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom-Centered Prompt Input Area with 'Sparkle' Icon (Modern Gemini UX)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_bottom_prompt_bar"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = CyberSurfaceElevated,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .border(
                            width = 1.2.dp,
                            brush = GeminiBorderGradient,
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Leading Gemini 'Sparkle' Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient)
                                .clickable { onSelectTab(StudioBottomTab.AI_MEDIA.index) }
                                .testTag("bottom_prompt_sparkle_icon"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.bottom_prompt_sparkle_content_description),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Minimalist Prompt Input TextField
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bottom_prompt_input_field"),
                            placeholder = {
                                Text(
                                    text = "${stringResource(R.string.bottom_prompt_placeholder)} (${activeProvider.shortBadge})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = CyberTextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val trimmed = promptText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onSubmitPrompt(trimmed)
                                        promptText = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = CyberCyan
                            )
                        )

                        // Send Prompt Button
                        val canSend = promptText.trim().isNotEmpty() && !isBusy
                        FilledIconButton(
                            onClick = {
                                val trimmed = promptText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSubmitPrompt(trimmed)
                                    promptText = ""
                                    focusManager.clearFocus()
                                } else {
                                    onSelectTab(StudioBottomTab.AI_MEDIA.index)
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (canSend) GeminiPillSelectedContainer else CyberSurfaceHigh,
                                contentColor = if (canSend) GeminiOnPillSelectedContainer else CyberTextSecondary
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("bottom_prompt_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.bottom_prompt_send_content_description),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiEngineControlBar(
    selectedAiModel: String,
    onSelectAiModel: (String) -> Unit,
    onToggleAiProvider: (Boolean) -> Unit
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)
    val isChatGptSelected = activeProvider == AiEngineProvider.CHATGPT
    val modelDisplayLabel = GeminiAiRepository.SUPPORTED_TEXT_MODELS
        .firstOrNull { it.first == selectedAiModel }
        ?.second
        ?: selectedAiModel

    Surface(
        color = CyberBackground,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_engine_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Provider Pills + Toggle (Gemini vs ChatGPT)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isChatGptSelected,
                    onClick = { onToggleAiProvider(!isChatGptSelected) },
                    label = {
                        Text(
                            text = if (isChatGptSelected) "🤖 ChatGPT" else "✨ Gemini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberEmerald.copy(alpha = 0.22f),
                        selectedLabelColor = CyberEmerald,
                        containerColor = GeminiPillSelectedContainer,
                        labelColor = GeminiOnPillSelectedContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isChatGptSelected,
                        borderColor = CyberCyan.copy(alpha = 0.5f),
                        selectedBorderColor = CyberEmerald.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("ai_provider_toggle")
                )
            }

            // Gemini-style Model Dropdown Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyberSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isChatGptSelected) CyberEmerald.copy(alpha = 0.4f) else CyberBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { modelMenuExpanded = true }
                        .minimumInteractiveComponentSize()
                        .testTag("ai_model_dropdown_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (isChatGptSelected) CyberEmerald else CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = modelDisplayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select AI Model",
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false },
                    modifier = Modifier
                        .background(CyberSurfaceElevated)
                        .testTag("ai_model_dropdown_menu")
                ) {
                    GeminiAiRepository.SUPPORTED_TEXT_MODELS.forEach { (modelId, label) ->
                        val isCurrent = modelId == selectedAiModel
                        val optionProvider = AiEngineProvider.fromModelId(modelId)
                        val badgeColor = if (optionProvider == AiEngineProvider.CHATGPT) CyberEmerald else CyberCyan
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrent) badgeColor else CyberTextPrimary,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = badgeColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = optionProvider.shortBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                modelMenuExpanded = false
                                onSelectAiModel(modelId)
                            },
                            modifier = Modifier.testTag("ai_dropdown_option_$modelId")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTopAppBar(
    userEmail: String,
    userDisplayName: String,
    activeBranch: String,
    skillsCount: Int,
    pinnedSkillsCount: Int,
    isSkillsHubSelected: Boolean,
    isCloudSynced: Boolean,
    selectedAiModel: String,
    onOpenNavigationDrawer: () -> Unit,
    onOpenSkillsHub: () -> Unit,
    onOpenProfileDialog: () -> Unit,
    onSignOut: () -> Unit
) {
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)

    Surface(
        color = CyberBackground,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_top_app_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Minimalist Drawer Menu Button + Gemini Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onOpenNavigationDrawer,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.open_navigation_drawer),
                        tint = CyberTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GeminiSparkleGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GeminiPillSelectedContainer.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = activeProvider.shortBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = GeminiOnPillSelectedContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "$activeBranch • $selectedAiModel",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Skills Pill + Google Profile Avatar + Sign Out
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSkillsHubSelected) {
                        GeminiPillSelectedContainer
                    } else {
                        CyberSurface
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSkillsHubSelected) CyberCyan else CyberBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSkillsHub)
                        .minimumInteractiveComponentSize()
                        .testTag("top_skills_memory_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.skills_action_content_description),
                            tint = if (isSkillsHubSelected) GeminiOnPillSelectedContainer else CyberViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.skills_action_button_format,
                                skillsCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSkillsHubSelected) GeminiOnPillSelectedContainer else CyberTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (pinnedSkillsCount > 0) {
                            Badge(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF002D4B)
                            ) {
                                Text(
                                    text = pinnedSkillsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Interactive Google Profile & Sync Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyberSurface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCloudSynced) CyberEmerald.copy(alpha = 0.45f) else CyberBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenProfileDialog)
                        .minimumInteractiveComponentSize()
                        .testTag("top_cloud_profile_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userDisplayName.trim().take(1).uppercase().ifBlank { "G" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = stringResource(R.string.cloud_profile_content_description),
                            tint = CyberEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = CyberTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudProfileAndSyncDialog(
    userDisplayName: String,
    userEmail: String,
    userId: String,
    activeBranch: String,
    syncedProjectsCount: Int,
    syncedBioExperimentsCount: Int,
    pinnedSkillsCount: Int,
    syncLatencyMs: Long,
    lastSyncTimestampLabel: String,
    selectedAiModel: String,
    onSelectAiModel: (String) -> Unit,
    onToggleAiProvider: (Boolean) -> Unit,
    onForceCloudSync: () -> Unit,
    onSwitchGoogleAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cloud_profile_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Column {
                            Text(
                                text = userDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_profile_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss_banner),
                            tint = CyberTextSecondary
                        )
                    }
                }

                // Active Sync Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyberEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.cloud_sync_active_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "آخر مزامنة: $lastSyncTimestampLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberTextSecondary
                                )
                            }
                        }
                        Text(
                            text = "${syncLatencyMs}ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Preferred AI Model Persistence Section
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyberSurfaceElevated,
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "محرك الذكاء الاصطناعي المفضل (محفوظ سحابياً)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (activeProvider == AiEngineProvider.CHATGPT) {
                                    CyberEmerald.copy(alpha = 0.2f)
                                } else {
                                    GeminiPillSelectedContainer
                                }
                            ) {
                                Text(
                                    text = activeProvider.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeProvider == AiEngineProvider.CHATGPT) {
                                        CyberEmerald
                                    } else {
                                        GeminiOnPillSelectedContainer
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "النموذج النشط: $selectedAiModel",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onToggleAiProvider(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_select_gemini_button"),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeProvider == AiEngineProvider.GEMINI) CyberCyan else CyberBorder
                                )
                            ) {
                                Text("✨ Gemini", color = CyberCyan)
                            }
                            OutlinedButton(
                                onClick = { onToggleAiProvider(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_select_openai_button"),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeProvider == AiEngineProvider.CHATGPT) CyberEmerald else CyberBorder
                                )
                            ) {
                                Text("🤖 ChatGPT", color = CyberEmerald)
                            }
                        }
                    }
                }

                // Synced Entities Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileSyncMetricBox(
                        label = "المشاريع",
                        value = syncedProjectsCount.toString(),
                        accent = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileSyncMetricBox(
                        label = "تجارب المختبر",
                        value = syncedBioExperimentsCount.toString(),
                        accent = CyberCoral,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileSyncMetricBox(
                        label = "المهارات المثبتة",
                        value = pinnedSkillsCount.toString(),
                        accent = CyberViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CyberBorder)

                Text(
                    text = "المعرف السحابي (UID): ${userId.take(16)}... • الفرع النشط: $activeBranch",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextSecondary
                )

                // Manual Force Cloud Sync Button
                Button(
                    onClick = onForceCloudSync,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeminiPillSelectedContainer,
                        contentColor = GeminiOnPillSelectedContainer
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("force_cloud_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.force_cloud_sync_action),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Switch Google Account & Sign Out Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSwitchGoogleAccount,
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_google_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.switch_google_account_action),
                            color = CyberTextPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sign_out),
                            color = CyberTextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSyncMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CyberSurfaceElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CyberTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusBannerBar(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = GeminiPillSelectedContainer.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("status_banner_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = GeminiOnPillSelectedContainer,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("dismiss_status_banner_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss_banner),
                    tint = GeminiOnPillSelectedContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
