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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily

const val SKILLS_HUB_TAB_INDEX = 5

/**
 * Defines the five primary bottom navigation tabs in the NovaForge AI Studio main screen layout.
 */
enum class StudioBottomTab(
    val index: Int,
    @StringRes val labelResId: Int,
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
 * Main screen Scaffold layout providing:
 * 1. TopAppBar with studio branding, active branch info, a prominent 'Skills' action button,
 *    interactive Google User Profile & Cloud Sync status dialog, and sign-out action.
 * 2. Five-tab Material 3 Bottom NavigationBar (`Dashboard`, `IDE`, `DB & VCS`, `AI & Media`, `Bioscience`).
 * 3. Non-blocking dismissible status banner and BackHandler navigation to the primary Dashboard tab.
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
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
            AnimatedVisibility(visible = statusBanner != null) {
                Surface(
                    color = CyberSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .testTag("main_status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onDismissStatusBanner,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dismiss_status_banner_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.dismiss_banner),
                                color = CyberCyan
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
    val activeAccent = if (isSkillsSelected) CyberCyan else CyberEmerald

    TopAppBar(
        modifier = modifier.testTag("main_top_app_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CyberSurface,
            titleContentColor = CyberCyan
        ),
        title = {
            Column(
                modifier = Modifier
                    .clickable(onClick = onUserProfileClick)
                    .testTag("top_bar_identity_header")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.studio_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isBusy) {
                        Spacer(modifier = Modifier.width(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
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
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$userEmail • branch: $activeBranch",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onSkillsClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(
                    width = if (isSkillsSelected) 1.5.dp else 1.dp,
                    color = activeAccent.copy(alpha = if (isSkillsSelected) 0.9f else 0.55f)
                ),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = activeAccent.copy(alpha = if (isSkillsSelected) 0.22f else 0.12f),
                    contentColor = activeAccent
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .padding(end = 4.dp)
                    .testTag("top_skills_memory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = stringResource(id = R.string.skills_action_content_description),
                    tint = activeAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.skills_action_button_format, skillsCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = activeAccent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (pinnedSkillsCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = CyberCyan.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("top_skills_pinned_badge")
                    ) {
                        Text(
                            text = "★$pinnedSkillsCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(id = R.string.cloud_profile_content_description),
                    tint = CyberCyan
                )
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
                    tint = MaterialTheme.colorScheme.onSurface
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
        AiEngineProvider.DUAL_ENGINE -> CyberAmber
        AiEngineProvider.GEMINI -> CyberCyan
    }

    Surface(
        color = CyberSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, activeAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .testTag("ai_engine_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔷 Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!isChatGptActive) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    text = "🟢 ChatGPT",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isChatGptActive) CyberEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isChatGptActive) FontWeight.Bold else FontWeight.Normal
                )
            }

            Box {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ai_model_dropdown_button")
                ) {
                    Text(
                        text = if (isDualActive) "🤝 Dual Engine" else selectedAiModel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = activeAccent,
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

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .testTag("ai_model_dropdown_menu")
                ) {
                    GeminiAiRepository.SUPPORTED_TEXT_MODELS.forEach { (modelId, label) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selectedAiModel == modelId) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedAiModel == modelId) CyberCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = modelId,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .testTag("cloud_user_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CyberViolet.copy(alpha = 0.25f))
                            .border(1.5.dp, CyberCyan, CircleShape),
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
                            text = userDisplayName.ifBlank { "Google Cloud Developer" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = CyberEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                            color = CyberCyan
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileMetricBox(
                        label = "المشاريع المتزامنة",
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
                        label = "النموذج المفضل",
                        value = selectedAiModel,
                        accent = CyberAmber,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_google_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = null,
                        tint = CyberEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.switch_google_account_action),
                        color = CyberEmerald,
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
                            color = CyberCyan
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
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBackground)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily
            ),
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
