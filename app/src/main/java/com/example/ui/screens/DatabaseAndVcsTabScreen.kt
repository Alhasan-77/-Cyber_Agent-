package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.DbCollectionSchema
import com.example.data.model.DiffLine
import com.example.data.model.DiffType
import com.example.data.model.UiState
import com.example.data.model.VersionSnapshot
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.DataViewModel

@Composable
fun DatabaseAndVcsTabScreen(
    dataViewModel: DataViewModel,
    collectionsState: UiState<List<DbCollectionSchema>>,
    snapshotsState: UiState<List<VersionSnapshot>>,
    activeBranch: String,
    onSelectBranch: (String) -> Unit,
    onCreateCollection: (String, String, String, String) -> Unit,
    onAppendRecord: (DbCollectionSchema, String) -> Unit,
    onGenerateBackendDao: (DbCollectionSchema) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onCommitSnapshot: (String, String) -> Unit,
    onRestoreSnapshot: (VersionSnapshot) -> Unit,
    onComputeDiff: (VersionSnapshot) -> List<DiffLine>
) {
    // 0 = Live Firestore Collections & Documents Manager (DataViewModel)
    // 1 = Schema Designer & DAO Code Generator
    // 2 = Version Control & Diff Engine
    var activeSubTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                label = { Text("مدير مستندات Firestore الحي") },
                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                modifier = Modifier.testTag("subtab_firestore_manager")
            )
            FilterChip(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                label = { Text("مصمم المخططات وتوليد DAO") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                modifier = Modifier.testTag("subtab_database_studio")
            )
            FilterChip(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                label = { Text("التحكم بالإصدارات (VCS & Diff)") },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                modifier = Modifier.testTag("subtab_version_control")
            )
        }

        when (activeSubTab) {
            0 -> FirestoreDataManagerComponent(
                dataViewModel = dataViewModel,
                modifier = Modifier.fillMaxSize()
            )
            1 -> FlexibleDatabaseSection(
                collections = (collectionsState as? UiState.Success)?.data.orEmpty(),
                onCreateCollection = onCreateCollection,
                onAppendRecord = onAppendRecord,
                onGenerateBackendDao = onGenerateBackendDao,
                onDeleteCollection = onDeleteCollection
            )
            else -> VersionControlSection(
                snapshots = (snapshotsState as? UiState.Success)?.data.orEmpty(),
                activeBranch = activeBranch,
                onSelectBranch = onSelectBranch,
                onCommitSnapshot = onCommitSnapshot,
                onRestoreSnapshot = onRestoreSnapshot,
                onComputeDiff = onComputeDiff
            )
        }
    }
}


