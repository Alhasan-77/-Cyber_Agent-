package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.GeminiBorderGradient
import com.example.ui.theme.GeminiOnPillSelectedContainer
import com.example.ui.theme.GeminiPillSelectedContainer
import com.example.ui.theme.GeminiSparkleGradient
import com.example.ui.theme.JetBrainsMonoFontFamily

const val SKILLS_HUB_TAB_INDEX = 5

/**
 * Defines the five primary bottom navigation tabs in the Gemini Studio main screen layout.
 */
enum class StudioBottomTab(
    val index: Int,
    @param:StringRes val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        index = 0,
        labelResId = R.string.tab_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_tab_dashboard"
    ),
    IDE(
        index = 1,
        labelResId = R.string.tab_ide,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code,
        testTag = "nav_tab_ide"
    ),
    DB_VCS(
        index = 2,
        labelResId = R.string.tab_db_vcs,
        selectedIcon = Icons.Filled.Storage,
        unselectedIcon = Icons.Outlined.Storage,
        testTag = "nav_tab_db_vcs"
    ),
    AI_MEDIA(
        index = 3,
        labelResId = R.string.tab_ai_media,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_ai_media"
    ),
    BIOSCIENCE(
        index = 4,
        labelResId = R.string.tab_bioscience,
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science,
        testTag = "nav_tab_bioscience"
    )
}

/**
 * Main screen Scaffold layout styled after the official Google Gemini Android app:
 * 1. Flat Obsidian TopAppBar (`#131314`) with the Gemini 4-point sparkle emblem, gradient branding,
 *    pill-shaped 'Skills' button, Google account avatar ring, and sign-out action.
 * 2. Pill-shaped Gemini Model & Provider Switcher Bar (`#1E1F20`).
 * 3. Five-tab Material 3 Gemini Bottom NavigationBar (`#1E1F20` with `#004A77` pill indicators).
 */
