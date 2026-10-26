package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.CyberViolet
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

    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val snapshots = (snapshotsState as? UiState.Success)?.data.orEmpty()
    val collections = (collectionsState as? UiState.Success)?.data.orEmpty()
    val logs = (buildLogsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner & Cloud Status
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero),
                            contentDescription = "Cloud Studio Hero",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(CyberBackground.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "البيئة السحابية متصلة • الفرع: $activeBranch",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "لوحة التحكم الشاملة ومراقبة العمليات",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "إدارة المشاريع Full-Stack، مراقبة الكود الفورية، وتوليد التقارير",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showNewProjectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.testTag("create_project_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CyberBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشروع جديد", color = CyberBackground, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Cloud CI/CD & Security Audit Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRunSecurityAudit,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("run_security_audit_button")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص أمني للكود", color = CyberCyan)
                            }
                            Button(
                                onClick = onTriggerCloudDeploy,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trigger_cloud_deploy_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نشر سحابي فوري", color = CyberBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Telemetry Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Code,
                    label = "المشاريع النشطة",
                    value = "${projects.size}",
                    accent = CyberCyan
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    label = "جداول البيانات",
                    value = "${collections.size}",
                    accent = CyberViolet
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    label = "لقطات الإصدارات",
                    value = "${snapshots.size}",
                    accent = CyberEmerald
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "متوسط البناء",
                    value = "${logs.firstOrNull()?.durationMs ?: 210}ms",
                    accent = CyberAmber
                )
            }
        }

        // Projects Section Header
        item {
            Text(
                text = "مشاريعك السحابية (Full-Stack Projects)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "جاري تجهيز مساحة العمل السحابية أو لا توجد مشاريع بعد.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                val isSelected = project.id == selectedProjectId
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyberSurfaceElevated else CyberSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CyberCyan else CyberBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectProjectAndOpenIde(project) }
                        .testTag("project_card_${project.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = project.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "البيئة: ${project.language}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                            OutlinedButton(
                                onClick = { onSelectProjectAndOpenIde(project) },
                                modifier = Modifier.testTag("open_ide_button_${project.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح المحرر والمعاينة", color = CyberCyan)
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Code Monitoring & Detailed Reports Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "نظام المراقبة الفورية للكود والتقارير التفصيلية (Live Telemetry & Audit Reports)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(logs.take(8), key = { it.id }) { log ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${log.durationMs}ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                            IconButton(
                                onClick = { onReadReportAloud(log.reportSummary) },
                                modifier = Modifier.testTag("tts_report_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "قراءة التقرير صوتياً عبر gemini-3.8-flash-tts",
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBackground)
                            .padding(10.dp)
                    )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        title = {
            Text("إنشاء مشروع Full-Stack جديد بالذكاء الاصطناعي")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المشروع") },
                    placeholder = { Text("مثال: نظام إدارة المستشفيات الذكي") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المميزات المطلوبة") },
                    placeholder = { Text("لوحة تحكم، قاعدة بيانات، وتقارير فورية...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_desc_input")
                )
                Text(
                    text = "اختر لغات البرمجة والبيئة:",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) }
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
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("توليد وبناء المشروع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
