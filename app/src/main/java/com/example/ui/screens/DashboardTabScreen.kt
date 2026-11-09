package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BuildExecutionLog
import com.example.data.model.DbCollectionSchema
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.VersionSnapshot
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardTabScreen(
    projectsState: UiState<List<StudioProject>>,
    snapshotsState: UiState<List<VersionSnapshot>>,
    collectionsState: UiState<List<DbCollectionSchema>>,
    buildLogsState: UiState<List<BuildExecutionLog>>,
    selectedProjectId: String?,
    activeBranch: String,
    onSelectProjectAndOpenIde: (StudioProject) -> Unit,
    onCreateNewProject: (String, String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onReadReportAloud: (String) -> Unit,
    onTriggerCloudDeploy: () -> Unit,
    onRunSecurityAudit: () -> Unit
) {
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var dashboardFilter by remember { mutableStateOf("ALL") } // ALL, PROJECTS, LOGS

    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val snapshots = (snapshotsState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()
    val logs = (buildLogsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Authentic Gemini Welcome Hero Card
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeminiBorderGradient, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Text(
                                    text = "الفرع: $activeBranch • بيانات الحساب السحابية",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberTextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GeminiSparkleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Gemini Gradient Greeting Title
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "مرحباً، ماذا سنبني اليوم؟",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                brush = GeminiSparkleGradient
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مساحة عمل Google Gemini الموحدة لتطوير المشاريع Full-Stack، فحص الأمان، ومراقبة الأداء الفوري.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextSecondary
                        )
                    }

                    // Subtle Visual Banner Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero),
                            contentDescription = "Gemini Cloud Studio",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CyberBackground.copy(alpha = 0.35f))
                        )
                    }

                    // Gemini Action Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showNewProjectDialog = true },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GeminiPillSelectedContainer,
                                contentColor = GeminiOnPillSelectedContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("create_project_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = GeminiOnPillSelectedContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشروع جديد",
                                color = GeminiOnPillSelectedContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onRunSecurityAudit,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CyberBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberSurfaceElevated,
                                contentColor = CyberTextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_security_audit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "مراجعة الكود",
                                color = CyberTextPrimary,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onTriggerCloudDeploy,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = CyberBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trigger_cloud_deploy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = CyberBackground,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "حالة النشر",
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Gemini Minimalist Metrics Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Code,
                    label = "المشاريع",
                    value = "${projects.size}",
                    accent = CyberCyan
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    label = "الجداول",
                    value = "${collections.size}",
                    accent = CyberViolet
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    label = "اللقطات",
                    value = "${snapshots.size}",
                    accent = CyberEmerald
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "سرعة البناء",
                    value = "${logs.firstOrNull()?.durationMs ?: 210}ms",
                    accent = CyberAmber
                )
            }
        }

        // 3. Gemini Segmented Section Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مساحة العمل السحابية",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ALL" to "الكل",
                        "PROJECTS" to "المشاريع (${projects.size})",
                        "LOGS" to "السجل (${logs.size})"
                    ).forEach { (key, label) ->
                        val selected = dashboardFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { dashboardFilter = key },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CyberSurface,
                                labelColor = CyberTextSecondary,
                                selectedContainerColor = GeminiPillSelectedContainer,
                                selectedLabelColor = GeminiOnPillSelectedContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = CyberBorder,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        // 4. Cloud Projects List
        if (dashboardFilter == "ALL" || dashboardFilter == "PROJECTS") {
            if (projects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا توجد مشاريع بعد — اضغط على «مشروع جديد» للبدء مع Gemini.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val isSelected = project.id == selectedProjectId
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberSurfaceElevated else CyberSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyberCyan else CyberBorder,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { onSelectProjectAndOpenIde(project) }
                            .testTag("project_card_${project.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) GeminiPillSelectedContainer else CyberSurfaceElevated
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = if (isSelected) GeminiOnPillSelectedContainer else CyberCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CyberTextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = project.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CyberTextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteProject(project.id) },
                                    modifier = Modifier.testTag("delete_project_${project.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف المشروع",
                                        tint = CyberCoral
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = CyberSurfaceElevated,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, CyberBorder)
                                ) {
                                    Text(
                                        text = project.language,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberEmerald,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Button(
                                    onClick = { onSelectProjectAndOpenIde(project) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeminiPillSelectedContainer,
                                        contentColor = GeminiOnPillSelectedContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("open_ide_button_${project.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = GeminiOnPillSelectedContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "فتح المحرر والمعاينة",
                                        color = GeminiOnPillSelectedContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Real-Time Code Monitoring & Detailed Reports Section
        if (dashboardFilter == "ALL" || dashboardFilter == "LOGS") {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "سجل المراقبة الفورية والتقارير الذكية",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(logs.take(8), key = { it.id }) { log ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (log.status == "SUCCESS") CyberEmerald else CyberAmber
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.stage,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = CyberSurfaceElevated,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${log.durationMs}ms",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onReadReportAloud(log.reportSummary) },
                                    modifier = Modifier.testTag("tts_report_${log.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "قراءة التقرير صوتياً باستخدام قارئ Android",
                                        tint = CyberCyan
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.reportSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = CyberTextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CyberSurfaceElevated)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name, desc, lang ->
                showNewProjectDialog = false
                onCreateNewProject(name, desc, lang)
            }
        )
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = CyberTextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val languages = listOf(
        "Full-Stack Web (React/TS)",
        "Python + FastAPI + SQL",
        "Kotlin + Jetpack Compose",
        "Node.js + GraphQL",
        "Bioscience Python Pipeline"
    )
    var selectedLang by remember { mutableStateOf(languages.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyberCyan
                )
                Text(
                    text = "إنشاء مشروع جديد مع Gemini",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المشروع") },
                    placeholder = { Text("مثال: نظام إدارة المستشفيات الذكي") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المميزات المطلوبة") },
                    placeholder = { Text("لوحة تحكم، قاعدة بيانات، وتقارير فورية...") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_desc_input")
                )
                Text(
                    text = "اختر بيئة العمل:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeName = name.ifBlank { "تطبيق سحابي ذكي" }
                    val safeDesc = description.ifBlank { "تطبيق Full-Stack متكامل" }
                    onConfirm(safeName, safeDesc, selectedLang)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text(
                    text = "توليد وبناء المشروع",
                    color = CyberBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = CyberTextSecondary)
            }
        }
    )
}
