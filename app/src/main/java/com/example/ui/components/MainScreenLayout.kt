package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface

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
 * 1. TopAppBar with studio branding, active branch info, a prominent 'Skills' action button, and sign-out action.
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
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    BackHandler(enabled = selectedTab != StudioBottomTab.DASHBOARD.index) {
        onSelectTab(StudioBottomTab.DASHBOARD.index)
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
            Column {
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
                Text(
                    text = "$userEmail • branch: $activeBranch",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
