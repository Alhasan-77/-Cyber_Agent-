package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.skills.ExtractedSkillItem
import com.example.data.skills.GlobalToolConfigItem
import com.example.data.skills.SkillsMemoryCatalog
import com.example.data.skills.SkillsMemoryEngine
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SkillsMemoryHubTabScreen(
    catalog: SkillsMemoryCatalog,
    pinnedSkillIds: Set<String>,
    autoInjectSkillsEnabled: Boolean,
    lastAppliedSkillIds: List<String>,
    isSyncedWithFirestore: Boolean,
    onToggleAutoInjectSkills: (Boolean) -> Unit,
    onTogglePinSkill: (String) -> Unit,
    onSyncSkillsToFirestore: () -> Unit,
    onInjectCodeIntoIde: (code: String, targetTab: String, label: String) -> Unit,
    onApplySkillInAiChat: (ExtractedSkillItem) -> Unit
) {
    val context = LocalContext.current
    var activeSectionTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showPinnedOnly by rememberSaveable { mutableStateOf(false) }

    var selectedSkillId by rememberSaveable {
        mutableStateOf(catalog.skills.firstOrNull()?.id.orEmpty())
    }
    var selectedDetailSubTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedReferenceIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedRawFilePath by rememberSaveable {
        mutableStateOf(catalog.allFilePaths.firstOrNull().orEmpty())
    }
    var copiedBanner by remember { mutableStateOf<String?>(null) }

    val categories = remember(catalog.skills) {
        listOf("ALL") + catalog.skills.map { it.category }.distinct()
    }

    val filteredSkills = remember(catalog.skills, searchQuery, selectedCategory, showPinnedOnly, pinnedSkillIds) {
        catalog.skills.filter { skill ->
            val matchesCategory = selectedCategory == "ALL" || skill.category == selectedCategory
            val matchesPinned = !showPinnedOnly || skill.id in pinnedSkillIds
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                skill.id.lowercase().contains(q) ||
                skill.titleAr.lowercase().contains(q) ||
                skill.descriptionEn.lowercase().contains(q) ||
                skill.keywords.any { it.lowercase().contains(q) } ||
                skill.packages.any { it.lowercase().contains(q) } ||
                skill.references.any { it.fileName.lowercase().contains(q) }
            matchesCategory && matchesPinned && matchesSearch
        }
    }

    val selectedSkill = remember(catalog.skills, selectedSkillId, filteredSkills) {
        catalog.skills.find { it.id == selectedSkillId }
            ?: filteredSkills.firstOrNull()
            ?: catalog.skills.firstOrNull()
    }

    val totalReferencesCount = remember(catalog.skills) {
        catalog.skills.sumOf { it.references.size }
    }
    val totalPackagesCount = remember(catalog.skills) {
        catalog.skills.flatMap { it.packages }.distinct().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(12.dp)
            .testTag("skills_memory_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Memory Status & Auto-Routing Control Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, com.example.ui.theme.GeminiBorderGradient, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ذاكرة مهارات Gemini والأدوات الدائمة (Skills Hub)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    brush = com.example.ui.theme.GeminiSparkleGradient
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${catalog.totalSkills} مهارة محمّلة • $totalReferencesCount مرجع • $totalPackagesCount مكتبة • ${catalog.totalArchiveFiles} ملف • MCP: ${catalog.mcpServerUrl.ifBlank { "غير مهيأ" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onSyncSkillsToFirestore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSyncedWithFirestore) CyberEmerald else CyberCyan
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("sync_skills_firestore_button")
                    ) {
                        Icon(
                            imageVector = if (isSyncedWithFirestore) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = CyberBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncedWithFirestore) "محفوظ في Firestore ✓" else "مزامنة مع Firestore",
                            color = CyberBackground,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Switch(
                            checked = autoInjectSkillsEnabled,
                            onCheckedChange = onToggleAutoInjectSkills,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBackground,
                                checkedTrackColor = CyberEmerald
                            ),
                            modifier = Modifier.testTag("toggle_auto_inject_skills")
                        )
                        Column {
                            Text(
                                text = "التفعيل التلقائي للمهارات في محرر الكود والمساعد الذكي (Auto-Skill Routing)",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "المهارات المثبّتة حالياً (${pinnedSkillIds.size}): ${pinnedSkillIds.take(5).joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (lastAppliedSkillIds.isNotEmpty()) {
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "آخر استدعاء: ${lastAppliedSkillIds.take(3).joinToString(" • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = copiedBanner != null) {
                    Surface(
                        color = CyberSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = copiedBanner.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                            TextButton(onClick = { copiedBanner = null }) {
                                Text("إغلاق", color = CyberCyan)
                            }
                        }
                    }
                }
            }
        }

        // Sub-navigation tabs inside the Memory Hub
        SecondaryScrollableTabRow(
            selectedTabIndex = activeSectionTab,
            containerColor = CyberSurface,
            contentColor = CyberCyan,
            edgePadding = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSectionTab == 0,
                onClick = { activeSectionTab = 0 },
                text = { Text("📚 دليل المهارات الـ 25 (${catalog.skills.size})") },
                modifier = Modifier.testTag("skills_subtab_catalog")
            )
            Tab(
                selected = activeSectionTab == 1,
                onClick = { activeSectionTab = 1 },
                text = { Text("🛠️ أدوات MCP والسكريبتات والـ CI (${catalog.globalToolsAndConfigs.size})") },
                modifier = Modifier.testTag("skills_subtab_tools")
            )
            Tab(
                selected = activeSectionTab == 2,
                onClick = { activeSectionTab = 2 },
                text = { Text("📦 المكتبات وأوامر CLI ($totalPackagesCount)") },
                modifier = Modifier.testTag("skills_subtab_packages")
            )
            Tab(
                selected = activeSectionTab == 3,
                onClick = { activeSectionTab = 3 },
                text = { Text("🗂️ مستكشف ملفات الأرشيف (${catalog.totalArchiveFiles})") },
                modifier = Modifier.testTag("skills_subtab_raw_files")
            )
        }

        when (activeSectionTab) {
            0 -> SkillsInteractiveExplorerSection(
                skills = filteredSkills,
                selectedSkill = selectedSkill,
                pinnedSkillIds = pinnedSkillIds,
                categories = categories,
                selectedCategory = selectedCategory,
                searchQuery = searchQuery,
                showPinnedOnly = showPinnedOnly,
                selectedDetailSubTab = selectedDetailSubTab,
                selectedReferenceIndex = selectedReferenceIndex,
                onSearchQueryChange = { searchQuery = it },
                onSelectCategory = { selectedCategory = it },
                onTogglePinnedFilter = { showPinnedOnly = !showPinnedOnly },
                onSelectSkill = {
                    selectedSkillId = it.id
                    selectedReferenceIndex = 0
                },
                onSelectDetailSubTab = { selectedDetailSubTab = it },
                onSelectReferenceIndex = { selectedReferenceIndex = it },
                onTogglePinSkill = onTogglePinSkill,
                onCopyText = { label, text ->
                    copyToClipboard(context, label, text)
                    copiedBanner = "تم نسخ $label إلى الحافظة بنجاح ✓"
                },
                onInjectCodeIntoIde = { code, tab, label ->
                    onInjectCodeIntoIde(code, tab, label)
                    copiedBanner = "تم حقن $label في محرر الكود ($tab) ✓"
                },
                onApplySkillInAiChat = onApplySkillInAiChat
            )
            1 -> GlobalToolsAndMcpSection(
                tools = catalog.globalToolsAndConfigs,
                mcpServerUrl = catalog.mcpServerUrl,
                onCopyText = { label, text ->
                    copyToClipboard(context, label, text)
                    copiedBanner = "تم نسخ $label إلى الحافظة ✓"
                },
                onInjectIntoIde = { code, label ->
                    onInjectCodeIntoIde(code, "Backend", label)
                    copiedBanner = "تم حقن $label في تبويب Backend بالمحرر ✓"
                }
            )
            2 -> ExtractedPackagesAndCliSection(
                skills = catalog.skills,
                onCopyText = { label, text ->
                    copyToClipboard(context, label, text)
                    copiedBanner = "تم نسخ $label إلى الحافظة ✓"
                },
                onSelectSkillAndOpen = { skillId ->
                    selectedSkillId = skillId
                    activeSectionTab = 0
                }
            )
            3 -> RawArchiveFileBrowserSection(
                context = context,
                allFilePaths = catalog.allFilePaths,
                selectedPath = selectedRawFilePath,
                onSelectPath = { selectedRawFilePath = it },
                onCopyText = { label, text ->
                    copyToClipboard(context, label, text)
                    copiedBanner = "تم نسخ محتوى الملف $label ✓"
                },
                onInjectIntoIde = { code, path ->
                    val targetTab = when {
                        path.endsWith(".html") -> "HTML"
                        path.endsWith(".css") -> "CSS"
                        path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".tsx") || path.endsWith(".mjs") -> "JS"
                        else -> "Backend"
                    }
                    onInjectCodeIntoIde(code, targetTab, path)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SkillsInteractiveExplorerSection(
    skills: List<ExtractedSkillItem>,
    selectedSkill: ExtractedSkillItem?,
    pinnedSkillIds: Set<String>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    showPinnedOnly: Boolean,
    selectedDetailSubTab: Int,
    selectedReferenceIndex: Int,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onTogglePinnedFilter: () -> Unit,
    onSelectSkill: (ExtractedSkillItem) -> Unit,
    onSelectDetailSubTab: (Int) -> Unit,
    onSelectReferenceIndex: (Int) -> Unit,
    onTogglePinSkill: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onInjectCodeIntoIde: (String, String, String) -> Unit,
    onApplySkillInAiChat: (ExtractedSkillItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search & Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("ابحث في ${catalog.totalSkills} مهارة محمّلة أو في الحزم والمراجع...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("skills_search_input")
            )
            FilterChip(
                selected = showPinnedOnly,
                onClick = onTogglePinnedFilter,
                label = { Text("المثبّتة فقط (${pinnedSkillIds.size})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                    selectedLabelColor = CyberCyan
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = {
                        Text(
                            text = if (cat == "ALL") "جميع الفئات (All)" else cat,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // Split Sidebar (Skills List) + Central Detail Panel
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left/Start Sidebar: Skills List
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(skills, key = { it.id }) { skill ->
                        val isSelected = selectedSkill?.id == skill.id
                        val isPinned = skill.id in pinnedSkillIds
                        Surface(
                            color = if (isSelected) CyberSurfaceElevated else CyberBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else CyberBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectSkill(skill) }
                                .testTag("skill_item_${skill.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = skill.id,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onTogglePinSkill(skill.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "تثبيت المهارة في الذاكرة",
                                            tint = if (isPinned) CyberEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = skill.titleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📄 ${skill.references.size} مراجع",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberAmber
                                    )
                                    if (skill.scripts.isNotEmpty()) {
                                        Text(
                                            text = "⚙️ ${skill.scripts.size} سكريبت",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberViolet
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Central/Right Panel: Selected Skill Details, References, Scripts & Code Snippets
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                if (selectedSkill == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("اختر مهارة من القائمة لاستعراض محتواها ومراجعها", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val isPinned = selectedSkill.id in pinnedSkillIds
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Skill Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedSkill.id,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberCyan
                                    )
                                    Surface(
                                        color = CyberViolet.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = selectedSkill.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = selectedSkill.titleAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onTogglePinSkill(selectedSkill.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("pin_selected_skill_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Default.CheckCircle else Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = if (isPinned) CyberEmerald else CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPinned) "مثبّتة في الذاكرة" else "تثبيت دائماً",
                                        color = if (isPinned) CyberEmerald else CyberCyan,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Button(
                                    onClick = { onApplySkillInAiChat(selectedSkill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("apply_skill_ai_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyberBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "استخدام مع المساعد",
                                        color = CyberBackground,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Sub-tabs for Selected Skill: SKILL.md | References | Code Snippets | Scripts
                        SecondaryScrollableTabRow(
                            selectedTabIndex = selectedDetailSubTab,
                            containerColor = CyberBackground,
                            contentColor = CyberCyan,
                            edgePadding = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Tab(
                                selected = selectedDetailSubTab == 0,
                                onClick = { onSelectDetailSubTab(0) },
                                text = { Text("📘 دليل SKILL.md") }
                            )
                            Tab(
                                selected = selectedDetailSubTab == 1,
                                onClick = { onSelectDetailSubTab(1) },
                                text = { Text("📑 المراجع الفرعية (${selectedSkill.references.size})") }
                            )
                            Tab(
                                selected = selectedDetailSubTab == 2,
                                onClick = { onSelectDetailSubTab(2) },
                                text = { Text("💻 القوالب والأكواد (${selectedSkill.codeSnippets.size})") }
                            )
                            Tab(
                                selected = selectedDetailSubTab == 3,
                                onClick = { onSelectDetailSubTab(3) },
                                text = { Text("⚙️ السكريبتات والحزم (${selectedSkill.scripts.size + selectedSkill.packages.size})") }
                            )
                        }

                        // Detail Body
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedDetailSubTab) {
                                0 -> Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedSkill.descriptionEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyberAmber
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                onCopyText(selectedSkill.id, selectedSkill.skillMarkdown)
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ SKILL.md بالكامل", color = CyberEmerald)
                                        }
                                        TextButton(
                                            onClick = {
                                                onInjectCodeIntoIde(
                                                    "/* === SKILL: ${selectedSkill.id} (${selectedSkill.titleAr}) ===\n${selectedSkill.skillMarkdown}\n*/",
                                                    "Backend",
                                                    selectedSkill.id
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Default.Code, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حقن في المحرر", color = CyberCyan)
                                        }
                                    }
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CyberBackground)
                                                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = selectedSkill.skillMarkdown,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 11.5.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                1 -> {
                                    if (selectedSkill.references.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "جميع القواعد والأمثلة لهذه المهارة مدمجة مباشرة في ملف SKILL.md الرئيسي.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        val safeIdx = selectedReferenceIndex.coerceIn(0, selectedSkill.references.lastIndex)
                                        val activeRef = selectedSkill.references[safeIdx]
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                selectedSkill.references.forEachIndexed { idx, refDoc ->
                                                    FilterChip(
                                                        selected = safeIdx == idx,
                                                        onClick = { onSelectReferenceIndex(idx) },
                                                        label = { Text(refDoc.fileName) }
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = activeRef.path,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                    color = CyberAmber
                                                )
                                                Row {
                                                    TextButton(
                                                        onClick = { onCopyText(activeRef.fileName, activeRef.content) }
                                                    ) {
                                                        Text("نسخ المرجع", color = CyberEmerald)
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            val codeToInject = activeRef.codeBlocks.firstOrNull()?.code
                                                                ?: activeRef.content
                                                            onInjectCodeIntoIde(
                                                                codeToInject,
                                                                "JS",
                                                                activeRef.fileName
                                                            )
                                                        }
                                                    ) {
                                                        Text("حقن الكود في IDE", color = CyberCyan)
                                                    }
                                                }
                                            }

                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(CyberBackground)
                                                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = activeRef.content,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            fontSize = 11.5.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    val allSnippets = remember(selectedSkill) {
                                        (selectedSkill.codeSnippets + selectedSkill.references.flatMap { it.codeBlocks }).take(20)
                                    }
                                    if (allSnippets.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("لا توجد مقاطع كود منفصلة؛ راجع تبويب SKILL.md.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(allSnippets.size) { index ->
                                                val snippet = allSnippets[index]
                                                Card(
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = CardDefaults.cardColors(containerColor = CyberBackground),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Snippet #${index + 1} (${snippet.language})",
                                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                                                color = CyberCyan
                                                            )
                                                            Row {
                                                                TextButton(
                                                                    onClick = { onCopyText("Snippet #${index + 1}", snippet.code) }
                                                                ) {
                                                                    Text("نسخ", color = CyberEmerald)
                                                                }
                                                                TextButton(
                                                                    onClick = {
                                                                        val targetTab = when (snippet.language.lowercase()) {
                                                                            "html" -> "HTML"
                                                                            "css" -> "CSS"
                                                                            "js", "jsx", "ts", "tsx", "javascript", "typescript" -> "JS"
                                                                            else -> "Backend"
                                                                        }
                                                                        onInjectCodeIntoIde(
                                                                            snippet.code,
                                                                            targetTab,
                                                                            "${selectedSkill.id} Snippet #${index + 1}"
                                                                        )
                                                                    }
                                                                ) {
                                                                    Text("حقن في IDE", color = CyberCyan)
                                                                }
                                                            }
                                                        }
                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                            Text(
                                                                text = snippet.code,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = JetBrainsMonoFontFamily,
                                                                    fontSize = 11.sp
                                                                ),
                                                                color = CyberEmerald,
                                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                3 -> Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "📦 الحزم والمكتبات المستخرجة (${selectedSkill.packages.size}):",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        selectedSkill.packages.forEach { pkg ->
                                            AssistChip(
                                                onClick = { onCopyText(pkg, "npx expo install $pkg") },
                                                label = {
                                                    Text(
                                                        text = pkg,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = CyberBackground,
                                                    labelColor = CyberEmerald
                                                )
                                            )
                                        }
                                    }

                                    if (selectedSkill.cliCommands.isNotEmpty()) {
                                        HorizontalDivider(color = CyberBorder)
                                        Text(
                                            text = "⌨️ أوامر التشغيل والأدوات (CLI Commands):",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CyberAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                        selectedSkill.cliCommands.forEach { cmd ->
                                            Surface(
                                                color = CyberBackground,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                                    .clickable { onCopyText("CLI Command", cmd) }
                                            ) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        text = "$ $cmd",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                        color = CyberCyan,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (selectedSkill.scripts.isNotEmpty()) {
                                        HorizontalDivider(color = CyberBorder)
                                        Text(
                                            text = "⚙️ سكريبتات الأتمتة المرفقة (${selectedSkill.scripts.size}):",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CyberViolet,
                                            fontWeight = FontWeight.Bold
                                        )
                                        selectedSkill.scripts.forEach { script ->
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = CyberBackground),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = script.fileName,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                                            color = CyberCyan
                                                        )
                                                        Row {
                                                            TextButton(onClick = { onCopyText(script.fileName, script.content) }) {
                                                                Text("نسخ السكريبت", color = CyberEmerald)
                                                            }
                                                            TextButton(
                                                                onClick = {
                                                                    onInjectCodeIntoIde(script.content, "Backend", script.fileName)
                                                                }
                                                            ) {
                                                                Text("حقن في IDE", color = CyberCyan)
                                                            }
                                                        }
                                                    }
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                        Text(
                                                            text = script.content.take(1500),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = JetBrainsMonoFontFamily,
                                                                fontSize = 11.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalToolsAndMcpSection(
    tools: List<GlobalToolConfigItem>,
    mcpServerUrl: String,
    onCopyText: (String, String) -> Unit,
    onInjectIntoIde: (String, String) -> Unit
) {
    var selectedTool by remember(tools) { mutableStateOf(tools.firstOrNull()) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tools, key = { it.path }) { item ->
                    val isSelected = selectedTool?.path == item.path
                    Surface(
                        color = if (isSelected) CyberSurfaceElevated else CyberBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberCyan else CyberBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTool = item }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .border(1.dp, CyberViolet.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
        ) {
            val active = selectedTool
            if (active != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = active.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MCP (مرجع إعداد فقط): ${mcpServerUrl.ifBlank { "غير مهيأ" }} • Type: ${active.type}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmerald
                            )
                        }
                        Row {
                            TextButton(onClick = { onCopyText(active.name, active.content) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", color = CyberEmerald)
                            }
                            Button(
                                onClick = { onInjectIntoIde(active.content, active.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("حقن في المحرر", color = CyberBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberBackground)
                                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = active.content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtractedPackagesAndCliSection(
    skills: List<ExtractedSkillItem>,
    onCopyText: (String, String) -> Unit,
    onSelectSkillAndOpen: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(skills.filter { it.packages.isNotEmpty() || it.cliCommands.isNotEmpty() }, key = { it.id }) { skill ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${skill.id} — ${skill.titleAr}",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = skill.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber
                            )
                        }
                        TextButton(onClick = { onSelectSkillAndOpen(skill.id) }) {
                            Text("فتح المهارة ←", color = CyberEmerald)
                        }
                    }

                    if (skill.packages.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            skill.packages.forEach { pkg ->
                                AssistChip(
                                    onClick = { onCopyText(pkg, "npx expo install $pkg") },
                                    label = {
                                        Text(
                                            text = pkg,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Hub, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }

                    if (skill.cliCommands.isNotEmpty()) {
                        skill.cliCommands.take(4).forEach { cmd ->
                            Surface(
                                color = CyberBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCopyText("CLI Command", cmd) }
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cmd,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = CyberEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RawArchiveFileBrowserSection(
    context: Context,
    allFilePaths: List<String>,
    selectedPath: String,
    onSelectPath: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onInjectIntoIde: (String, String) -> Unit
) {
    var filterText by rememberSaveable { mutableStateOf("") }
    val filteredPaths = remember(allFilePaths, filterText) {
        val q = filterText.trim().lowercase()
        if (q.isEmpty()) allFilePaths else allFilePaths.filter { it.lowercase().contains(q) }
    }

    val rawContent = remember(selectedPath) {
        if (selectedPath.isBlank()) ""
        else if (selectedPath.endsWith(".png")) "[Binary Image Asset in Archive: $selectedPath]"
        else SkillsMemoryEngine.readRawArchiveFile(context, selectedPath)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("ابحث في جميع ملفات الأرشيف (${allFilePaths.size} ملف)...") },
            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = CyberCyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredPaths, key = { it }) { path ->
                        val isSelected = path == selectedPath
                        val shortName = path.removePrefix("skills-main/")
                        Surface(
                            color = if (isSelected) CyberSurfaceElevated else CyberBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPath(path) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = shortName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = selectedPath.removePrefix("skills-main/"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CyberCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row {
                            TextButton(onClick = { onCopyText(selectedPath, rawContent) }) {
                                Text("نسخ الملف", color = CyberEmerald)
                            }
                            TextButton(onClick = { onInjectIntoIde(rawContent, selectedPath) }) {
                                Text("حقن في IDE", color = CyberCyan)
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberBackground)
                                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = rawContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
