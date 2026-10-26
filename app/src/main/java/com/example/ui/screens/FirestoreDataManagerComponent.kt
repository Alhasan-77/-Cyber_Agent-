package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FirestoreCollectionInfo
import com.example.data.model.FirestoreConnectionStatus
import com.example.data.model.FirestoreDocumentItem
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
fun FirestoreDataManagerComponent(
    dataViewModel: DataViewModel,
    modifier: Modifier = Modifier
) {
    FirestoreStudioScreen(
        dataViewModel = dataViewModel,
        modifier = modifier
    )
}

/**
 * Composable screen that displays Firestore collections and document navigation in a sidebar
 * and the selected document content in a central editing panel, using [DataViewModel] to maintain state.
 */
@Composable
fun FirestoreStudioScreen(
    dataViewModel: DataViewModel,
    modifier: Modifier = Modifier
) {
    val connectionStatus by dataViewModel.connectionStatus.collectAsStateWithLifecycle()
    val latencyMs by dataViewModel.connectionLatencyMs.collectAsStateWithLifecycle()
    val collections by dataViewModel.firestoreCollections.collectAsStateWithLifecycle()
    val selectedCollectionId by dataViewModel.selectedCollectionId.collectAsStateWithLifecycle()
    val documents by dataViewModel.activeCollectionDocuments.collectAsStateWithLifecycle()
    val selectedDocument by dataViewModel.selectedDocument.collectAsStateWithLifecycle()
    val isCreatingNewDocument by dataViewModel.isCreatingNewDocument.collectAsStateWithLifecycle()
    val activeEditorTab by dataViewModel.activeEditorTab.collectAsStateWithLifecycle()
    val searchQuery by dataViewModel.searchQuery.collectAsStateWithLifecycle()
    val statusMessage by dataViewModel.dataStatusMessage.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var isSidebarVisible by rememberSaveable { mutableStateOf(true) }

    val activeCollectionInfo = collections.find { it.id == selectedCollectionId }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Connection & Workspace Controls Bar
        FirestoreWorkspaceTopBar(
            connectionStatus = connectionStatus,
            databaseId = dataViewModel.databaseId,
            userId = dataViewModel.currentUserId,
            latencyMs = latencyMs,
            totalCollections = collections.size,
            totalDocuments = collections.sumOf { it.documentCount },
            isSidebarVisible = isSidebarVisible,
            statusMessage = statusMessage,
            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
            onPingAndFetch = {
                dataViewModel.verifyFirestoreConnection()
                dataViewModel.fetchCollections()
                dataViewModel.fetchDocuments(selectedCollectionId)
            },
            onDismissMessage = { dataViewModel.clearDataStatusMessage() }
        )

        // Main Split-Pane Layout: Sidebar (Collections & Documents Tree) + Central Editing Panel
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWideScreen = maxWidth >= 660.dp
            val sidebarWidth = if (isWideScreen) 280.dp else 215.dp

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(visible = isSidebarVisible) {
                    FirestoreCollectionsSidebar(
                        collections = collections,
                        selectedCollectionId = selectedCollectionId,
                        documents = documents,
                        selectedDocumentId = selectedDocument?.id,
                        searchQuery = searchQuery,
                        onSelectCollection = { colId ->
                            dataViewModel.selectCollection(colId)
                            dataViewModel.fetchDocuments(colId)
                        },
                        onSelectDocument = { docId ->
                            dataViewModel.selectDocument(docId)
                        },
                        onSearchQueryChange = { query ->
                            dataViewModel.updateSearchQuery(query)
                        },
                        onStartCreateNewDocument = {
                            dataViewModel.startCreatingNewDocument()
                        },
                        onRefreshCollection = {
                            dataViewModel.fetchDocuments(selectedCollectionId)
                        },
                        modifier = Modifier
                            .width(sidebarWidth)
                            .fillMaxHeight()
                    )
                }

                // Central Editing & Inspection Panel
                FirestoreCentralEditingPanel(
                    activeCollectionInfo = activeCollectionInfo,
                    selectedDocument = selectedDocument,
                    isCreatingNewDocument = isCreatingNewDocument,
                    activeEditorTab = activeEditorTab,
                    onSelectEditorTab = { dataViewModel.setEditorTab(it) },
                    onStartCreateNewDocument = { dataViewModel.startCreatingNewDocument() },
                    onCancelCreateNewDocument = { dataViewModel.cancelCreatingNewDocument() },
                    onCreateDocument = { primary, secondary, third ->
                        dataViewModel.createDocumentInActiveCollection(primary, secondary, third)
                    },
                    onFetchSingleDocument = { doc ->
                        dataViewModel.fetchDocument(doc.collectionId, doc.id)
                    },
                    onCopyJson = { jsonText ->
                        clipboardManager.setText(AnnotatedString(jsonText))
                    },
                    onUpdateDocument = { doc, primary, secondary, third ->
                        dataViewModel.updateDocumentInFirestore(
                            collectionId = doc.collectionId,
                            documentId = doc.id,
                            updatedPrimary = primary,
                            updatedSecondary = secondary,
                            updatedJsonOrThird = third
                        )
                    },
                    onDeleteDocument = { doc ->
                        dataViewModel.deleteDocumentFromFirestore(doc.collectionId, doc.id)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun FirestoreWorkspaceTopBar(
    connectionStatus: FirestoreConnectionStatus,
    databaseId: String,
    userId: String,
    latencyMs: Long,
    totalCollections: Int,
    totalDocuments: Int,
    isSidebarVisible: Boolean,
    statusMessage: String?,
    onToggleSidebar: () -> Unit,
    onPingAndFetch: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val badgeColor = when (connectionStatus) {
        FirestoreConnectionStatus.CONNECTED -> CyberEmerald
        FirestoreConnectionStatus.SYNCING -> CyberAmber
        FirestoreConnectionStatus.ERROR -> CyberCoral
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.testTag("toggle_firestore_sidebar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuOpen,
                            contentDescription = "إظهار أو إخفاء الشريط الجانبي للمجموعات",
                            tint = if (isSidebarVisible) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )

                    Column {
                        Text(
                            text = "${connectionStatus.labelAr} • $totalCollections مجموعات • $totalDocuments مستند",
                            style = MaterialTheme.typography.labelLarge,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "DB: ${databaseId.take(22)}... | Scope: /users/${userId.take(10)}... | ${latencyMs}ms",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPingAndFetch,
                    modifier = Modifier.testTag("firestore_ping_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مزامنة وفحص", color = CyberCyan)
                }
            }

            if (statusMessage != null) {
                Surface(
                    color = CyberBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissMessage) {
                            Text("إخفاء", color = CyberCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirestoreCollectionsSidebar(
    collections: List<FirestoreCollectionInfo>,
    selectedCollectionId: String,
    documents: List<FirestoreDocumentItem>,
    selectedDocumentId: String?,
    searchQuery: String,
    onSelectCollection: (String) -> Unit,
    onSelectDocument: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStartCreateNewDocument: () -> Unit,
    onRefreshCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section 1: Firestore Collections Header & Vertical List
            Text(
                text = "📂 مجموعات Firestore",
                style = MaterialTheme.typography.titleMedium,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                collections.forEach { colInfo ->
                    val isSelected = colInfo.id == selectedCollectionId
                    Surface(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.16f) else CyberBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyberCyan else CyberBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectCollection(colInfo.id) }
                            .testTag("select_collection_${colInfo.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = colInfo.name,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = colInfo.titleAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = if (isSelected) CyberCyan else CyberSurface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${colInfo.documentCount}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = if (isSelected) CyberBackground else CyberEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CyberBorder)

            // Section 2: Documents List within the Selected Collection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄 مستندات ($selectedCollectionId)",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberEmerald,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onRefreshCollection,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("fetch_collection_docs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "جلب مستندات المجموعة",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onStartCreateNewDocument,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_new_doc_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة مستند جديد",
                            tint = CyberEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                placeholder = { Text("تصفية المستندات...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firestore_doc_search_input")
            )

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مستندات في هذه المجموعة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(documents, key = { "${it.collectionId}_${it.id}" }) { doc ->
                        val isDocSelected = doc.id == selectedDocumentId
                        Surface(
                            color = if (isDocSelected) CyberViolet.copy(alpha = 0.22f) else CyberBackground,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isDocSelected) 1.5.dp else 1.dp,
                                    color = if (isDocSelected) CyberCyan else CyberBorder,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { onSelectDocument(doc.id) }
                                .testTag("inspect_doc_${doc.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = doc.id,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = if (isDocSelected) CyberCyan else CyberEmerald,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (!doc.isUpdateAllowed) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "سجل غير قابل للتعديل",
                                            tint = CyberAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = doc.primaryTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isDocSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirestoreCentralEditingPanel(
    activeCollectionInfo: FirestoreCollectionInfo?,
    selectedDocument: FirestoreDocumentItem?,
    isCreatingNewDocument: Boolean,
    activeEditorTab: Int,
    onSelectEditorTab: (Int) -> Unit,
    onStartCreateNewDocument: () -> Unit,
    onCancelCreateNewDocument: () -> Unit,
    onCreateDocument: (String, String, String) -> Unit,
    onFetchSingleDocument: (FirestoreDocumentItem) -> Unit,
    onCopyJson: (String) -> Unit,
    onUpdateDocument: (FirestoreDocumentItem, String, String, String) -> Unit,
    onDeleteDocument: (FirestoreDocumentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier.border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        if (isCreatingNewDocument) {
            CentralCreateDocumentView(
                collectionId = activeCollectionInfo?.id ?: DataViewModel.COLLECTION_SCHEMAS,
                collectionTitleAr = activeCollectionInfo?.titleAr ?: "مجموعة البيانات",
                schemaFields = activeCollectionInfo?.schemaFields.orEmpty(),
                onCancel = onCancelCreateNewDocument,
                onCreateDocument = onCreateDocument
            )
        } else if (selectedDocument == null) {
            CentralEmptyDocumentState(
                collectionInfo = activeCollectionInfo,
                onStartCreateNewDocument = onStartCreateNewDocument
            )
        } else {
            CentralDocumentEditorView(
                collectionInfo = activeCollectionInfo,
                document = selectedDocument,
                activeEditorTab = activeEditorTab,
                onSelectEditorTab = onSelectEditorTab,
                onStartCreateNewDocument = onStartCreateNewDocument,
                onFetchSingleDocument = { onFetchSingleDocument(selectedDocument) },
                onCopyJson = { onCopyJson(selectedDocument.rawJson) },
                onUpdateDocument = { primary, secondary, third ->
                    onUpdateDocument(selectedDocument, primary, secondary, third)
                },
                onDeleteDocument = { onDeleteDocument(selectedDocument) }
            )
        }
    }
}

@Composable
private fun CentralDocumentEditorView(
    collectionInfo: FirestoreCollectionInfo?,
    document: FirestoreDocumentItem,
    activeEditorTab: Int,
    onSelectEditorTab: (Int) -> Unit,
    onStartCreateNewDocument: () -> Unit,
    onFetchSingleDocument: () -> Unit,
    onCopyJson: () -> Unit,
    onUpdateDocument: (String, String, String) -> Unit,
    onDeleteDocument: () -> Unit
) {
    val initialPrimary = remember(document.id, document.primaryTitle) {
        document.fields.values.firstOrNull().orEmpty()
    }
    val initialSecondary = remember(document.id, document.secondarySubtitle) {
        document.fields.values.drop(1).firstOrNull().orEmpty()
    }
    val initialThird = remember(document.id, document.rawJson) {
        document.fields["sampleRecordsJson"]
            ?: document.fields["language"]
            ?: document.fields.values.drop(2).firstOrNull().orEmpty()
    }

    var editPrimary by remember(document.id, initialPrimary) { mutableStateOf(initialPrimary) }
    var editSecondary by remember(document.id, initialSecondary) { mutableStateOf(initialSecondary) }
    var editThird by remember(document.id, initialThird) { mutableStateOf(initialThird) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Central Document Header Bar
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
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = document.id,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (document.isUpdateAllowed) CyberEmerald.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (document.isUpdateAllowed) "قابل للتعديل (Read/Write)" else "سجل موثق غير قابل للتعديل (Immutable)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (document.isUpdateAllowed) CyberEmerald else CyberAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = document.primaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "/${document.fullPath} • Synced: ${document.timestampLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onFetchSingleDocument,
                    modifier = Modifier.testTag("fetch_single_doc_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث المستند من Firestore",
                        tint = CyberCyan
                    )
                }
                IconButton(
                    onClick = onCopyJson,
                    modifier = Modifier.testTag("copy_doc_json_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ JSON",
                        tint = CyberCyan
                    )
                }
                IconButton(
                    onClick = onDeleteDocument,
                    modifier = Modifier.testTag("delete_doc_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف المستند من Firestore",
                        tint = CyberCoral
                    )
                }
            }
        }

        HorizontalDivider(color = CyberBorder)

        // 2. Editor Mode Sub-Tabs + New Document Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeEditorTab == 0,
                    onClick = { onSelectEditorTab(0) },
                    label = { Text("محرر الحقول المهيكل (Fields Editor)") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("central_tab_fields_editor")
                )
                FilterChip(
                    selected = activeEditorTab == 1,
                    onClick = { onSelectEditorTab(1) },
                    label = { Text("مفتش JSON والمخطط (Raw JSON)") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("central_tab_json_inspector")
                )
            }

            OutlinedButton(
                onClick = onStartCreateNewDocument,
                modifier = Modifier.testTag("central_new_doc_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مستند جديد", color = CyberEmerald)
            }
        }

        // 3. Central Panel Content based on Selected Sub-Tab
        if (activeEditorTab == 0) {
            // Structured Fields Editor & Key-Value Inspector
            if (document.isUpdateAllowed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تحرير بيانات المستند في مجموعة (${document.collectionId})",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )

                    OutlinedTextField(
                        value = editPrimary,
                        onValueChange = { editPrimary = it },
                        label = {
                            Text(
                                if (document.collectionId == DataViewModel.COLLECTION_SCHEMAS)
                                    "اسم المجموعة (collectionName)"
                                else
                                    "اسم المشروع (name)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_doc_primary_${document.id}")
                    )

                    OutlinedTextField(
                        value = editSecondary,
                        onValueChange = { editSecondary = it },
                        label = {
                            Text(
                                if (document.collectionId == DataViewModel.COLLECTION_SCHEMAS)
                                    "تعريف الحقول والأنواع (fieldsSpec)"
                                else
                                    "وصف المشروع (description)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_doc_secondary_${document.id}")
                    )

                    OutlinedTextField(
                        value = editThird,
                        onValueChange = { editThird = it },
                        label = {
                            Text(
                                if (document.collectionId == DataViewModel.COLLECTION_SCHEMAS)
                                    "محتوى السجلات المهيكلة (sampleRecordsJson)"
                                else
                                    "لغة وتقنية المشروع (language)"
                            )
                        },
                        singleLine = false,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_doc_third_${document.id}")
                    )

                    Button(
                        onClick = { onUpdateDocument(editPrimary, editSecondary, editThird) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_doc_${document.id}")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ وتحديث المستند في Cloud Firestore",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    color = CyberAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyberAmber)
                        Text(
                            text = "هذا المستند ينتمي إلى مجموعة تدقيق غير قابلة للتعديل (${document.collectionId}) وفق قواعد الأمان الصارمة في firestore.rules. يمكنك استعراض الحقول أو إنشاء مستند جديد أو حذف المستند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberAmber
                        )
                    }
                }
            }

            // Structured Key-Value Fields Table
            Text(
                text = "جدول الحقول والقيم الحالية (Document Fields Map):",
                style = MaterialTheme.typography.labelLarge,
                color = CyberEmerald,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBackground)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                document.fields.forEach { (fieldKey, fieldValue) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = fieldKey,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(155.dp)
                            )
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = fieldValue,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                }
            }
        } else {
            // Raw JSON Payload & Schema Inspector
            if (collectionInfo != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "مخطط الحقول المعتمدة في قواعد أمان المجموعة (${collectionInfo.name}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberAmber
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        collectionInfo.schemaFields.forEach { fieldName ->
                            Surface(
                                color = CyberBackground,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = fieldName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = CyberCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = document.rawJson,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = CyberEmerald,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CentralCreateDocumentView(
    collectionId: String,
    collectionTitleAr: String,
    schemaFields: List<String>,
    onCancel: () -> Unit,
    onCreateDocument: (String, String, String) -> Unit
) {
    var primaryInput by remember(collectionId) { mutableStateOf("") }
    var secondaryInput by remember(collectionId) { mutableStateOf("") }
    var thirdInput by remember(collectionId) { mutableStateOf("") }

    val (primaryLabel, secondaryLabel, thirdLabel) = when (collectionId) {
        DataViewModel.COLLECTION_SCHEMAS -> Triple(
            "اسم المجموعة الجديدة (collectionName)",
            "تعريف الحقول والأنواع (fieldsSpec)",
            "سجلات JSON الأولية (sampleRecordsJson)"
        )
        DataViewModel.COLLECTION_PROJECTS -> Triple(
            "اسم المشروع السحابي (name)",
            "وصف المشروع (description)",
            "بيئة/لغة التطوير (language)"
        )
        DataViewModel.COLLECTION_SNAPSHOTS -> Triple(
            "وسم الإصدار (versionTag مثل v1.4.0)",
            "رسالة التوثيق (commitMessage)",
            "ملاحظات الإصدار"
        )
        else -> Triple(
            "مرحلة العملية (stage مثل SECURITY_SCAN)",
            "الحالة (SUCCESS / WARNING / ERROR)",
            "ملخص التقرير التفصيلي (reportSummary)"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إدراج مستند جديد في: $collectionTitleAr",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "سيتم التحقق من توافق الحقول تلقائياً مع قواعد أمان Cloud Firestore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_new_doc_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = CyberCoral)
            }
        }

        if (schemaFields.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مخطط المجموعة:", style = MaterialTheme.typography.labelMedium, color = CyberAmber)
                schemaFields.forEach { field ->
                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = field,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = primaryInput,
            onValueChange = { primaryInput = it },
            label = { Text(primaryLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_doc_primary_input")
        )

        OutlinedTextField(
            value = secondaryInput,
            onValueChange = { secondaryInput = it },
            label = { Text(secondaryLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_doc_secondary_input")
        )

        OutlinedTextField(
            value = thirdInput,
            onValueChange = { thirdInput = it },
            label = { Text(thirdLabel) },
            singleLine = false,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_doc_third_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = {
                    onCreateDocument(primaryInput, secondaryInput, thirdInput)
                    primaryInput = ""
                    secondaryInput = ""
                    thirdInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                modifier = Modifier
                    .weight(2f)
                    .testTag("submit_new_firestore_doc_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = CyberBackground)
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ المستند في Cloud Firestore", color = CyberBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CentralEmptyDocumentState(
    collectionInfo: FirestoreCollectionInfo?,
    onStartCreateNewDocument: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "اختر مستنداً من الشريط الجانبي أو أنشئ مستنداً جديداً",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = collectionInfo?.descriptionAr ?: "استعرض مجموعات Cloud Firestore وحرر المستندات بشكل مباشر.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onStartCreateNewDocument,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            modifier = Modifier.testTag("empty_state_new_doc_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = CyberBackground)
            Spacer(modifier = Modifier.width(6.dp))
            Text("إنشاء مستند جديد الآن", color = CyberBackground, fontWeight = FontWeight.Bold)
        }
    }
}
