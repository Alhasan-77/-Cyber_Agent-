package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.BioExperimentRecord
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
import kotlinx.coroutines.CancellationException
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

    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

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

    private val _connectionStatus = MutableStateFlow(FirestoreConnectionStatus.SYNCING)
    val connectionStatus: StateFlow<FirestoreConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionLatencyMs = MutableStateFlow(0L)
    val connectionLatencyMs: StateFlow<Long> = _connectionLatencyMs.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow(COLLECTION_SCHEMAS)
    val selectedCollectionId: StateFlow<String> = _selectedCollectionId.asStateFlow()

    private val _selectedDocumentId = MutableStateFlow<String?>(null)
    val selectedDocumentId: StateFlow<String?> = _selectedDocumentId.asStateFlow()

    private val _selectedDataProjectId = MutableStateFlow<String?>(null)
    val selectedDataProjectId: StateFlow<String?> = _selectedDataProjectId.asStateFlow()

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

    val bioExperimentsState: StateFlow<UiState<List<BioExperimentRecord>>> = repository.observeBioExperiments()
        .map<List<BioExperimentRecord>, UiState<List<BioExperimentRecord>>> { list ->
            _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
            UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L })
        }
        .catch { e ->
            _connectionStatus.value = FirestoreConnectionStatus.ERROR
            emit(UiState.Error(e.message ?: "فشل تحميل تجارب المختبر الحيوي"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val firestoreCollections: StateFlow<List<FirestoreCollectionInfo>> = combine(
        schemasState,
        projectsState,
        snapshotsState,
        logsState,
        bioExperimentsState
    ) { schemasUi, projectsUi, snapshotsUi, logsUi, bioUi ->
        val schemasCount = (schemasUi as? UiState.Success)?.data?.size ?: 0
        val projectsCount = (projectsUi as? UiState.Success)?.data?.size ?: 0
        val snapshotsCount = (snapshotsUi as? UiState.Success)?.data?.size ?: 0
        val logsCount = (logsUi as? UiState.Success)?.data?.size ?: 0
        val bioCount = (bioUi as? UiState.Success)?.data?.size ?: 0

        buildCollectionsSummaryList(
            schemasCount = schemasCount,
            projectsCount = projectsCount,
            snapshotsCount = snapshotsCount,
            logsCount = logsCount,
            bioCount = bioCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCollectionDocuments: StateFlow<List<FirestoreDocumentItem>> = combine(
        _selectedCollectionId,
        _searchQuery,
        schemasState,
        projectsState,
        combine(snapshotsState, logsState, bioExperimentsState) { s, l, b -> Triple(s, l, b) }
    ) { selectedCol, query, schemasUi, projectsUi, (snapshotsUi, logsUi, bioUi) ->
        val rawDocs = when (selectedCol) {
            COLLECTION_SCHEMAS -> {
                (schemasUi as? UiState.Success)?.data.orEmpty().map { mapSchemaToDocumentItem(it) }
            }
            COLLECTION_PROJECTS -> {
                (projectsUi as? UiState.Success)?.data.orEmpty().map { mapProjectToDocumentItem(it) }
            }
            COLLECTION_BIO_EXPERIMENTS -> {
                (bioUi as? UiState.Success)?.data.orEmpty().map { mapBioExperimentToDocumentItem(it) }
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
            docs.find { it.id == selectedId }
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
            COLLECTION_BIO_EXPERIMENTS -> repository.observeBioExperiments()
                .map<List<BioExperimentRecord>, UiState<List<FirestoreDocumentItem>>> { list ->
                    UiState.Success(list.map { mapBioExperimentToDocumentItem(it) })
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
            runCatchingCancellable {
                val schemas = repository.getCollectionSchemas().getOrThrow()
                val projects = repository.getUserProjects().getOrThrow()
                val snapshots = repository.getSnapshots().getOrThrow()
                val logs = repository.getBuildLogs().getOrThrow()
                val bioExps = repository.getBioExperiments().getOrThrow()
                buildCollectionsSummaryList(
                    schemasCount = schemas.size,
                    projectsCount = projects.size,
                    snapshotsCount = snapshots.size,
                    logsCount = logs.size,
                    bioCount = bioExps.size
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
            val result = runCatchingCancellable {
                when (collectionId) {
                    COLLECTION_SCHEMAS -> repository.getCollectionSchemas().getOrThrow()
                        .map { mapSchemaToDocumentItem(it) }
                    COLLECTION_PROJECTS -> repository.getUserProjects().getOrThrow()
                        .map { mapProjectToDocumentItem(it) }
                    COLLECTION_BIO_EXPERIMENTS -> repository.getBioExperiments().getOrThrow()
                        .map { mapBioExperimentToDocumentItem(it) }
                    COLLECTION_SNAPSHOTS -> repository.getSnapshots().getOrThrow()
                        .map { mapSnapshotToDocumentItem(it) }
                    COLLECTION_LOGS -> repository.getBuildLogs().getOrThrow()
                        .map { mapLogToDocumentItem(it) }
                    else -> throw IllegalArgumentException("Unknown Firestore collection: $collectionId")
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
            val result = runCatchingCancellable {
                when (collectionId) {
                    COLLECTION_SCHEMAS -> repository.getCollectionSchemaById(documentId).getOrThrow()
                        ?.let { mapSchemaToDocumentItem(it) }
                    COLLECTION_PROJECTS -> repository.getProjectById(currentUserId, documentId).getOrThrow()
                        ?.let { mapProjectToDocumentItem(it) }
                    COLLECTION_BIO_EXPERIMENTS -> repository.getBioExperimentById(documentId).getOrThrow()
                        ?.let { mapBioExperimentToDocumentItem(it) }
                    COLLECTION_SNAPSHOTS -> repository.getSnapshotById(documentId).getOrThrow()
                        ?.let { mapSnapshotToDocumentItem(it) }
                    COLLECTION_LOGS -> repository.getBuildLogById(documentId).getOrThrow()
                        ?.let { mapLogToDocumentItem(it) }
                    else -> throw IllegalArgumentException("Unknown Firestore collection: $collectionId")
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
            try {
                val existing = (schemasState.value as? UiState.Success)?.data?.find { it.id == schemaId }
                    ?: repository.getCollectionSchemaById(schemaId).getOrThrow()
                    ?: throw IllegalStateException("تعذر العثور على مخطط المجموعة $schemaId لتحديثه.")
                val updated = existing.copy(
                    collectionName = collectionName.ifBlank { existing.collectionName },
                    fieldsSpec = fieldsSpec.ifBlank { existing.fieldsSpec },
                    indexConfig = indexConfig.ifBlank { existing.indexConfig },
                    sampleRecordsJson = sampleRecordsJson.ifBlank { existing.sampleRecordsJson }
                )
                repository.updateCollectionSchema(updated).getOrThrow()
                val message = "تم تحديث مخطط المجموعة ${updated.collectionName} ($schemaId) في Firestore."
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _mutationState.value = UiState.Success(message)
                _dataStatusMessage.value = message
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                val message = "فشل تحديث مخطط المجموعة: ${error.localizedMessage ?: error.message ?: "خطأ غير معروف"}"
                _mutationState.value = UiState.Error(message)
                _dataStatusMessage.value = message
            }
        }
        return mutationState
    }

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
        if (_selectedCollectionId.value == COLLECTION_PROJECTS && documentId != null) {
            _selectedDataProjectId.value = documentId
            _dataStatusMessage.value = "تم اختيار المشروع $documentId. ستُربط به لقطات الإصدارات والمخططات وسجلات العمليات الجديدة."
        }
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
        viewModelScope.launch {
            _connectionStatus.value = FirestoreConnectionStatus.SYNCING
            _mutationState.value = UiState.Loading
            try {
                val message = when (activeCol) {
                    COLLECTION_SCHEMAS -> {
                        val project = resolveProjectForDataOperation()
                        require(primaryField.isNotBlank()) { "أدخل اسم المجموعة." }
                        require(secondaryField.isNotBlank()) { "أدخل تعريف الحقول وأنواعها." }
                        val sampleJson = jsonOrSummaryField.ifBlank { "[]" }
                        val id = repository.createCollectionSchema(
                            projectId = project.id,
                            collectionName = primaryField.trim(),
                            fieldsSpec = secondaryField.trim(),
                            sampleRecordsJson = sampleJson,
                            indexConfig = ""
                        ).getOrThrow()
                        "تم إنشاء مخطط المجموعة $id وربطه بالمشروع ${project.name}. لم تتم إضافة سجلات إلى مجموعة تطبيق منفصلة."
                    }

                    COLLECTION_PROJECTS -> {
                        require(primaryField.isNotBlank()) { "أدخل اسم المشروع." }
                        require(secondaryField.isNotBlank()) { "أدخل وصف المشروع." }
                        require(jsonOrSummaryField.isNotBlank()) { "أدخل لغة أو تقنية المشروع." }
                        val id = repository.createProject(
                            name = primaryField.trim(),
                            description = secondaryField.trim(),
                            language = jsonOrSummaryField.trim(),
                            htmlCode = "",
                            cssCode = "",
                            jsCode = "",
                            backendCode = ""
                        ).getOrThrow()
                        "تم إنشاء المشروع $id بملفات مصدر فارغة. يمكنك فتحه وإضافة الشيفرة من محرر IDE."
                    }

                    COLLECTION_SNAPSHOTS -> {
                        val project = resolveProjectForDataOperation()
                        val existingSnapshots = repository.getSnapshots().getOrThrow()
                            .filter { it.projectId == project.id }
                            .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        val tag = primaryField.trim().ifBlank { "v1.${existingSnapshots.size + 1}.0" }
                        val commitMessage = secondaryField.trim().ifBlank {
                            "Checkpoint created from Firestore data manager"
                        }
                        val notes = jsonOrSummaryField.trim()
                        val previous = existingSnapshots.firstOrNull()
                        val previousSource = previous?.let {
                            listOf(it.htmlCode, it.cssCode, it.jsCode, it.backendCode).joinToString("\n")
                        } ?: listOf(project.htmlCode, project.cssCode, project.jsCode, project.backendCode).joinToString("\n")
                        val currentSource = listOf(
                            project.htmlCode, project.cssCode, project.jsCode, project.backendCode
                        ).joinToString("\n")
                        val (added, deleted) = countLineChanges(previousSource, currentSource)
                        val id = repository.createSnapshot(
                            projectId = project.id,
                            commitMessage = if (notes.isBlank()) commitMessage else "$commitMessage\n$notes",
                            versionTag = tag,
                            htmlCode = project.htmlCode,
                            cssCode = project.cssCode,
                            jsCode = project.jsCode,
                            backendCode = project.backendCode,
                            addedLines = added,
                            deletedLines = deleted
                        ).getOrThrow()
                        "تم إنشاء لقطة $tag ($id) من ملفات المشروع الفعلية ${project.name}."
                    }

                    COLLECTION_LOGS -> {
                        require(primaryField.isNotBlank()) { "أدخل مرحلة العملية التي تريد توثيقها." }
                        val status = secondaryField.trim().uppercase(Locale.ROOT)
                        require(status in listOf("SUCCESS", "WARNING", "ERROR", "RUNNING")) {
                            "اختر حالة صحيحة: SUCCESS أو WARNING أو ERROR أو RUNNING."
                        }
                        require(jsonOrSummaryField.isNotBlank()) { "أدخل ملخصاً حقيقياً للعملية." }
                        val project = resolveProjectForDataOperation()
                        val id = repository.createBuildLog(
                            projectId = project.id,
                            stage = primaryField.trim(),
                            status = status,
                            durationMs = 0L,
                            reportSummary = jsonOrSummaryField.trim() +
                                "\nمدة التنفيذ غير مقاسة في واجهة مدير البيانات."
                        ).getOrThrow()
                        "تم حفظ سجل العملية $id بالحالة $status. لم تقِس هذه الواجهة مدة التنفيذ."
                    }

                    COLLECTION_BIO_EXPERIMENTS -> {
                        throw IllegalArgumentException(
                            "أنشئ سجلات التجارب الحيوية من مختبر العلوم الحيوية بعد إدخال التسلسل الحقيقي وبيانات التحليل؛ لم يتم إنشاء بيانات افتراضية."
                        )
                    }

                    else -> throw IllegalArgumentException("مجموعة Firestore غير مدعومة: $activeCol")
                }

                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _isCreatingNewDocument.value = false
                _mutationState.value = UiState.Success(message)
                _dataStatusMessage.value = message
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                val message = "تعذر إنشاء المستند: ${error.localizedMessage ?: error.message ?: "خطأ غير معروف"}"
                _mutationState.value = UiState.Error(message)
                _dataStatusMessage.value = message
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
            try {
                val message = when (collectionId) {
                    COLLECTION_SCHEMAS -> {
                        val existing = (schemasState.value as? UiState.Success)?.data
                            ?.find { it.id == documentId }
                            ?: repository.getCollectionSchemaById(documentId).getOrThrow()
                            ?: throw IllegalStateException("لم يتم العثور على مخطط المجموعة $documentId.")
                        val updated = existing.copy(
                            collectionName = updatedPrimary.ifBlank { existing.collectionName },
                            fieldsSpec = updatedSecondary.ifBlank { existing.fieldsSpec },
                            sampleRecordsJson = updatedJsonOrThird.ifBlank { existing.sampleRecordsJson }
                        )
                        repository.updateCollectionSchema(updated).getOrThrow()
                        "تم تحديث مخطط المجموعة ${existing.collectionName}."
                    }

                    COLLECTION_PROJECTS -> {
                        val existing = (projectsState.value as? UiState.Success)?.data
                            ?.find { it.id == documentId }
                            ?: repository.getProjectById(currentUserId, documentId).getOrThrow()
                            ?: throw IllegalStateException("لم يتم العثور على المشروع $documentId.")
                        val updated = existing.copy(
                            name = updatedPrimary.ifBlank { existing.name },
                            description = updatedSecondary.ifBlank { existing.description },
                            language = updatedJsonOrThird.ifBlank { existing.language }
                        )
                        repository.updateProject(updated).getOrThrow()
                        "تم تحديث المشروع ${updated.name}."
                    }

                    COLLECTION_BIO_EXPERIMENTS -> {
                        val existing = (bioExperimentsState.value as? UiState.Success)?.data
                            ?.find { it.id == documentId }
                            ?: repository.getBioExperimentById(documentId).getOrThrow()
                            ?: throw IllegalStateException("لم يتم العثور على التجربة الحيوية $documentId.")
                        val updated = existing.copy(
                            title = updatedPrimary.ifBlank { existing.title },
                            workflowMode = updatedSecondary.ifBlank { existing.workflowMode },
                            aiReportSummary = updatedJsonOrThird.ifBlank { existing.aiReportSummary }
                        )
                        repository.updateBioExperiment(updated).getOrThrow()
                        "تم تحديث سجل التجربة الحيوية ${updated.title}."
                    }

                    COLLECTION_SNAPSHOTS, COLLECTION_LOGS ->
                        throw IllegalArgumentException("هذا السجل غير قابل للتعديل؛ أنشئ إصداراً أو سجلاً جديداً بدلاً من تغيير التاريخ.")

                    else -> throw IllegalArgumentException("مجموعة Firestore غير مدعومة: $collectionId")
                }

                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                _mutationState.value = UiState.Success(message)
                _dataStatusMessage.value = message
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _connectionStatus.value = FirestoreConnectionStatus.ERROR
                val message = "تعذر تحديث المستند: ${error.localizedMessage ?: error.message ?: "خطأ غير معروف"}"
                _mutationState.value = UiState.Error(message)
                _dataStatusMessage.value = message
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
                COLLECTION_BIO_EXPERIMENTS -> repository.deleteBioExperiment(documentId)
                COLLECTION_SNAPSHOTS -> repository.deleteSnapshot(documentId)
                COLLECTION_LOGS -> repository.deleteBuildLog(documentId)
                else -> Result.failure(IllegalArgumentException("Unknown collection: $collectionId"))
            }
            result.onSuccess {
                _connectionStatus.value = FirestoreConnectionStatus.CONNECTED
                if (_selectedDocumentId.value == documentId) {
                    _selectedDocumentId.value = null
                }
                if (collectionId == COLLECTION_PROJECTS && _selectedDataProjectId.value == documentId) {
                    _selectedDataProjectId.value = null
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

    private suspend fun resolveProjectForDataOperation(): StudioProject {
        val projects = (projectsState.value as? UiState.Success)?.data
            ?: repository.getUserProjects().getOrThrow()
        val selectedId = _selectedDataProjectId.value

        if (!selectedId.isNullOrBlank()) {
            return projects.firstOrNull { it.id == selectedId }
                ?: repository.getProjectById(currentUserId, selectedId).getOrThrow()
                ?: throw IllegalStateException("المشروع المختار لم يعد موجوداً. اختر مشروعاً آخر.")
        }

        return when (projects.size) {
            0 -> throw IllegalStateException("أنشئ مشروعاً أولاً قبل إضافة مخطط أو لقطة أو سجل عملية.")
            1 -> projects.single()
            else -> throw IllegalStateException(
                "لديك أكثر من مشروع. افتح مجموعة projects واختر المشروع المطلوب أولاً؛ لن أربط السجل بمشروع عشوائي."
            )
        }
    }

    private fun countLineChanges(previous: String, current: String): Pair<Long, Long> {
        fun frequencies(value: String): Map<String, Int> =
            value.lines().groupingBy { it }.eachCount()

        val oldCounts = frequencies(previous)
        val newCounts = frequencies(current)
        val allLines = oldCounts.keys + newCounts.keys
        val added = allLines.sumOf { line ->
            ((newCounts[line] ?: 0) - (oldCounts[line] ?: 0)).coerceAtLeast(0)
        }
        val deleted = allLines.sumOf { line ->
            ((oldCounts[line] ?: 0) - (newCounts[line] ?: 0)).coerceAtLeast(0)
        }
        return added.toLong() to deleted.toLong()
    }

    private fun buildCollectionsSummaryList(
        schemasCount: Int,
        projectsCount: Int,
        snapshotsCount: Int,
        logsCount: Int,
        bioCount: Int = 0
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
            id = COLLECTION_BIO_EXPERIMENTS,
            name = "bio_experiments",
            titleAr = "تجارب المختبر الحيوي (bio_experiments)",
            fullPath = "users/$currentUserId/bio_experiments",
            documentCount = bioCount,
            isUpdateAllowed = true,
            descriptionAr = "تجارب طي البروتين وتعديل الجينات CRISPR وتحليل الجينوم المتزامنة سحابياً",
            schemaFields = listOf("title", "workflowMode", "plddtScore", "freeEnergyKcal", "alphaHelixPct", "betaSheetPct", "aiReportSummary")
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

    private fun mapBioExperimentToDocumentItem(exp: BioExperimentRecord): FirestoreDocumentItem {
        val ts = formatTimestamp(exp.updatedAt?.toDate() ?: exp.createdAt?.toDate())
        return FirestoreDocumentItem(
            id = exp.id,
            collectionId = COLLECTION_BIO_EXPERIMENTS,
            fullPath = "users/$currentUserId/bio_experiments/${exp.id}",
            primaryTitle = exp.title,
            secondarySubtitle = "${exp.workflowMode} • pLDDT: ${exp.plddtScore}% • ΔG: ${exp.freeEnergyKcal} kcal/mol • Helix: ${exp.alphaHelixPct}%",
            fields = linkedMapOf(
                "title" to exp.title,
                "workflowMode" to exp.workflowMode,
                "aiReportSummary" to exp.aiReportSummary,
                "plddtScore" to "${exp.plddtScore}%",
                "freeEnergyKcal" to "${exp.freeEnergyKcal} kcal/mol",
                "alphaHelixPct" to "${exp.alphaHelixPct}%",
                "betaSheetPct" to "${exp.betaSheetPct}%",
                "researchGoal" to exp.researchGoal
            ),
            rawJson = buildPrettyJson(
                mapOf(
                    "id" to exp.id,
                    "userId" to exp.userId,
                    "title" to exp.title,
                    "workflowMode" to exp.workflowMode,
                    "plddtScore" to exp.plddtScore.toString(),
                    "freeEnergyKcal" to exp.freeEnergyKcal.toString(),
                    "alphaHelixPct" to exp.alphaHelixPct.toString(),
                    "betaSheetPct" to exp.betaSheetPct.toString(),
                    "researchGoal" to exp.researchGoal,
                    "aiReportSummary" to exp.aiReportSummary,
                    "updatedAt" to ts
                )
            ),
            isUpdateAllowed = true,
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
        const val COLLECTION_BIO_EXPERIMENTS = "bio_experiments"
        const val COLLECTION_SNAPSHOTS = "snapshots"
        const val COLLECTION_LOGS = "logs"
    }
}