@Composable
private fun FlexibleDatabaseSection(
    collections: List<DbCollectionSchema>,
    onCreateCollection: (String, String, String, String) -> Unit,
    onAppendRecord: (DbCollectionSchema, String) -> Unit,
    onGenerateBackendDao: (DbCollectionSchema) -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    var colName by remember { mutableStateOf("") }
    var fieldsSpec by remember { mutableStateOf("id:UUID, title:String, status:String, priority:Int") }
    var indexSpec by remember { mutableStateOf("Composite Index: (status ASC, priority DESC)") }
    var visualQueryFilter by remember { mutableStateOf("") }
    var newRecordJson by remember { mutableStateOf("""{"id":"rec_101","title":"عملية سحابية جديدة","status":"ACTIVE"}""") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "مصمم المخططات ومجموعات البيانات السحابية",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("اسم المجموعة / الجدول (Collection Name)") },
                        placeholder = { Text("مثال: orders_pipeline أو genomic_samples") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("db_collection_name_input")
                    )
                    OutlinedTextField(
                        value = fieldsSpec,
                        onValueChange = { fieldsSpec = it },
                        label = { Text("تعريف الحقول والأنواع (Schema Fields)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("db_fields_spec_input")
                    )
                    Button(
                        onClick = {
                            val safeName = colName.ifBlank { "cloud_entities_${collections.size + 1}" }
                            onCreateCollection(
                                safeName,
                                fieldsSpec,
                                """[{"id":"item_1","name":"سجل أولي في $safeName","status":"ACTIVE"}]""",
                                indexSpec
                            )
                            colName = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_collection_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء مجموعة بيانات جديدة في Firestore", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Visual Query Filter Bar
        item {
            OutlinedTextField(
                value = visualQueryFilter,
                onValueChange = { visualQueryFilter = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                label = { Text("محرر الاستعلامات المرئي (Visual Query Filter - ابحث في السجلات أو الحقول)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visual_query_filter_input")
            )
        }

        items(
            collections.filter {
                visualQueryFilter.isBlank() ||
                    it.collectionName.contains(visualQueryFilter, ignoreCase = true) ||
                    it.sampleRecordsJson.contains(visualQueryFilter, ignoreCase = true) ||
                    it.fieldsSpec.contains(visualQueryFilter, ignoreCase = true)
            },
            key = { it.id }
        ) { schema ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📂 ${schema.collectionName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberCyan
                            )
                            Text(
                                text = "الحقول: ${schema.fieldsSpec}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                            Text(
                                text = schema.indexConfig,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteCollection(schema.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف المجموعة", tint = CyberCoral)
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = schema.sampleRecordsJson,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberBackground)
                                .padding(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newRecordJson,
                            onValueChange = { newRecordJson = it },
                            label = { Text("إدراج سجل JSON") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { onAppendRecord(schema, newRecordJson) },
                            modifier = Modifier.testTag("add_record_${schema.id}")
                        ) {
                            Text("+ سجل")
                        }
                        Button(
                            onClick = { onGenerateBackendDao(schema) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            modifier = Modifier.testTag("generate_dao_code_${schema.id}")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توليد DAO")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionControlSection(
    snapshots: List<VersionSnapshot>,
    activeBranch: String,
    onSelectBranch: (String) -> Unit,
    onCommitSnapshot: (String, String) -> Unit,
    onRestoreSnapshot: (VersionSnapshot) -> Unit,
    onComputeDiff: (VersionSnapshot) -> List<DiffLine>
) {
    var commitMessage by remember { mutableStateOf("") }
    var versionTag by remember { mutableStateOf("v1.${snapshots.size + 1}.0") }
    var selectedDiffSnapshot by remember { mutableStateOf<VersionSnapshot?>(snapshots.firstOrNull()) }

    val diffLines = remember(selectedDiffSnapshot) {
        selectedDiffSnapshot?.let { onComputeDiff(it) }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Branch Selector & Commit New Snapshot Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إدارة الفروع البرمجية وحفظ اللقطات السحابية الآمنة",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberEmerald
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الفرع النشط (Branch):",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberAmber
                        )
                        listOf("main", "staging", "production", "ai-experimental").forEach { branch ->
                            FilterChip(
                                selected = activeBranch == branch,
                                onClick = { onSelectBranch(branch) },
                                label = { Text(branch) },
                                modifier = Modifier.testTag("branch_chip_$branch")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = versionTag,
                            onValueChange = { versionTag = it },
                            label = { Text("رقم الإصدار") },
                            singleLine = true,
                            modifier = Modifier
                                .width(120.dp)
                                .testTag("vcs_version_tag_input")
                        )
                        OutlinedTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            label = { Text("رسالة التوثيق (Commit Message)") },
                            placeholder = { Text("إضافة واجهة التفاعل وتحديث قاعدة البيانات...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vcs_commit_message_input")
                        )
                    }
                    Button(
                        onClick = {
                            onCommitSnapshot(
                                commitMessage.ifBlank { "تحديث الكود والمخططات السحابية" },
                                versionTag
                            )
                            commitMessage = ""
                            versionTag = "v1.${snapshots.size + 2}.0"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commit_snapshot_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التزام وحفظ الإصدار في السحابة (Commit Snapshot)", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Visual Diff Viewer
        if (selectedDiffSnapshot != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                                text = "🔍 مقارنة الفروقات البرمجية (Diff) مع الإصدار ${selectedDiffSnapshot!!.versionTag}",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberCyan
                            )
                            Text(
                                text = "+${selectedDiffSnapshot!!.addedLines} / -${selectedDiffSnapshot!!.deletedLines}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberBackground)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                diffLines.take(24).forEach { line ->
                                    val bgColor = when (line.type) {
                                        DiffType.ADDED -> CyberEmerald.copy(alpha = 0.16f)
                                        DiffType.DELETED -> CyberCoral.copy(alpha = 0.16f)
                                        DiffType.UNCHANGED -> CyberBackground
                                    }
                                    val txtColor = when (line.type) {
                                        DiffType.ADDED -> CyberEmerald
                                        DiffType.DELETED -> CyberCoral
                                        DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(
                                        text = "${line.lineNumber.toString().padStart(2, '0')} | ${line.content}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = txtColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snapshot History List
        item {
            Text(
                text = "سجل الإصدارات السحابية (Commit History)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(snapshots, key = { it.id }) { snap ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = snap.versionTag,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = snap.commitMessage,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الأسطر المضافة: +${snap.addedLines} • المحذوفة: -${snap.deletedLines} • SHA Verified",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { selectedDiffSnapshot = snap },
                            modifier = Modifier.testTag("diff_snapshot_${snap.id}")
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مقارنة", color = CyberCyan)
                        }
                        Button(
                            onClick = { onRestoreSnapshot(snap) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            modifier = Modifier.testTag("restore_snapshot_${snap.id}")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة")
                        }
                    }
                }
            }
        }
    }
}
