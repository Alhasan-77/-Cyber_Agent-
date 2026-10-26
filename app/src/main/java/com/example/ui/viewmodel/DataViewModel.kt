package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.BuildExecutionLog
import com.example.data.model.DbCollectionSchema
import com.example.data.model.FirestoreCollectionInfo
import com.example.data.model.FirestoreConnectionStatus
import com.example.data.model.FirestoreDocumentItem
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.VersionSnapshot
import com.example.data.repository.StudioRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataViewModel(
    private val repository: StudioRepository,
    val databaseId: String,
    val currentUserId: String
) : ViewModel() {

    /**
     * Secondary constructor that directly initializes FirebaseFirestore using the mandatory
     * named database ID (`R.string.firestore_database_id`) and resolves the signed-in Google user UID.
     */
    constructor(context: Context) : this(
        repository = StudioRepository(
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        ),
        databaseId = context.applicationContext.getString(R.string.firestore_database_id),
        currentUserId = Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before initializing DataViewModel.")
    )

    private val _connectionStatus = MutableStateFlow(FirestoreConnectionStatus.CONNECTED)
    val connectionStatus: StateFlow<FirestoreConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionLatencyMs = MutableStateFlow(18L)
    val connectionLatencyMs: StateFlow<Long> = _connectionLatencyMs.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow(COLLECTION_SCHEMAS)
    val selectedCollectionId: StateFlow<String> = _selectedCollectionId.asStateFlow()

    private val _selectedDocumentId = MutableStateFlow<String?>(null)
    val selectedDocumentId: StateFlow<String?> = _selectedDocumentId.asStateFlow()

    private val _isCreatingNewDocument = MutableStateFlow(false)
    val isCreatingNewDocument: StateFlow<Boolean> = _isCreatingNewDocument.asStateFlow()

    private val _activeEditorTab = MutableStateFlow(0)
    val activeEditorTab: StateFlow<Int> = _activeEditorTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dataStatusMessage = MutableStateFlow<String?>(null)
    val dataStatusMessage: StateFlow<String?> = _dataStatusMessage.asStateFlow()

    // Observable states for on-demand fetch and update operations
    private val _fetchedCollectionsState =
        MutableStateFlow<UiState<List<FirestoreCollectionInfo>>>(UiState.Loading)
    val fetchedCollectionsState: StateFlow<UiState<List<FirestoreCollectionInfo>>> =
        _fetchedCollectionsState.asStateFlow()

    private val _fetchedDocumentsState =
        MutableStateFlow<UiState<List<FirestoreDocumentItem>>>(UiState.Loading)
    val fetchedDocumentsState: StateFlow<UiState<List<FirestoreDocumentItem>>> =
        _fetchedDocumentsState.asStateFlow()

    private val _fetchedDocumentState =
        MutableStateFlow<UiState<FirestoreDocumentItem?>>(UiState.Success(null))
    val fetchedDocumentState: StateFlow<UiState<FirestoreDocumentItem?>> =
        _fetchedDocumentState.asStateFlow()

    private val _mutationState =
        MutableStateFlow<UiState<String>>(UiState.Success("Ready"))
    val mutationState: StateFlow<UiState<String>> = _mutationState.asStateFlow()

    // Two-Tier Kotlin Flow Error Handling for all Firestore collections
    val schemasState: StateFlow<UiState<List<DbCollectionSchema>>> = repository.observeCollections()
        .map<List<DbCollectionSchema>, UiState<List<DbCollectionSchema>>> { list ->
            _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
            UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
        }
        .catch { e ->
            _connectionStatus.value = FirestoreConnectionStatus.ERROR
            emit(UiState.Error(e.message ?: "فشل تحميل مجموعات البيانات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val projectsState: StateFlow<UiState<List<StudioProject>>> = repository.observeProjects()
        .map<List<StudioProject>, UiState<List<StudioProject>>> { list ->
            _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
            UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
        }
        .catch { e ->
            _connectionStatus.value = FirestoreConnectionStatus.ERROR
            emit(UiState.Error(e.message ?: "فشل تحميل المشاريع"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val snapshotsState: StateFlow<UiState<List<VersionSnapshot>>> = repository.observeSnapshots()
        .map<List<VersionSnapshot>, UiState<List<VersionSnapshot>>> { list ->
            _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { e ->
            _connectionStatus.value = FirestoreConnectionStatus.ERROR
            emit(UiState.Error(e.message ?: "فشل تحميل لقطات الإصدارات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val logsState: StateFlow<UiState<List<BuildExecutionLog>>> = repository.observeBuildLogs()
        .map<List<BuildExecutionLog>, UiState<List<BuildExecutionLog>>> { list ->
            _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { e ->
            _connectionStatus.value = FirestoreConnectionStatus.ERROR
            emit(UiState.Error(e.message ?: "فشل تحميل سجلات العمليات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val firestoreCollections: StateFlow<List<FirestoreCollectionInfo>> = combine(
        schemasState,
        projectsState,
        snapshotsState,
        logsState
    ) { schemasUi, projectsUi, snapshotsUi, logsUi ->
        val schemasCount = (schemasUi as? UiState.Success)?.data?.size ?: 0
        val projectsCount = (projectsUi as? UiState.Success)?.data?.size ?: 0
        val snapshotsCount = (snapshotsUi as? UiState.Success)?.data?.size ?: 0
        val logsCount = (logsUi as? UiState.Success)?.data?.size ?: 0

        buildCollectionsSummaryList(
            schemasCount = schemasCount,
            projectsCount = projectsCount,
            snapshotsCount = snapshotsCount,
            logsCount = logsCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCollectionDocuments: StateFlow<List<FirestoreDocumentItem>> = combine(
        _selectedCollectionId,
        _searchQuery,
        schemasState,
        projectsState,
        combine(snapshotsState, logsState) { s, l -> s to l }
    ) { selectedCol, query, schemasUi, projectsUi, (snapshotsUi, logsUi) ->
        val rawDocs = when (selectedCol) {
            COLLECTION_SCHEMAS -> {
                (schemasUi as? UiState.Success)?.data.orEmpty().map { mapSchemaToDocumentItem(it) }
            }
            COLLECTION_PROJECTS -> {
                (projectsUi as? UiState.Success)?.data.orEmpty().map { mapProjectToDocumentItem(it) }
            }
            COLLECTION_SNAPSHOTS -> {
                (snapshotsUi as? UiState.Success)?.data.orEmpty().map { mapSnapshotToDocumentItem(it) }
            }
            COLLECTION_LOGS -> {
                (logsUi as? UiState.Success)?.data.orEmpty().map { mapLogToDocumentItem(it) }
            }
            else -> emptyList()
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            rawDocs
        } else {
            rawDocs.filter { doc ->
                doc.id.contains(trimmed, ignoreCase = true) ||
                    doc.primaryTitle.contains(trimmed, ignoreCase = true) ||
                    doc.secondarySubtitle.contains(trimmed, ignoreCase = true) ||
                    doc.rawJson.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val selectedDocument: StateFlow<FirestoreDocumentItem?> = combine(
        activeCollectionDocuments,
        _selectedDocumentId
    ) { docs, selectedId ->
        if (selectedId != null) {
            docs.find { it.id == selectedId } ?: docs.firstOrNull()
        } else {
            docs.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        verifyFirestoreConnection()
    }

    /**
     * Observable method to list all Firestore collections with real-time document counts and schema metadata.
     */
    fun listCollections(): Flow<UiState<List<FirestoreCollectionInfo>>> =
        firestoreCollections
            .map<List<FirestoreCollectionInfo>, UiState<List<FirestoreCollectionInfo>>> {
                UiState.Success(it)
            }
            .catch { e ->
                emit(UiState.Error(e.message ?: "فشل قراءة قائمة المجموعات"))
            }

    /**
     * Observable method to list documents in a given Firestore collection in real time.
     */
    fun listDocuments(collectionId: String): Flow<UiState<List<FirestoreDocumentItem>>> =
        when (collectionId) {
            COLLECTION_SCHEMAS -> repository.observeCollections()
                .map<List<DbCollectionSchema>, UiState<List<FirestoreDocumentItem>>> { list ->
                    UiState.Success(list.map { mapSchemaToDocumentItem(it) })
                }
            COLLECTION_PROJECTS -> repository.observeProjects()
                .map<List<StudioProject>, UiState<List<FirestoreDocumentItem>>> { list ->
                    UiState.Success(list.map { mapProjectToDocumentItem(it) })
                }
            COLLECTION_SNAPSHOTS -> repository.observeSnapshots()
                .map<List<VersionSnapshot>, UiState<List<FirestoreDocumentItem>>> { list ->
                    UiState.Success(list.map { mapSnapshotToDocumentItem(it) })
                }
            COLLECTION_LOGS -> repository.observeBuildLogs()
                .map<List<BuildExecutionLog>, UiState<List<FirestoreDocumentItem>>> { list ->
                    UiState.Success(list.map { mapLogToDocumentItem(it) })
                }
            else -> MutableStateFlow(UiState.Error("Unknown collection: $collectionId"))
        }.catch { e ->
            emit(UiState.Error(e.message ?: "فشل مراقبة مستندات المجموعة $collectionId"))
        }

    /**
     * On-demand method to fetch all collections and their document counts from Firestore,
     * updating the observable [fetchedCollectionsState].
     */
    fun fetchCollections(): StateFlow<UiState<List<FirestoreCollectionInfo>>> {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _fetchedCollectionsState.value = UiState.Loading
            runCatching {
                val schemas = repository.getCollectionSchemas().getOrThrow()
                val projects = repository.getUserProjects().getOrThrow()
                val snapshots = repository.getSnapshots().getOrThrow()
                val logs = repository.getBuildLogs().getOrThrow()
                buildCollectionsSummaryList(
                    schemasCount = schemas.size,
                    projectsCount = projects.size,
                    snapshotsCount = snapshots.size,
                    logsCount = logs.size
                )
            }.onSuccess { list ->
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _fetchedCollectionsState.value = UiState.Success(list)
                _dataStatusMessage.value =
                    "تم جلب وتحديث ${list.size} مجموعات (${list.sumOf { it.documentCount }} مستند) من Cloud Firestore"
            }.onFailure { e ->
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                _fetchedCollectionsState.value =
                    UiState.Error(e.localizedMessage ?: "فشل جلب مجموعات البيانات")
                _dataStatusMessage.value = "خطأ أثناء جلب المجموعات: ${e.localizedMessage}"
            }
        }
        return fetchedCollectionsState
    }

    /**
     * On-demand method to fetch all documents from a specific Firestore collection,
     * updating the observable [fetchedDocumentsState].
     */
    fun fetchDocuments(collectionId: String = _selectedCollectionId.value): StateFlow<UiState<List<FirestoreDocumentItem>>> {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _fetchedDocumentsState.value = UiState.Loading
            val result = runCatching {
                when (collectionId) {
                    COLLECTION_SCHEMAS -> repository.getCollectionSchemas().getOrThrow()
                        .map { mapSchemaToDocumentItem(it) }
                    COLLECTION_PROJECTS -> repository.getUserProjects().getOrThrow()
                        .map { mapProjectToDocumentItem(it) }
                    COLLECTION_SNAPSHOTS -> repository.getSnapshots().getOrThrow()
                        .map { mapSnapshotToDocumentItem(it) }
                    COLLECTION_LOGS -> repository.getBuildLogs().getOrThrow()
                        .map { mapLogToDocumentItem(it) }
                    else -> emptyList()
                }
            }
            result.onSuccess { docs ->
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _fetchedDocumentsState.value = UiState.Success(docs)
                _dataStatusMessage.value =
                    "تم جلب ${docs.size} مستند من مجموعة $collectionId بنجاح"
            }.onFailure { e ->
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                _fetchedDocumentsState.value =
                    UiState.Error(e.localizedMessage ?: "فشل جلب مستندات المجموعة")
                _dataStatusMessage.value = "فشل جلب مستندات $collectionId: ${e.localizedMessage}"
            }
        }
        return fetchedDocumentsState
    }

    /**
     * On-demand method to fetch a single document by ID from Firestore,
     * updating the observable [fetchedDocumentState].
     */
    fun fetchDocument(
        collectionId: String,
        documentId: String
    ): StateFlow<UiState<FirestoreDocumentItem?>> {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _fetchedDocumentState.value = UiState.Loading
            val result = runCatching {
                when (collectionId) {
                    COLLECTION_SCHEMAS -> repository.getCollectionSchemaById(documentId).getOrThrow()
                        ?.let { mapSchemaToDocumentItem(it) }
                    COLLECTION_PROJECTS -> repository.getProjectById(currentUserId, documentId).getOrThrow()
                        ?.let { mapProjectToDocumentItem(it) }
                    COLLECTION_SNAPSHOTS -> repository.getSnapshotById(documentId).getOrThrow()
                        ?.let { mapSnapshotToDocumentItem(it) }
                    COLLECTION_LOGS -> repository.getBuildLogById(documentId).getOrThrow()
                        ?.let { mapLogToDocumentItem(it) }
                    else -> null
                }
            }
            result.onSuccess { item ->
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _fetchedDocumentState.value = UiState.Success(item)
                if (item != null) {
                    _selectedDocumentId.value = item.id
                    _dataStatusMessage.value = "تم جلب المستند ${item.id} من /$collectionId بنجاح"
                } else {
                    _dataStatusMessage.value = "لم يتم العثور على المستند $documentId في $collectionId"
                }
            }.onFailure { e ->
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                _fetchedDocumentState.value =
                    UiState.Error(e.localizedMessage ?: "فشل جلب المستند")
                _dataStatusMessage.value = "خطأ في جلب المستند: ${e.localizedMessage}"
            }
        }
        return fetchedDocumentState
    }

    /**
     * Updates a database collection schema & records in Firestore and emits status to [mutationState].
     */
    fun updateCollection(
        schemaId: String,
        collectionName: String,
        fieldsSpec: String,
        indexConfig: String,
        sampleRecordsJson: String
    ): StateFlow<UiState<String>> {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _mutationState.value = UiState.Loading
            val existing = (schemasState.value as? UiState.Success)?.data?.find { it.id == schemaId }
                ?: repository.getCollectionSchemaById(schemaId).getOrNull()

            if (existing == null) {
                val msg = "تعذر العثور على مخطط المجموعة $schemaId لتحديثه"
                _mutationState.value = UiState.Error(msg)
                _dataStatusMessage.value = msg
                return@launch
            }

            val updated = existing.copy(
                collectionName = collectionName.ifBlank { existing.collectionName },
                fieldsSpec = fieldsSpec.ifBlank { existing.fieldsSpec },
                indexConfig = indexConfig.ifBlank { existing.indexConfig },
                sampleRecordsJson = sampleRecordsJson.ifBlank { existing.sampleRecordsJson }
            )
            repository.updateCollectionSchema(updated)
                .onSuccess {
                    _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                    val msg = "تم تحديث مخطط المجموعة ${updated.collectionName} ($schemaId) في Firestore"
                    _mutationState.value = UiState.Success(msg)
                    _dataStatusMessage.value = msg
                }
                .onFailure { e ->
                    _connectionStatus.value = FirestoreConnectionStatus.ERROR
                    val msg = "فشل تحديث مخطط المجموعة: ${e.localizedMessage}"
                    _mutationState.value = UiState.Error(msg)
                    _dataStatusMessage.value = msg
                }
        }
        return mutationState
    }

    /**
     * Updates a document's structured fields map in Firestore and emits status to [mutationState].
     */
    fun updateDocument(
        collectionId: String,
        documentId: String,
        updatedFields: Map<String, String>
    ): StateFlow<UiState<String>> {
        val primary = updatedFields["collectionName"]
            ?: updatedFields["name"]
            ?: updatedFields["primary"]
            ?: ""
        val secondary = updatedFields["fieldsSpec"]
            ?: updatedFields["description"]
            ?: updatedFields["secondary"]
            ?: ""
        val third = updatedFields["sampleRecordsJson"]
            ?: updatedFields["language"]
            ?: updatedFields["third"]
            ?: ""
        updateDocumentInFirestore(
            collectionId = collectionId,
            documentId = documentId,
            updatedPrimary = primary,
            updatedSecondary = secondary,
            updatedJsonOrThird = third
        )
        return mutationState
    }

    fun selectCollection(collectionId: String) {
        _selectedCollectionId.value = collectionId
        _selectedDocumentId.value = null
        _isCreatingNewDocument.value = false
    }

    fun selectDocument(documentId: String?) {
        _selectedDocumentId.value = documentId
        _isCreatingNewDocument.value = false
    }

    fun startCreatingNewDocument() {
        _isCreatingNewDocument.value = true
    }

    fun cancelCreatingNewDocument() {
        _isCreatingNewDocument.value = false
    }

    fun setEditorTab(tabIndex: Int) {
        _activeEditorTab.value = tabIndex.coerceIn(0, 1)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearDataStatusMessage() {
        _dataStatusMessage.value = null
    }

    fun verifyFirestoreConnection() {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            repository.checkConnectionLatency()
                .onSuccess { latency ->
                    _connectionLatencyMs.value = latency
                    _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                    _dataStatusMessage.value = "اتصال Cloud Firestore نشط ومؤمن (${latency}ms) • DB: ${databaseId.take(24)}..."
                }
                .onFailure { e ->
                    _connectionStatus.value = FirestoreConnectionStatus.ERROR
                    _dataStatusMessage.value = "تعذر التحقق من اتصال Firestore: ${e.localizedMessage}"
                }
        }
    }

    fun createDocumentInActiveCollection(
        primaryField: String,
        secondaryField: String,
        jsonOrSummaryField: String
    ) {
        val activeCol = _selectedCollectionId.value
        val defaultProjId = (projectsState.value as? UiState.Success)?.data?.firstOrNull()?.id ?: "proj_default"

        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _mutationState.value = UiState.Loading
            when (activeCol) {
                COLLECTION_SCHEMAS -> {
                    val colName = primaryField.ifBlank { "custom_entities" }
                    val fieldsSpec = secondaryField.ifBlank { "id:UUID, name:String, status:String, createdAt:Timestamp" }
                    val recordsJson = jsonOrSummaryField.ifBlank {
                        """[{"id":"doc_1","name":"سجل أولي في $colName","status":"ACTIVE"}]"""
                    }
                    repository.createCollectionSchema(
                        projectId = defaultProjId,
                        collectionName = colName,
                        fieldsSpec = fieldsSpec,
                        sampleRecordsJson = recordsJson,
                        indexConfig = "Composite Index: (status ASC, createdAt DESC)"
                    ).onSuccess { newId ->
                        _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                        _selectedDocumentId.value = newId
                        _isCreatingNewDocument.value = false
                        val msg = "تم إنشاء مستند مجموعة جديد ($newId) في /users/$currentUserId/collections"
                        _mutationState.value = UiState.Success(msg)
                        _dataStatusMessage.value = msg
                    }.onFailure { e ->
                        _connectionStatus.value = FirestoreConnectionStatus.ERROR
                        val msg = "فشل إنشاء مستند المجموعة: ${e.localizedMessage}"
                        _mutationState.value = UiState.Error(msg)
                        _dataStatusMessage.value = msg
                    }
                }
                COLLECTION_PROJECTS -> {
                    val projName = primaryField.ifBlank { "New Cloud Microservice" }
                    val projDesc = secondaryField.ifBlank { "مشروع سحابي تم إنشاؤه من مدير مستندات Firestore" }
                    val lang = jsonOrSummaryField.ifBlank { "Kotlin + Compose" }
                    repository.createProject(
                        name = projName,
                        description = projDesc,
                        language = lang,
                        htmlCode = "<div class='card'><h1>$projName</h1><p>$projDesc</p></div>",
                        cssCode = "body { background:#060913; color:#F0F6FC; font-family:sans-serif; padding:20px; }",
                        jsCode = "console.log('Initialized $projName');",
                        backendCode = "// Firestore Document Driven Service: $projName"
                    ).onSuccess { newId ->
                        _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                        _selectedDocumentId.value = newId
                        _isCreatingNewDocument.value = false
                        val msg = "تم إنشاء مستند مشروع جديد ($newId) في /users/$currentUserId/projects"
                        _mutationState.value = UiState.Success(msg)
                        _dataStatusMessage.value = msg
                    }.onFailure { e ->
                        _connectionStatus.value = FirestoreConnectionStatus.ERROR
                        val msg = "فشل إنشاء مستند المشروع: ${e.localizedMessage}"
                        _mutationState.value = UiState.Error(msg)
                        _dataStatusMessage.value = msg
                    }
                }
                COLLECTION_SNAPSHOTS -> {
                    val tag = primaryField.ifBlank { "v1.0.${((snapshotsState.value as? UiState.Success)?.data?.size ?: 0) + 1}" }
                    val commitMsg = secondaryField.ifBlank { "لقطة أمان سحابية من مدير المستندات" }
                    repository.createSnapshot(
                        projectId = defaultProjId,
                        commitMessage = commitMsg,
                        versionTag = tag,
                        htmlCode = "<!-- Snapshot $tag -->",
                        cssCode = "/* Snapshot CSS */",
                        jsCode = "// Snapshot JS",
                        backendCode = "// Snapshot Backend",
                        addedLines = 12L,
                        deletedLines = 2L
                    ).onSuccess { newId ->
                        _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                        _selectedDocumentId.value = newId
                        _isCreatingNewDocument.value = false
                        val msg = "تم إنشاء مستند لقطة إصدار ($newId) في /users/$currentUserId/snapshots"
                        _mutationState.value = UiState.Success(msg)
                        _dataStatusMessage.value = msg
                    }.onFailure { e ->
                        _connectionStatus.value = FirestoreConnectionStatus.ERROR
                        val msg = "فشل إنشاء لقطة الإصدار: ${e.localizedMessage}"
                        _mutationState.value = UiState.Error(msg)
                        _dataStatusMessage.value = msg
                    }
                }
                COLLECTION_LOGS -> {
                    val stage = primaryField.ifBlank { "FIRESTORE_DATA_AUDIT" }
                    val status = secondaryField.uppercase().let {
                        if (it in listOf("SUCCESS", "WARNING", "ERROR", "RUNNING")) it else "SUCCESS"
                    }
                    val summary = jsonOrSummaryField.ifBlank { "تم فحص ومزامنة مجموعات البيانات السحابية بنجاح." }
                    repository.createBuildLog(
                        projectId = defaultProjId,
                        stage = stage,
                        status = status,
                        durationMs = 45L,
                        reportSummary = summary
                    ).onSuccess { newId ->
                        _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                        _selectedDocumentId.value = newId
                        _isCreatingNewDocument.value = false
                        val msg = "تم تسجيل مستند مراقبة ($newId) في /users/$currentUserId/logs"
                        _mutationState.value = UiState.Success(msg)
                        _dataStatusMessage.value = msg
                    }.onFailure { e ->
                        _connectionStatus.value = FirestoreConnectionStatus.ERROR
                        val msg = "فشل إنشاء سجل المراقبة: ${e.localizedMessage}"
                        _mutationState.value = UiState.Error(msg)
                        _dataStatusMessage.value = msg
                    }
                }
            }
        }
    }

    fun updateDocumentInFirestore(
        collectionId: String,
        documentId: String,
        updatedPrimary: String,
        updatedSecondary: String,
        updatedJsonOrThird: String
    ) {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _mutationState.value = UiState.Loading
            when (collectionId) {
                COLLECTION_SCHEMAS -> {
                    val existing = (schemasState.value as? UiState.Success)?.data?.find { it.id == documentId }
                        ?: repository.getCollectionSchemaById(documentId).getOrNull()
                    if (existing != null) {
                        val updatedSchema = existing.copy(
                            collectionName = updatedPrimary.ifBlank { existing.collectionName },
                            fieldsSpec = updatedSecondary.ifBlank { existing.fieldsSpec },
                            sampleRecordsJson = updatedJsonOrThird.ifBlank { existing.sampleRecordsJson }
                        )
                        repository.updateCollectionSchema(updatedSchema)
                            .onSuccess {
                                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                                val msg = "تم تحديث المستند ${existing.id} في مجموعة collections بنجاح"
                                _mutationState.value = UiState.Success(msg)
                                _dataStatusMessage.value = msg
                            }
                            .onFailure { e ->
                                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                                val msg = "فشل تحديث المستند: ${e.localizedMessage}"
                                _mutationState.value = UiState.Error(msg)
                                _dataStatusMessage.value = msg
                            }
                    }
                }
                COLLECTION_PROJECTS -> {
                    val existing = (projectsState.value as? UiState.Success)?.data?.find { it.id == documentId }
                        ?: repository.getProjectById(currentUserId, documentId).getOrNull()
                    if (existing != null) {
                        val updatedProj = existing.copy(
                            name = updatedPrimary.ifBlank { existing.name },
                            description = updatedSecondary.ifBlank { existing.description },
                            language = updatedJsonOrThird.ifBlank { existing.language }
                        )
                        repository.updateProject(updatedProj)
                            .onSuccess {
                                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                                val msg = "تم تحديث مستند المشروع ${existing.id} في Firestore بنجاح"
                                _mutationState.value = UiState.Success(msg)
                                _dataStatusMessage.value = msg
                            }
                            .onFailure { e ->
                                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                                val msg = "فشل تحديث المشروع: ${e.localizedMessage}"
                                _mutationState.value = UiState.Error(msg)
                                _dataStatusMessage.value = msg
                            }
                    }
                }
                else -> {
                    _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                    val msg = "هذه المجموعة محمية بقواعد عدم التعديل (Immutable Audit Records) في Firestore"
                    _mutationState.value = UiState.Error(msg)
                    _dataStatusMessage.value = msg
                }
            }
        }
    }

    fun deleteDocumentFromFirestore(collectionId: String, documentId: String) {
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _mutationState.value = UiState.Loading
            val result = when (collectionId) {
                COLLECTION_SCHEMAS -> repository.deleteCollectionSchema(documentId)
                COLLECTION_PROJECTS -> repository.deleteProject(documentId)
                COLLECTION_SNAPSHOTS -> repository.deleteSnapshot(documentId)
                COLLECTION_LOGS -> repository.deleteBuildLog(documentId)
                else -> Result.failure(IllegalArgumentException("Unknown collection: $collectionId"))
            }
            result.onSuccess {
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                if (_selectedDocumentId.value == documentId) {
                    _selectedDocumentId.value = null
                }
                val msg = "تم حذف المستند ($documentId) من مجموعة $collectionId"
                _mutationState.value = UiState.Success(msg)
                _dataStatusMessage.value = msg
            }.onFailure { e ->
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                val msg = "فشل حذف المستند: ${e.localizedMessage}"
                _mutationState.value = UiState.Error(msg)
                _dataStatusMessage.value = msg
            }
        }
    }

    private fun buildCollectionsSummaryList(
        schemasCount: Int,
        projectsCount: Int,
        snapshotsCount: Int,
        logsCount: Int
    ): List<FirestoreCollectionInfo> = listOf(
        FirestoreCollectionInfo(
            id = COLLECTION_SCHEMAS,
            name = "collections",
            titleAr = "مخططات ومجموعات البيانات (collections)",
            fullPath = "users/$currentUserId/collections",
            documentCount = schemasCount,
            isUpdateAllowed = true,
            descriptionAr = "مستندات تعريف الجداول والحقول والسجلات المهيكلة بصيغة JSON",
            schemaFields = listOf("collectionName", "fieldsSpec", "sampleRecordsJson", "indexConfig", "projectId")
        ),
        FirestoreCollectionInfo(
            id = COLLECTION_PROJECTS,
            name = "projects",
            titleAr = "مشاريع التطبيقات الكاملة (projects)",
            fullPath = "users/$currentUserId/projects",
            documentCount = projectsCount,
            isUpdateAllowed = true,
            descriptionAr = "مستندات المشاريع السحابية وأكواد HTML/CSS/JS/Backend",
            schemaFields = listOf("name", "description", "language", "htmlCode", "cssCode", "jsCode", "backendCode")
        ),
        FirestoreCollectionInfo(
            id = COLLECTION_SNAPSHOTS,
            name = "snapshots",
            titleAr = "لقطات الإصدارات الموثقة (snapshots)",
            fullPath = "users/$currentUserId/snapshots",
            documentCount = snapshotsCount,
            isUpdateAllowed = false,
            descriptionAr = "سجل التغييرات غير القابل للتعديل (Immutable Commits) لضمان أمان البيانات",
            schemaFields = listOf("versionTag", "commitMessage", "projectId", "addedLines", "deletedLines")
        ),
        FirestoreCollectionInfo(
            id = COLLECTION_LOGS,
            name = "logs",
            titleAr = "سجلات البناء والمراقبة (logs)",
            fullPath = "users/$currentUserId/logs",
            documentCount = logsCount,
            isUpdateAllowed = false,
            descriptionAr = "تقارير الفحص الأمني والأداء وخطوط أنابيب النشر السحابي",
            schemaFields = listOf("stage", "status", "durationMs", "reportSummary", "projectId")
        )
    )

    private fun mapSchemaToDocumentItem(schema: DbCollectionSchema): FirestoreDocumentItem {
        val ts = formatTimestamp(schema.updatedAt?.toDate() ?: schema.createdAt?.toDate())
        return FirestoreDocumentItem(
            id = schema.id,
            collectionId = COLLECTION_SCHEMAS,
            fullPath = "users/$currentUserId/collections/${schema.id}",
            primaryTitle = schema.collectionName,
            secondarySubtitle = "الحقول: ${schema.fieldsSpec} • الفهرس: ${schema.indexConfig}",
            fields = linkedMapOf(
                "collectionName" to schema.collectionName,
                "fieldsSpec" to schema.fieldsSpec,
                "indexConfig" to schema.indexConfig,
                "projectId" to schema.projectId,
                "sampleRecordsJson" to schema.sampleRecordsJson
            ),
            rawJson = buildPrettyJson(
                mapOf(
                    "id" to schema.id,
                    "userId" to schema.userId,
                    "projectId" to schema.projectId,
                    "collectionName" to schema.collectionName,
                    "fieldsSpec" to schema.fieldsSpec,
                    "indexConfig" to schema.indexConfig,
                    "sampleRecordsJson" to schema.sampleRecordsJson,
                    "updatedAt" to ts
                )
            ),
            isUpdateAllowed = true,
            timestampLabel = ts
        )
    }

    private fun mapProjectToDocumentItem(proj: StudioProject): FirestoreDocumentItem {
        val ts = formatTimestamp(proj.updatedAt?.toDate() ?: proj.createdAt?.toDate())
        return FirestoreDocumentItem(
            id = proj.id,
            collectionId = COLLECTION_PROJECTS,
            fullPath = "users/$currentUserId/projects/${proj.id}",
            primaryTitle = proj.name,
            secondarySubtitle = "${proj.language} • ${proj.description}",
            fields = linkedMapOf(
                "name" to proj.name,
                "description" to proj.description,
                "language" to proj.language,
                "htmlCodeLength" to "${proj.htmlCode.length} chars",
                "cssCodeLength" to "${proj.cssCode.length} chars",
                "jsCodeLength" to "${proj.jsCode.length} chars",
                "backendCodeLength" to "${proj.backendCode.length} chars"
            ),
            rawJson = buildPrettyJson(
                mapOf(
                    "id" to proj.id,
                    "userId" to proj.userId,
                    "name" to proj.name,
                    "description" to proj.description,
                    "language" to proj.language,
                    "updatedAt" to ts
                )
            ),
            isUpdateAllowed = true,
            timestampLabel = ts
        )
    }

    private fun mapSnapshotToDocumentItem(snap: VersionSnapshot): FirestoreDocumentItem {
        val ts = formatTimestamp(snap.createdAt?.toDate())
        return FirestoreDocumentItem(
            id = snap.id,
            collectionId = COLLECTION_SNAPSHOTS,
            fullPath = "users/$currentUserId/snapshots/${snap.id}",
            primaryTitle = "${snap.versionTag} — ${snap.commitMessage}",
            secondarySubtitle = "المشروع: ${snap.projectId} • (+${snap.addedLines} / -${snap.deletedLines})",
            fields = linkedMapOf(
                "versionTag" to snap.versionTag,
                "commitMessage" to snap.commitMessage,
                "projectId" to snap.projectId,
                "addedLines" to snap.addedLines.toString(),
                "deletedLines" to snap.deletedLines.toString()
            ),
            rawJson = buildPrettyJson(
                mapOf(
                    "id" to snap.id,
                    "userId" to snap.userId,
                    "projectId" to snap.projectId,
                    "versionTag" to snap.versionTag,
                    "commitMessage" to snap.commitMessage,
                    "addedLines" to snap.addedLines.toString(),
                    "deletedLines" to snap.deletedLines.toString(),
                    "createdAt" to ts
                )
            ),
            isUpdateAllowed = false,
            timestampLabel = ts
        )
    }

    private fun mapLogToDocumentItem(log: BuildExecutionLog): FirestoreDocumentItem {
        val ts = formatTimestamp(log.createdAt?.toDate())
        return FirestoreDocumentItem(
            id = log.id,
            collectionId = COLLECTION_LOGS,
            fullPath = "users/$currentUserId/logs/${log.id}",
            primaryTitle = "[${log.status}] ${log.stage}",
            secondarySubtitle = "${log.durationMs}ms • ${log.reportSummary.take(90)}",
            fields = linkedMapOf(
                "stage" to log.stage,
                "status" to log.status,
                "durationMs" to "${log.durationMs} ms",
                "projectId" to log.projectId,
                "reportSummary" to log.reportSummary
            ),
            rawJson = buildPrettyJson(
                mapOf(
                    "id" to log.id,
                    "userId" to log.userId,
                    "projectId" to log.projectId,
                    "stage" to log.stage,
                    "status" to log.status,
                    "durationMs" to log.durationMs.toString(),
                    "reportSummary" to log.reportSummary,
                    "createdAt" to ts
                )
            ),
            isUpdateAllowed = false,
            timestampLabel = ts
        )
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return "الآن (Server Timestamp)"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(date)
    }

    private fun buildPrettyJson(map: Map<String, String>): String {
        val entries = map.entries.joinToString(",\n") { (k, v) ->
            val escaped = v.replace("\"", "\\\"").replace("\n", "\\n")
            "  \"$k\": \"$escaped\""
        }
        return "{\n$entries\n}"
    }

    companion object {
        const val COLLECTION_SCHEMAS = "collections"
        const val COLLECTION_PROJECTS = "projects"
        const val COLLECTION_SNAPSHOTS = "snapshots"
        const val COLLECTION_LOGS = "logs"
    }
}