@Composable
fun MainScreenLayout(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    userEmail: String,
    activeBranch: String,
    skillsCount: Int,
    pinnedSkillsCount: Int = 0,
    isBusy: Boolean = false,
    statusBanner: String? = null,
    onDismissStatusBanner: () -> Unit = {},
    onSignOut: () -> Unit = {},
    userDisplayName: String = "NovaForge Developer",
    userId: String = "",
    syncedProjectsCount: Int = 0,
    syncedBioExperimentsCount: Int = 0,
    syncLatencyMs: Long = 16L,
    lastSyncTimestampLabel: String = "الآن",
    selectedAiModel: String = GeminiAiRepository.MODEL_DUAL_ENGINE,
    onSelectAiModel: (String) -> Unit = {},
    onToggleAiProvider: (Boolean) -> Unit = {},
    onForceCloudSync: () -> Unit = {},
    onSwitchGoogleAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != StudioBottomTab.DASHBOARD.index) {
        onSelectTab(StudioBottomTab.DASHBOARD.index)
    }

    if (showProfileDialog) {
        CloudUserProfileDialog(
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            userId = userId,
            activeBranch = activeBranch,
            syncedProjectsCount = syncedProjectsCount,
            syncedBioExperimentsCount = syncedBioExperimentsCount,
            syncLatencyMs = syncLatencyMs,
            lastSyncTimestampLabel = lastSyncTimestampLabel,
            selectedAiModel = selectedAiModel,
            onSelectAiModel = onSelectAiModel,
            isBusy = isBusy,
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberBackground,
        topBar = {
            StudioTopAppBar(
                userEmail = userEmail,
                activeBranch = activeBranch,
                skillsCount = skillsCount,
                pinnedSkillsCount = pinnedSkillsCount,
                isSkillsSelected = selectedTab == SKILLS_HUB_TAB_INDEX,
                isBusy = isBusy,
                onSkillsClick = { onSelectTab(SKILLS_HUB_TAB_INDEX) },
                onUserProfileClick = { showProfileDialog = true },
                onSignOutClick = onSignOut
            )
        },
        bottomBar = {
            StudioBottomNavigationBar(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AiEngineControlBar(
                selectedAiModel = selectedAiModel,
                onSelectAiModel = onSelectAiModel,
                onToggleAiProvider = onToggleAiProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            AnimatedVisibility(visible = statusBanner != null) {
                Surface(
                    color = CyberSurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .testTag("main_status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusBanner.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberTextPrimary
                            )
                        }
                        TextButton(
                            onClick = onDismissStatusBanner,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dismiss_status_banner_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.dismiss_banner),
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("main_screen_content_container")
            ) {
                content(selectedTab)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopAppBar(
    userEmail: String,
    activeBranch: String,
    skillsCount: Int,
    pinnedSkillsCount: Int,
    isSkillsSelected: Boolean,
    isBusy: Boolean,
    onSkillsClick: () -> Unit,
    onUserProfileClick: () -> Unit = {},
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("main_top_app_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CyberBackground,
            titleContentColor = CyberTextPrimary
        ),
        title = {
            Row(
                modifier = Modifier
                    .clickable(onClick = onUserProfileClick)
                    .testTag("top_bar_identity_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gemini 4-point Sparkle Orb
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
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.studio_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = GeminiSparkleGradient
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isBusy) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(15.dp)
                                    .testTag("top_bar_busy_indicator"),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$userEmail • $activeBranch",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onSkillsClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSkillsSelected) CyberCyan else CyberBorder
                ),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSkillsSelected) GeminiPillSelectedContainer else CyberSurface,
                    contentColor = if (isSkillsSelected) GeminiOnPillSelectedContainer else CyberTextPrimary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .padding(end = 4.dp)
                    .testTag("top_skills_memory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = stringResource(id = R.string.skills_action_content_description),
                    tint = if (isSkillsSelected) GeminiOnPillSelectedContainer else CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.skills_action_button_format, skillsCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSkillsSelected) GeminiOnPillSelectedContainer else CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (pinnedSkillsCount > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Surface(
                        color = CyberViolet.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("top_skills_pinned_badge")
                    ) {
                        Text(
                            text = "★$pinnedSkillsCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberViolet,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onUserProfileClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_user_profile_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated)
                        .border(1.5.dp, GeminiSparkleGradient, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(id = R.string.cloud_profile_content_description),
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(id = R.string.sign_out),
                    tint = CyberTextSecondary
                )
            }
        }
    )
}

@Composable
fun AiEngineControlBar(
    selectedAiModel: String,
    onSelectAiModel: (String) -> Unit,
    onToggleAiProvider: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isChatGptActive = GeminiAiRepository.isOpenAiModel(selectedAiModel)
    val isDualActive = GeminiAiRepository.isDualEngineModel(selectedAiModel)
    val activeProvider = AiEngineProvider.fromModelId(selectedAiModel)
    val activeAccent = when (activeProvider) {
        AiEngineProvider.CHATGPT -> CyberEmerald
        AiEngineProvider.DUAL_ENGINE -> CyberViolet
        AiEngineProvider.GEMINI -> CyberCyan
    }

    Surface(
        color = CyberSurface,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .border(1.dp, CyberBorder, RoundedCornerShape(50))
            .testTag("ai_engine_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✦ Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!isChatGptActive) CyberCyan else CyberTextSecondary,
                    fontWeight = if (!isChatGptActive) FontWeight.Bold else FontWeight.Normal
                )
                Switch(
                    checked = isChatGptActive,
                    onCheckedChange = { useChatGpt ->
                        onToggleAiProvider(useChatGpt)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackground,
                        checkedTrackColor = CyberEmerald,
                        uncheckedThumbColor = CyberBackground,
                        uncheckedTrackColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("ai_provider_toggle")
                )
                Text(
                    text = "ChatGPT",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isChatGptActive) CyberEmerald else CyberTextSecondary,
                    fontWeight = if (isChatGptActive) FontWeight.Bold else FontWeight.Normal
                )
            }

            Box {
                Surface(
                    onClick = { dropdownExpanded = true },
                    color = CyberSurfaceElevated,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ai_model_dropdown_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDualActive) "Dual Engine" else selectedAiModel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "اختيار نموذج الذكاء الاصطناعي",
                            tint = activeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                        .testTag("ai_model_dropdown_menu")
                ) {
                    GeminiAiRepository.SUPPORTED_TEXT_MODELS.forEach { (modelId, label) ->
                        val isCurrent = selectedAiModel == modelId
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) CyberCyan else CyberTextPrimary
                                        )
                                        Text(
                                            text = modelId,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = CyberTextSecondary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
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
fun CloudUserProfileDialog(
    userDisplayName: String,
    userEmail: String,
    userId: String,
    activeBranch: String,
    syncedProjectsCount: Int,
    syncedBioExperimentsCount: Int,
    syncLatencyMs: Long,
    lastSyncTimestampLabel: String,
    selectedAiModel: String = GeminiAiRepository.MODEL_DUAL_ENGINE,
    onSelectAiModel: (String) -> Unit = {},
    isBusy: Boolean,
    onForceCloudSync: () -> Unit,
    onSwitchGoogleAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GeminiBorderGradient, RoundedCornerShape(28.dp))
                .testTag("cloud_user_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceElevated)
                            .border(2.dp, GeminiSparkleGradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userDisplayName.firstOrNull()?.uppercase()
                                ?: userEmail.firstOrNull()?.uppercase()
                                ?: "G",
                            style = MaterialTheme.typography.titleLarge,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userDisplayName.ifBlank { "Google Gemini Developer" },
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan
                        )
                        if (userId.isNotBlank()) {
                            Text(
                                text = "UID: ${userId.take(18)}...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = CyberTextSecondary
                            )
                        }
                    }
                }

                Surface(
                    color = CyberSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.cloud_sync_active_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${syncLatencyMs}ms • $lastSyncTimestampLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = CyberTextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileMetricBox(
                        label = "المشاريع",
                        value = syncedProjectsCount.toString(),
                        accent = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileMetricBox(
                        label = "التجارب الحيوية",
                        value = syncedBioExperimentsCount.toString(),
                        accent = CyberEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileMetricBox(
                        label = "المحرك النشط",
                        value = selectedAiModel,
                        accent = CyberViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                AiEngineControlBar(
                    selectedAiModel = selectedAiModel,
                    onSelectAiModel = onSelectAiModel,
                    onToggleAiProvider = { useGpt ->
                        onSelectAiModel(
                            if (useGpt) GeminiAiRepository.MODEL_GPT_4O else GeminiAiRepository.MODEL_FLASH
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = CyberBorder)

                Button(
                    onClick = onForceCloudSync,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("force_cloud_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = CyberBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.force_cloud_sync_action),
                        color = CyberBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSwitchGoogleAccount,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_google_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = null,
                        tint = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.switch_google_account_action),
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("profile_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = CyberCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.sign_out),
                            color = CyberCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_profile_dialog_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.dismiss_banner),
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily
            ),
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CyberTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StudioBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .border(width = 0.5.dp, color = CyberBorder)
            .testTag("main_bottom_navigation_bar"),
        containerColor = CyberSurface,
        windowInsets = WindowInsets.navigationBars
    ) {
        StudioBottomTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab.index
            val tabLabel = stringResource(id = tab.labelResId)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab.index) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tabLabel
                    )
                },
                label = {
                    Text(
                        text = tabLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GeminiOnPillSelectedContainer,
                    selectedTextColor = CyberTextPrimary,
                    indicatorColor = GeminiPillSelectedContainer,
                    unselectedIconColor = CyberTextSecondary,
                    unselectedTextColor = CyberTextSecondary
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
