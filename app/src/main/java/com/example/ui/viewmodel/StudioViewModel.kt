package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.agent.AgentTaskRouter
import com.example.data.agent.AgentTaskType
import com.example.data.model.AiRolePersona
import com.example.data.model.BioExperimentRecord
import com.example.data.model.BuildExecutionLog
import com.example.data.model.ChatMessage
import com.example.data.model.DbCollectionSchema
import com.example.data.model.DiffLine
import com.example.data.model.DiffType
import com.example.data.model.GeneratedMediaAsset
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VersionSnapshot
import com.example.data.remote.AiEngineProvider
import com.example.data.remote.GeminiAiRepository
import com.example.data.repository.StudioRepository
import com.example.data.skills.BioscienceSimulationEngine
import com.example.data.skills.ExtractedSkillItem
import com.example.data.skills.SkillsMemoryCatalog
import com.example.data.skills.SkillsMemoryEngine
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudioViewModel(
    private val repository: StudioRepository,
    private val aiRepository: GeminiAiRepository,
    val currentUserId: String,
    val userEmail: String = "developer@novaforge.cloud",
    val userDisplayName: String = "NovaForge Developer",
    val userPhotoUrl: String = ""
) : ViewModel() {

    private val _syncLatencyMs = MutableStateFlow(16L)
    val syncLatencyMs: StateFlow<Long> = _syncLatencyMs.asStateFlow()

    private val _lastSyncTimestampLabel = MutableStateFlow(currentTimeLabel())
    val lastSyncTimestampLabel: StateFlow<String> = _lastSyncTimestampLabel.asStateFlow()

    private var hasRestoredPreferredModelFromCloud = false

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile()
        .map<UserProfile?, UiState<UserProfile?>> { profile ->
            _lastSyncTimestampLabel.value = currentTimeLabel()
            val savedModel = profile?.preferredAiModel?.takeIf { it.isNotBlank() }
            if (savedModel != null) {
                val normalized = GeminiAiRepository.normalizePreferredModel(savedModel)
                if (!hasRestoredPreferredModelFromCloud || _selectedChatModel.value != normalized) {
                    hasRestoredPreferredModelFromCloud = true
                    _selectedChatModel.value = normalized
                    aiRepository.setActiveModel(normalized)
                }
            }
            UiState.Success(profile)
        }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر مزامنة الملف الشخصي السحابي"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val projectsState: StateFlow<UiState<List<StudioProject>>> = repository.observeProjects()
        .map<List<StudioProject>, UiState<List<StudioProject>>> { list ->
            val sorted = list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            _lastSyncTimestampLabel.value = currentTimeLabel()
            if (sorted.isNotEmpty() && _selectedProjectId.value == null) {
                selectProject(sorted.first())
            } else if (sorted.isEmpty() && !hasSeededInitialData) {
                hasSeededInitialData = true
                seedInitialWorkspace()
            }
            UiState.Success(sorted)
        }
        .catch { e ->
            Log.w(TAG, "Error observing projects", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل المشاريع السحابية"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bioExperimentsState: StateFlow<UiState<List<BioExperimentRecord>>> = repository.observeBioExperiments()
        .map<List<BioExperimentRecord>, UiState<List<BioExperimentRecord>>> { list ->
            _lastSyncTimestampLabel.value = currentTimeLabel()
            UiState.Success(list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { e ->
            Log.w(TAG, "Error observing bio experiments", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل تجارب المختبر الحيوي السحابية"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val snapshotsState: StateFlow<UiState<List<VersionSnapshot>>> = repository.observeSnapshots()
        .map<List<VersionSnapshot>, UiState<List<VersionSnapshot>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { e ->
            Log.w(TAG, "Error observing snapshots", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل سجل الإصدارات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val collectionsState: StateFlow<UiState<List<DbCollectionSchema>>> = repository.observeCollections()
        .map<List<DbCollectionSchema>, UiState<List<DbCollectionSchema>>> { list ->
            val skillsDoc = list.find { it.collectionName == SkillsMemoryEngine.FIRESTORE_MEMORY_COLLECTION_NAME }
            if (skillsDoc != null) {
                _isSkillsSyncedToFirestore.value = true
                restorePinnedSkillsFromFirestoreDoc(skillsDoc)
            }
            UiState.Success(list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { e ->
            Log.w(TAG, "Error observing collections", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل مجموعات البيانات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val buildLogsState: StateFlow<UiState<List<BuildExecutionLog>>> = repository.observeBuildLogs()
        .map<List<BuildExecutionLog>, UiState<List<BuildExecutionLog>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { e ->
            Log.w(TAG, "Error observing build logs", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذر تحميل سجلات البناء"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private var hasSeededInitialData = false

    init {
        syncUserProfileToCloud()
    }

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    private val _activeCodeTab = MutableStateFlow("HTML")
    val activeCodeTab: StateFlow<String> = _activeCodeTab.asStateFlow()

    private val _editorName = MutableStateFlow("منصة التجارة والتحليلات الذكية Full-Stack")
    val editorName: StateFlow<String> = _editorName.asStateFlow()

    private val _editorLanguage = MutableStateFlow("Full-Stack Web + Kotlin")
    val editorLanguage: StateFlow<String> = _editorLanguage.asStateFlow()

    private val _htmlBuffer = MutableStateFlow(DEFAULT_HTML)
    val htmlBuffer: StateFlow<String> = _htmlBuffer.asStateFlow()

    private val _cssBuffer = MutableStateFlow(DEFAULT_CSS)
    val cssBuffer: StateFlow<String> = _cssBuffer.asStateFlow()

    private val _jsBuffer = MutableStateFlow(DEFAULT_JS)
    val jsBuffer: StateFlow<String> = _jsBuffer.asStateFlow()

    private val _backendBuffer = MutableStateFlow(DEFAULT_BACKEND)
    val backendBuffer: StateFlow<String> = _backendBuffer.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(
        listOf(
            "[NovaForge Cloud] Runtime container initialized (Node 22 / Kotlin 2.2 / Python 3.12)",
            "[Live Preview] WebView DOM & JS Bridge ready."
        )
    )
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _activeBranch = MutableStateFlow("main")
    val activeBranch: StateFlow<String> = _activeBranch.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome_1",
                isUser = false,
                text = "مرحباً بك في NovaForge AI Studio. يمكن استخدام Gemini عبر Firebase AI Logic، ويتاح OpenAI بعد إعداد مفتاحه في بيئة البناء. يعتمد وضع المحرك المزدوج على تهيئة المزودين المتاحين.",
                modelUsed = GeminiAiRepository.MODEL_DUAL_ENGINE,
                timestampLabel = currentTimeLabel()
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedChatModel = MutableStateFlow(GeminiAiRepository.MODEL_DUAL_ENGINE)
    val selectedChatModel: StateFlow<String> = _selectedChatModel.asStateFlow()

    private val _selectedPersona = MutableStateFlow(AiRolePersona.FULLSTACK_ARCHITECT)
    val selectedPersona: StateFlow<AiRolePersona> = _selectedPersona.asStateFlow()

    private val _useSearchGrounding = MutableStateFlow(true)
    val useSearchGrounding: StateFlow<Boolean> = _useSearchGrounding.asStateFlow()

    private val _useHighThinking = MutableStateFlow(true)
    val useHighThinking: StateFlow<Boolean> = _useHighThinking.asStateFlow()

    // Multimodal Media Studio state
    private val _generatedAssets = MutableStateFlow<List<GeneratedMediaAsset>>(emptyList())
    val generatedAssets: StateFlow<List<GeneratedMediaAsset>> = _generatedAssets.asStateFlow()

    private val _mediaAnalysisOutput = MutableStateFlow<String?>(null)
    val mediaAnalysisOutput: StateFlow<String?> = _mediaAnalysisOutput.asStateFlow()

    private val _liveVoiceHistory = MutableStateFlow<List<String>>(emptyList())
    val liveVoiceHistory: StateFlow<List<String>> = _liveVoiceHistory.asStateFlow()

    private val _lastTranscribedText = MutableStateFlow("")
    val lastTranscribedText: StateFlow<String> = _lastTranscribedText.asStateFlow()

    // Bioscience Lab state
    private val _bioscienceOutput = MutableStateFlow<String?>(null)
    val bioscienceOutput: StateFlow<String?> = _bioscienceOutput.asStateFlow()

    // Skills & Tools Memory Hub state
    private val _skillsCatalog = MutableStateFlow(
        SkillsMemoryCatalog(
            archiveName = "",
            totalArchiveFiles = 0,
            totalSkills = 0,
            mcpServerUrl = "",
            skills = emptyList(),
            globalToolsAndConfigs = emptyList(),
            allFilePaths = emptyList()
        )
    )
    val skillsCatalog: StateFlow<SkillsMemoryCatalog> = _skillsCatalog.asStateFlow()

    private val _pinnedSkillIds = MutableStateFlow(emptySet<String>())
    val pinnedSkillIds: StateFlow<Set<String>> = _pinnedSkillIds.asStateFlow()

    private val _autoInjectSkillsEnabled = MutableStateFlow(true)
    val autoInjectSkillsEnabled: StateFlow<Boolean> = _autoInjectSkillsEnabled.asStateFlow()

    private val _lastAppliedSkillIds = MutableStateFlow<List<String>>(
        listOf("expo-overview", "expo-ui", "expo-design-system")
    )
    val lastAppliedSkillIds: StateFlow<List<String>> = _lastAppliedSkillIds.asStateFlow()

    private val _isSkillsSyncedToFirestore = MutableStateFlow(false)
    val isSkillsSyncedToFirestore: StateFlow<Boolean> = _isSkillsSyncedToFirestore.asStateFlow()

    private var hasInitializedSkillsCatalog = false

    fun initializeSkillsMemory(catalog: SkillsMemoryCatalog) {
        if (hasInitializedSkillsCatalog && _skillsCatalog.value.skills.isNotEmpty()) return
        hasInitializedSkillsCatalog = true
        _skillsCatalog.value = catalog
        val defaultPinned = catalog.skills.filter { it.isPinnedByDefault }.map { it.id }.toSet()
        if (defaultPinned.isNotEmpty() && !_isSkillsSyncedToFirestore.value) {
            _pinnedSkillIds.value = defaultPinned
        }
        appendConsoleLog("[Skills Memory Hub] Loaded ${catalog.totalSkills} skills & ${catalog.totalArchiveFiles} files into active studio memory.")
        // Auto-sync to Firestore if not yet persisted
        viewModelScope.launch {
            delay(800L)
            if (!_isSkillsSyncedToFirestore.value) {
                syncSkillsMemoryToFirestore(silent = true)
            }
        }
    }

    fun toggleAutoInjectSkills(enabled: Boolean) {
        _autoInjectSkillsEnabled.value = enabled
        val availableSkills = _skillsCatalog.value.totalSkills
        _statusBanner.value = when {
            !enabled -> "تم إيقاف الاستدعاء التلقائي للمهارات."
            availableSkills == 0 -> "تم تفعيل الخيار، لكن لا توجد مهارات محمّلة للاستدعاء."
            else -> "تم تفعيل الاستدعاء التلقائي لـ $availableSkills مهارة محمّلة."
        }
    }

    fun togglePinSkillInMemory(skillId: String) {
        val current = _pinnedSkillIds.value.toMutableSet()
        val added = if (skillId in current) {
            current.remove(skillId)
            false
        } else {
            current.add(skillId)
            true
        }
        _pinnedSkillIds.value = current
        _statusBanner.value = if (added) {
            "📌 تم تثبيت المهارة «$skillId» في الذاكرة الدائمة"
        } else {
            "تم إلغاء تثبيت المهارة «$skillId» من الذاكرة الدائمة"
        }
        syncSkillsMemoryToFirestore(silent = true)
    }

    fun syncSkillsMemoryToFirestore(silent: Boolean = false) {
        viewModelScope.launch {
            val catalog = _skillsCatalog.value
            if (catalog.skills.isEmpty()) return@launch
            val pinned = _pinnedSkillIds.value
            val projectId = _selectedProjectId.value ?: "proj_skills_memory"
            val fieldsSpec = "skillId:string, titleAr:string, category:string, isPinned:boolean, referencesCount:int, scriptsCount:int | Pinned=${pinned.joinToString(",")}"
            val recordsJson = SkillsMemoryEngine.buildFirestoreRegistryJson(pinned, catalog)
            val existingList = (collectionsState.value as? UiState.Success)?.data.orEmpty()
            val existingDoc = existingList.find { it.collectionName == SkillsMemoryEngine.FIRESTORE_MEMORY_COLLECTION_NAME }

            val result = if (existingDoc != null && existingDoc.createdAt != null) {
                repository.updateCollectionSchema(
                    existingDoc.copy(
                        fieldsSpec = fieldsSpec.take(5000),
                        sampleRecordsJson = recordsJson.take(20000),
                        indexConfig = "MCP:${catalog.mcpServerUrl} | TotalSkills:${catalog.totalSkills} | Files:${catalog.totalArchiveFiles}"
                    )
                )
            } else {
                repository.createCollectionSchema(
                    projectId = projectId,
                    collectionName = SkillsMemoryEngine.FIRESTORE_MEMORY_COLLECTION_NAME,
                    fieldsSpec = fieldsSpec.take(5000),
                    sampleRecordsJson = recordsJson.take(20000),
                    indexConfig = "MCP:${catalog.mcpServerUrl} | TotalSkills:${catalog.totalSkills} | Files:${catalog.totalArchiveFiles}"
                ).map { }
            }

            result.onSuccess {
                _isSkillsSyncedToFirestore.value = true
                if (!silent) {
                    _statusBanner.value = "تمت مزامنة ${catalog.totalSkills} مهارة و${pinned.size} مهارة مثبتة مع Firestore."
                }
                appendConsoleLog(
                    "[Firestore Memory Sync] Synced ${catalog.totalSkills} skills & ${pinned.size} pinned items."
                )
            }.onFailure { error ->
                _isSkillsSyncedToFirestore.value = false
                Log.w(TAG, "Skills memory sync failed", error)
                if (!silent) {
                    _statusBanner.value = "تعذرت مزامنة المهارات: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            }
        }
    }

    private fun restorePinnedSkillsFromFirestoreDoc(doc: DbCollectionSchema) {
        val marker = "Pinned="
        val idx = doc.fieldsSpec.indexOf(marker)
        if (idx >= 0) {
            val csv = doc.fieldsSpec.substring(idx + marker.length).trim()
            val parsed = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            if (parsed.isNotEmpty()) {
                _pinnedSkillIds.value = parsed
            }
        }
    }

    fun injectSkillCodeIntoEditor(code: String, targetTab: String, label: String) {
        val cleanTab = if (targetTab in listOf("HTML", "CSS", "JS", "Backend")) targetTab else "Backend"
        when (cleanTab) {
            "HTML" -> _htmlBuffer.value = _htmlBuffer.value + "\n\n" + code
            "CSS" -> _cssBuffer.value = _cssBuffer.value + "\n\n" + code
            "JS" -> _jsBuffer.value = _jsBuffer.value + "\n\n" + code
            else -> _backendBuffer.value = _backendBuffer.value + "\n\n" + code
        }
        _activeCodeTab.value = cleanTab
        appendConsoleLog("[Skills Memory Hub] Injected '$label' into $cleanTab editor buffer.")
        _statusBanner.value = "💻 تم حقن «$label» في تبويب $cleanTab بمحرر الكود!"
    }

    private fun buildAutoSkillContextForPrompt(queryOrCode: String): Pair<String, List<String>> {
        if (!_autoInjectSkillsEnabled.value) return "" to emptyList()
        val catalog = _skillsCatalog.value
        val matched = SkillsMemoryEngine.matchRelevantSkills(
            queryOrCode = queryOrCode,
            pinnedSkillIds = _pinnedSkillIds.value,
            allSkills = catalog.skills,
            maxMatched = 4
        )
        if (matched.isEmpty()) return "" to emptyList()
        val ids = matched.map { it.id }
        _lastAppliedSkillIds.value = ids
        val block = SkillsMemoryEngine.buildSkillsContextBlock(matched, catalog.mcpServerUrl)
        return block to ids
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun selectCodeTab(tab: String) {
        _activeCodeTab.value = tab
    }

    fun setActiveBranch(branch: String) {
        _activeBranch.value = branch
        appendConsoleLog("[Git/VCS] Switched active branch to '$branch'")
        _statusBanner.value = "تم التبديل إلى الفرع البرمجي: $branch"
    }

    fun appendConsoleLog(logLine: String) {
        val stamped = "[${currentTimeLabel()}] $logLine"
        _consoleLogs.value = (_consoleLogs.value + stamped).takeLast(40)
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = listOf("[${currentTimeLabel()}] Console cleared.")
    }

    fun updateActiveBuffer(newContent: String) {
        when (_activeCodeTab.value) {
            "HTML" -> _htmlBuffer.value = newContent
            "CSS" -> _cssBuffer.value = newContent
            "JS" -> _jsBuffer.value = newContent
            "Backend" -> _backendBuffer.value = newContent
        }
    }

    fun selectProject(project: StudioProject) {
        _selectedProjectId.value = project.id
        _editorName.value = project.name
        _editorLanguage.value = project.language
        _htmlBuffer.value = project.htmlCode
        _cssBuffer.value = project.cssCode
        _jsBuffer.value = project.jsCode
        _backendBuffer.value = project.backendCode
        appendConsoleLog("[Workspace] Loaded project '${project.name}' (${project.language})")
    }

    fun loadStarterTemplate(templateKey: String) {
        when (templateKey) {
            "FINTECH" -> {
                _editorName.value = "منصة التحليلات المالية الفورية FinTech"
                _editorLanguage.value = "TypeScript + Python FastAPI"
                _htmlBuffer.value = FINTECH_HTML
                _cssBuffer.value = DEFAULT_CSS
                _jsBuffer.value = FINTECH_JS
                _backendBuffer.value = FINTECH_BACKEND
            }
            "BIOMED" -> {
                _editorName.value = "بوابة المختبرات الجينومية والعيادات الذكية"
                _editorLanguage.value = "Python Biopython + React"
                _htmlBuffer.value = BIOMED_HTML
                _cssBuffer.value = DEFAULT_CSS
                _jsBuffer.value = BIOMED_JS
                _backendBuffer.value = BIOMED_BACKEND
            }
            "THREEJS_BIOGAME" -> {
                _editorName.value = "محرك الألعاب الخلوية ومستعرض الجزيئات 3D (Three.js)"
                _editorLanguage.value = "Three.js WebGL + TypeScript"
                _htmlBuffer.value = BioscienceSimulationEngine.THREEJS_GAME_HTML
                _cssBuffer.value = BioscienceSimulationEngine.THREEJS_GAME_CSS
                _jsBuffer.value = BioscienceSimulationEngine.THREEJS_GAME_JS
                _backendBuffer.value = BioscienceSimulationEngine.THREEJS_GAME_BACKEND
            }
            else -> {
                _editorName.value = "منصة التجارة والتحليلات الذكية Full-Stack"
                _editorLanguage.value = "Full-Stack Web + Kotlin"
                _htmlBuffer.value = DEFAULT_HTML
                _cssBuffer.value = DEFAULT_CSS
                _jsBuffer.value = DEFAULT_JS
                _backendBuffer.value = DEFAULT_BACKEND
            }
        }
        appendConsoleLog("[Template Engine] Applied full-stack template: $templateKey")
        saveAndBuildCurrentProject()
    }

    fun runAiSecurityAndPerformanceAudit() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val source = _htmlBuffer.value + "\n" + _cssBuffer.value + "\n" +
                    _jsBuffer.value + "\n" + _backendBuffer.value
                val checksum = computeSha256Short(source)
                val report = buildString {
                    appendLine("مراجعة أولية للمصدر فقط، وليست فحصاً أمنياً ديناميكياً:")
                    appendLine("البصمة SHA-256 المختصرة: $checksum")
                    appendLine("حجم المصدر: ${source.length} حرف")
                    appendLine("لم يتم تشغيل SAST/DAST أو اختبارات XSS/SQL injection.")
                    appendLine("لا يمكن اعتماد سلامة التطبيق دون تشغيل أدوات اختبار مخصصة.")
                }
                val projectId = _selectedProjectId.value
                if (projectId.isNullOrBlank()) {
                    _statusBanner.value = "لا يوجد مشروع محدد لمراجعته."
                    return@launch
                }
                repository.createBuildLog(
                    projectId = projectId,
                    stage = "مراجعة أولية للكود (SHA:$checksum)",
                    status = "WARNING",
                    durationMs = 0L,
                    reportSummary = report
                ).onSuccess {
                    appendConsoleLog("[Code Review] Preliminary source review saved; no security scanner was run.")
                    _statusBanner.value = "اكتملت المراجعة الأولية. لم يتم إجراء فحص أمني فعلي."
                }.onFailure { error ->
                    _statusBanner.value = "تعذر حفظ تقرير المراجعة: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Code review failed", error)
                _statusBanner.value = "تعذرت مراجعة الكود: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun triggerCloudDeploymentPipeline() {
        _statusBanner.value = "النشر السحابي غير مهيأ في هذا الإصدار. لم يتم تنفيذ نشر أو إنشاء سجل نجاح."
        appendConsoleLog("[Cloud CI/CD] Deployment blocked: no deployment provider or credentials are configured.")
    }

    fun computeSha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(5).joinToString("") { "%02x".format(it) }
    }

    fun buildCombinedLivePreviewHtml(): String {
        val html = _htmlBuffer.value
        val css = _cssBuffer.value
        val js = _jsBuffer.value
        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <style>
              $css
              </style>
            </head>
            <body>
              $html
              <script>
              try {
                console.log("NovaForge Live Preview DOM mounted successfully.");
                $js
              } catch (err) {
                console.error("Runtime Error: " + err.message);
              }
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun seedInitialWorkspace() {
        if (hasSeededInitialData) return
        hasSeededInitialData = true
        viewModelScope.launch {
            try {
                val projectId = repository.createProject(
                    name = "مشروعي الأول",
                    description = "مشروع بداية يمكنك تعديله أو استبداله.",
                    language = "HTML / CSS / JavaScript",
                    htmlCode = DEFAULT_HTML,
                    cssCode = DEFAULT_CSS,
                    jsCode = DEFAULT_JS,
                    backendCode = DEFAULT_BACKEND
                ).getOrElse { error -> throw error }

                _selectedProjectId.value = projectId
                repository.createSnapshot(
                    projectId = projectId,
                    commitMessage = "الإصدار الأولي",
                    versionTag = "v1.0.0",
                    htmlCode = DEFAULT_HTML,
                    cssCode = DEFAULT_CSS,
                    jsCode = DEFAULT_JS,
                    backendCode = DEFAULT_BACKEND,
                    addedLines = (
                        DEFAULT_HTML.lines().size + DEFAULT_CSS.lines().size +
                            DEFAULT_JS.lines().size + DEFAULT_BACKEND.lines().size
                        ).toLong(),
                    deletedLines = 0L
                ).onFailure { error ->
                    Log.w(TAG, "Initial project snapshot failed", error)
                }
                _statusBanner.value = "تم إنشاء مشروع البداية."
            } catch (error: Exception) {
                hasSeededInitialData = false
                Log.e(TAG, "Initial workspace creation failed", error)
                _statusBanner.value = "تعذر إنشاء مشروع البداية: ${error.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    private suspend fun persistCurrentProject(): String {
        val startedAt = System.currentTimeMillis()
        val projects = (projectsState.value as? UiState.Success)?.data.orEmpty()
        val existing = projects.firstOrNull { it.id == _selectedProjectId.value }
        val result = if (existing != null && existing.createdAt != null) {
            repository.updateProject(
                existing.copy(
                    name = _editorName.value,
                    language = _editorLanguage.value,
                    htmlCode = _htmlBuffer.value,
                    cssCode = _cssBuffer.value,
                    jsCode = _jsBuffer.value,
                    backendCode = _backendBuffer.value
                )
            ).map { existing.id }
        } else {
            repository.createProject(
                name = _editorName.value,
                description = "Full-Stack Project (${_editorLanguage.value})",
                language = _editorLanguage.value,
                htmlCode = _htmlBuffer.value,
                cssCode = _cssBuffer.value,
                jsCode = _jsBuffer.value,
                backendCode = _backendBuffer.value
            )
        }
        val projectId = result.getOrElse { error -> throw error }
        _selectedProjectId.value = projectId
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        val source = _htmlBuffer.value + _cssBuffer.value + _jsBuffer.value + _backendBuffer.value
        val checksum = computeSha256Short(source)
        val report = "تم حفظ ملفات المشروع في Firestore. لم يتم تشغيل مترجم أو بناء APK/حزمة نشر.\n" +
            "حجم المصدر: ${source.length} حرف؛ SHA-256 مختصر: $checksum"
        val logResult = repository.createBuildLog(
            projectId = projectId,
            stage = "حفظ ملفات المشروع",
            status = "WARNING",
            durationMs = duration,
            reportSummary = report
        )
        if (logResult.isFailure) {
            Log.w(TAG, "Project saved but activity log failed", logResult.exceptionOrNull())
        }
        appendConsoleLog("[Project Save] Files saved for project $projectId. No compiler/build was run.")
        return "تم حفظ ملفات المشروع في Firestore بنجاح. لم يتم تشغيل مترجم أو إنشاء APK."
    }

    fun saveAndBuildCurrentProject() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                _statusBanner.value = persistCurrentProject()
            } catch (error: Exception) {
                Log.e(TAG, "Project save failed", error)
                _statusBanner.value = "تعذر حفظ المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun createNewProject(name: String, description: String, language: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val generated = aiRepository.generateFullStackCodebase(
                    projectBrief = "$name - $description",
                    stackLanguage = language,
                    preferredModel = _selectedChatModel.value
                )
                val html = generated["html"] ?: DEFAULT_HTML
                val css = generated["css"] ?: DEFAULT_CSS
                val js = generated["js"] ?: DEFAULT_JS
                val backend = generated["backend"] ?: DEFAULT_BACKEND
                val projectId = repository.createProject(
                    name = name,
                    description = description,
                    language = language,
                    htmlCode = html,
                    cssCode = css,
                    jsCode = js,
                    backendCode = backend
                ).getOrElse { error -> throw error }

                _selectedProjectId.value = projectId
                _editorName.value = name
                _editorLanguage.value = language
                _htmlBuffer.value = html
                _cssBuffer.value = css
                _jsBuffer.value = js
                _backendBuffer.value = backend
                repository.createSnapshot(
                    projectId = projectId,
                    commitMessage = "إنشاء مشروع بواسطة ${_selectedChatModel.value}",
                    versionTag = "v1.0.0",
                    htmlCode = html,
                    cssCode = css,
                    jsCode = js,
                    backendCode = backend,
                    addedLines = (html.lines().size + css.lines().size + js.lines().size + backend.lines().size).toLong(),
                    deletedLines = 0L
                ).onFailure { error ->
                    Log.w(TAG, "Project created but initial snapshot failed", error)
                }
                appendConsoleLog("[AI Generator] Created project '$name' (files saved; no compiler run).")
                _statusBanner.value = "تم إنشاء المشروع وحفظ ملفاته. لم يتم تشغيل مترجم أو نشر."
            } catch (error: Exception) {
                Log.e(TAG, "Project creation failed", error)
                _statusBanner.value = "تعذر إنشاء المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(projectId).onSuccess {
                    if (_selectedProjectId.value == projectId) {
                        _selectedProjectId.value = null
                    }
                    _statusBanner.value = "تم حذف المشروع من السحابة."
                }.onFailure { error ->
                    Log.e(TAG, "Project deletion failed", error)
                    _statusBanner.value = "تعذر حذف المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Project deletion failed", error)
                _statusBanner.value = "تعذر حذف المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    fun triggerInlineCodeSuggestion(instruction: String = "أضف ميزة مراقبة فورية وتحسين أداء") {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val currentTab = _activeCodeTab.value
                val currentContent = when (currentTab) {
                    "HTML" -> _htmlBuffer.value
                    "CSS" -> _cssBuffer.value
                    "JS" -> _jsBuffer.value
                    else -> _backendBuffer.value
                }
                val (skillsBlock, appliedIds) = buildAutoSkillContextForPrompt("$instruction\n$currentContent")
                val enrichedInstruction = if (skillsBlock.isNotBlank()) "$instruction\n\n$skillsBlock" else instruction
                val snippet = aiRepository.generateLowLatencyCodeSuggestion(
                    currentCode = currentContent,
                    activeTab = currentTab,
                    instruction = enrichedInstruction,
                    preferredModel = _selectedChatModel.value
                )
                updateActiveBuffer(currentContent + "\n\n" + snippet)
                val skillsNote = if (appliedIds.isNotEmpty()) " [Skills: ${appliedIds.joinToString(", ")}]" else ""
                appendConsoleLog("[AI Copilot] Added suggestion to $currentTab via ${_selectedChatModel.value}$skillsNote")
                _statusBanner.value = "تم إنشاء اقتراح برمجي وإضافته إلى المحرر."
            } catch (error: Exception) {
                Log.e(TAG, "Inline code suggestion failed", error)
                _statusBanner.value = "تعذر إنشاء الاقتراح: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun generateFullStackWithHighThinking(prompt: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val (skillsBlock, appliedIds) = buildAutoSkillContextForPrompt(prompt)
                val enrichedPrompt = if (skillsBlock.isNotBlank()) "$prompt\n\n$skillsBlock" else prompt
                val generated = aiRepository.generateFullStackCodebase(
                    projectBrief = enrichedPrompt,
                    stackLanguage = _editorLanguage.value,
                    preferredModel = _selectedChatModel.value
                )
                val html = generated["html"] ?: _htmlBuffer.value
                val css = generated["css"] ?: _cssBuffer.value
                val js = generated["js"] ?: _jsBuffer.value
                val backend = generated["backend"] ?: _backendBuffer.value
                val projectId = _selectedProjectId.value
                if (projectId.isNullOrBlank()) {
                    _statusBanner.value = "احفظ المشروع أولاً قبل تحديث ملفات المشروع المولدة."
                    return@launch
                }
                val current = (projectsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == projectId }
                if (current == null || current.createdAt == null) {
                    _statusBanner.value = "تعذر العثور على المشروع لحفظ الملفات المولدة."
                    return@launch
                }
                repository.updateProject(
                    current.copy(htmlCode = html, cssCode = css, jsCode = js, backendCode = backend)
                ).onSuccess {
                    _htmlBuffer.value = html
                    _cssBuffer.value = css
                    _jsBuffer.value = js
                    _backendBuffer.value = backend
                    _statusBanner.value = "تم توليد الملفات وحفظها. لم يتم تشغيل مترجم أو نشر. المهارات: ${appliedIds.take(3).joinToString(", ")}"
                }.onFailure { error ->
                    _statusBanner.value = "تم التوليد لكن تعذر حفظ الملفات: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Full-stack generation failed", error)
                _statusBanner.value = "تعذر توليد المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun commitVersionSnapshot(commitMessage: String, versionTag: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val projectId = _selectedProjectId.value
                if (projectId.isNullOrBlank()) {
                    _statusBanner.value = "احفظ المشروع أولاً قبل إنشاء لقطة إصدار."
                    return@launch
                }
                val previousSnap = (snapshotsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.projectId == projectId }
                val prevLines = (previousSnap?.htmlCode.orEmpty() + "\n" + previousSnap?.cssCode.orEmpty() +
                    "\n" + previousSnap?.jsCode.orEmpty() + "\n" + previousSnap?.backendCode.orEmpty()).lines()
                val currLines = (_htmlBuffer.value + "\n" + _cssBuffer.value + "\n" +
                    _jsBuffer.value + "\n" + _backendBuffer.value).lines()
                val added = currLines.count { it !in prevLines }.toLong()
                val deleted = prevLines.count { it !in currLines }.toLong()
                val tag = versionTag.ifBlank {
                    "v1.${((snapshotsState.value as? UiState.Success)?.data?.size ?: 0) + 1}.0"
                }
                repository.createSnapshot(
                    projectId = projectId,
                    commitMessage = "[${_activeBranch.value}] " + commitMessage.ifBlank { "تحديث الكود والمخططات" },
                    versionTag = tag,
                    htmlCode = _htmlBuffer.value,
                    cssCode = _cssBuffer.value,
                    jsCode = _jsBuffer.value,
                    backendCode = _backendBuffer.value,
                    addedLines = added,
                    deletedLines = deleted
                ).onSuccess {
                    appendConsoleLog("[Version Snapshot] Saved $tag (+$added/-$deleted)")
                    _statusBanner.value = "تم حفظ لقطة الإصدار $tag."
                }.onFailure { error ->
                    Log.e(TAG, "Version snapshot failed", error)
                    _statusBanner.value = "تعذر حفظ لقطة الإصدار: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Version snapshot failed", error)
                _statusBanner.value = "تعذر حفظ لقطة الإصدار: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun restoreVersionSnapshot(snapshot: VersionSnapshot) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val projectId = _selectedProjectId.value
                if (projectId.isNullOrBlank()) {
                    _statusBanner.value = "اختر مشروعاً قبل استعادة الإصدار."
                    return@launch
                }
                val current = (projectsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == projectId }
                if (current == null || current.createdAt == null) {
                    _statusBanner.value = "تعذر العثور على المشروع الحالي لحفظ النسخة المستعادة."
                    return@launch
                }
                repository.updateProject(
                    current.copy(
                        htmlCode = snapshot.htmlCode,
                        cssCode = snapshot.cssCode,
                        jsCode = snapshot.jsCode,
                        backendCode = snapshot.backendCode
                    )
                ).onSuccess {
                    _htmlBuffer.value = snapshot.htmlCode
                    _cssBuffer.value = snapshot.cssCode
                    _jsBuffer.value = snapshot.jsCode
                    _backendBuffer.value = snapshot.backendCode
                    appendConsoleLog("[VCS Restore] Restored ${snapshot.versionTag} to cloud project.")
                    _statusBanner.value = "تمت استعادة الإصدار ${snapshot.versionTag} وحفظه."
                }.onFailure { error ->
                    Log.e(TAG, "Version restore failed", error)
                    _statusBanner.value = "تعذرت استعادة الإصدار: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Version restore failed", error)
                _statusBanner.value = "تعذرت استعادة الإصدار: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun computeDiffAgainstCurrent(snapshot: VersionSnapshot): List<DiffLine> {
        val oldLines = (snapshot.htmlCode + "\n" + snapshot.jsCode + "\n" + snapshot.backendCode).lines()
        val newLines = (_htmlBuffer.value + "\n" + _jsBuffer.value + "\n" + _backendBuffer.value).lines()
        val result = mutableListOf<DiffLine>()
        val maxLen = maxOf(oldLines.size, newLines.size).coerceAtMost(80)
        for (i in 0 until maxLen) {
            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)
            when {
                oldLine == newLine && oldLine != null -> {
                    result.add(DiffLine(i + 1, "  $oldLine", DiffType.UNCHANGED))
                }
                oldLine != null && newLine != null -> {
                    result.add(DiffLine(i + 1, "- $oldLine", DiffType.DELETED))
                    result.add(DiffLine(i + 1, "+ $newLine", DiffType.ADDED))
                }
                oldLine != null -> {
                    result.add(DiffLine(i + 1, "- $oldLine", DiffType.DELETED))
                }
                newLine != null -> {
                    result.add(DiffLine(i + 1, "+ $newLine", DiffType.ADDED))
                }
            }
        }
        return result
    }

    fun createDatabaseCollection(
        collectionName: String,
        fieldsSpec: String,
        sampleRecordsJson: String,
        indexConfig: String
    ) {
        viewModelScope.launch {
            val projectId = _selectedProjectId.value ?: "proj_default"
            repository.createCollectionSchema(
                projectId = projectId,
                collectionName = collectionName,
                fieldsSpec = fieldsSpec,
                sampleRecordsJson = sampleRecordsJson,
                indexConfig = indexConfig
            )
            _statusBanner.value = "تم إنشاء مجموعة البيانات «$collectionName» في قاعدة البيانات السحابية"
        }
    }

    fun appendRecordToCollection(schema: DbCollectionSchema, newRecordJsonObject: String) {
        viewModelScope.launch {
            val currentJson = schema.sampleRecordsJson.trim()
            val updatedJson = if (currentJson.endsWith("]") && currentJson.length > 2) {
                currentJson.removeSuffix("]") + "," + newRecordJsonObject + "]"
            } else {
                "[$newRecordJsonObject]"
            }
            if (schema.createdAt != null) {
                repository.updateCollectionSchema(schema.copy(sampleRecordsJson = updatedJson))
                _statusBanner.value = "تم إدراج السجل الجديد في «${schema.collectionName}»"
            }
        }
    }

    fun generateBackendCodeFromSchema(schema: DbCollectionSchema) {
        val generatedRepoCode = """
            // Generated Data Access Layer & REST Endpoint for Collection: ${schema.collectionName}
            // Schema Fields: ${schema.fieldsSpec}
            // Index Strategy: ${schema.indexConfig}
            class ${schema.collectionName.replaceFirstChar { it.uppercase() }}Repository(private val db: FirebaseFirestore) {
                fun observeRecords(userId: String) =
                    db.collection("users").document(userId)
                      .collection("${schema.collectionName}")
                      .whereEqualTo("userId", userId)
                      .snapshots()
            }
        """.trimIndent()
        _backendBuffer.value = _backendBuffer.value + "\n\n" + generatedRepoCode
        _activeCodeTab.value = "Backend"
        _statusBanner.value = "تم توليد كود الواجهة الخلفية من جدول «${schema.collectionName}» وإضافته للمحرر!"
    }

    fun deleteDatabaseCollection(collectionId: String) {
        viewModelScope.launch {
            repository.deleteCollectionSchema(collectionId)
            _statusBanner.value = "تم حذف مجموعة البيانات."
        }
    }

    fun setChatModel(model: String) {
        val normalized = GeminiAiRepository.normalizePreferredModel(model)
        hasRestoredPreferredModelFromCloud = true
        _selectedChatModel.value = normalized
        aiRepository.setActiveModel(normalized)
        val provider = AiEngineProvider.fromModelId(normalized)
        appendConsoleLog("[AI Prompt Service] Injected ${provider.shortBadge} client ($normalized) & syncing preference to Firestore.")
        _statusBanner.value = "🤖 تم تفعيل محرك الذكاء الاصطناعي: ${provider.titleAr} ($normalized) وحفظه في ملفك السحابي"
        viewModelScope.launch {
            repository.updatePreferredAiModel(normalized).onSuccess {
                _lastSyncTimestampLabel.value = currentTimeLabel()
            }
        }
    }

    fun toggleAiProvider(useChatGpt: Boolean) {
        val targetModel = if (useChatGpt) {
            GeminiAiRepository.MODEL_GPT_4O
        } else {
            GeminiAiRepository.MODEL_FLASH
        }
        setChatModel(targetModel)
    }

    fun selectAiEngineProvider(provider: AiEngineProvider) {
        setChatModel(provider.defaultModelId)
    }

    fun setChatPersona(persona: AiRolePersona) {
        _selectedPersona.value = persona
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _useSearchGrounding.value = enabled
    }

    fun toggleHighThinking(enabled: Boolean) {
        _useHighThinking.value = enabled
    }

    private suspend fun executeAgentTask(task: AgentTaskType, originalPrompt: String): Pair<String, List<String>> {
        return when (task) {
            AgentTaskType.SAVE_PROJECT -> {
                val result = persistCurrentProject()
                result to listOf("Cyber Agent Workspace Executor", "Firestore Project Save")
            }
            AgentTaskType.CREATE_SNAPSHOT -> {
                val projectId = _selectedProjectId.value
                    ?: throw IllegalStateException("احفظ المشروع أولاً قبل إنشاء لقطة إصدار.")
                val projectSnapshots = (snapshotsState.value as? UiState.Success)?.data
                    .orEmpty()
                    .filter { it.projectId == projectId }
                val previous = projectSnapshots.firstOrNull()
                val previousLines = (
                    previous?.htmlCode.orEmpty() + "\n" + previous?.cssCode.orEmpty() +
                        "\n" + previous?.jsCode.orEmpty() + "\n" + previous?.backendCode.orEmpty()
                    ).lines()
                val currentLines = (
                    _htmlBuffer.value + "\n" + _cssBuffer.value + "\n" +
                        _jsBuffer.value + "\n" + _backendBuffer.value
                    ).lines()
                val added = currentLines.count { it !in previousLines }.toLong()
                val deleted = previousLines.count { it !in currentLines }.toLong()
                val versionTag = "v1.${projectSnapshots.size + 1}.0"
                val snapshotId = repository.createSnapshot(
                    projectId = projectId,
                    commitMessage = originalPrompt.take(300),
                    versionTag = versionTag,
                    htmlCode = _htmlBuffer.value,
                    cssCode = _cssBuffer.value,
                    jsCode = _jsBuffer.value,
                    backendCode = _backendBuffer.value,
                    addedLines = added,
                    deletedLines = deleted
                ).getOrElse { error -> throw error }
                appendConsoleLog("[Agent Task] Created snapshot $versionTag ($snapshotId).")
                "تم إنشاء لقطة الإصدار $versionTag وحفظها في Firestore." to
                    listOf("Cyber Agent Workspace Executor", "Firestore Version Snapshot")
            }
            AgentTaskType.REVIEW_CODE -> {
                val source = buildString {
                    appendLine("HTML:\n${_htmlBuffer.value}")
                    appendLine("CSS:\n${_cssBuffer.value}")
                    appendLine("JavaScript:\n${_jsBuffer.value}")
                    appendLine("Backend:\n${_backendBuffer.value}")
                }
                val reviewPrompt = """
                    راجع الشيفرة التالية مراجعة أمنية وبرمجية دقيقة.
                    صنّف النتائج إلى: أخطاء مؤكدة من النص، مخاطر محتملة تحتاج تحققاً، وتحسينات.
                    لا تدّعِ تشغيل أدوات SAST أو DAST أو تنفيذ الاختبارات.
                    لا تخترع نتائج تشغيل. اربط كل ملاحظة بمقطع أو ملف واضح.
                    أعد خطوات إصلاح عملية.
                    $source
                """.trimIndent()
                val (review, sources) = aiRepository.sendMultiTurnChat(
                    history = emptyList(),
                    userPrompt = reviewPrompt,
                    selectedModel = _selectedChatModel.value,
                    persona = AiRolePersona.SECURITY_AUDITOR,
                    useSearchGrounding = false,
                    useHighThinking = true
                )
                "مراجعة برمجية بمساعدة الذكاء الاصطناعي (ليست فحصاً ديناميكياً أو SAST/DAST):\n\n$review" to
                    (listOf("Cyber Agent AI Code Review") + sources).distinct()
            }
            AgentTaskType.EDIT_ACTIVE_CODE -> {
                val tab = _activeCodeTab.value
                val currentCode = when (tab) {
                    "HTML" -> _htmlBuffer.value
                    "CSS" -> _cssBuffer.value
                    "JS" -> _jsBuffer.value
                    else -> _backendBuffer.value
                }
                val suggestion = aiRepository.generateLowLatencyCodeSuggestion(
                    currentCode = currentCode,
                    activeTab = tab,
                    instruction = originalPrompt,
                    preferredModel = _selectedChatModel.value
                ).trim()
                require(suggestion.isNotBlank()) { "لم يرجع نموذج الذكاء الاصطناعي أي تعديل." }
                updateActiveBuffer(
                    if (currentCode.isBlank()) suggestion else "$currentCode\n\n$suggestion"
                )
                val saveResult = persistCurrentProject()
                "تم تطبيق التعديل المقترح على تبويب $tab وحفظ ملفات المشروع.\n$saveResult\n\nراجع التغيير في المحرر قبل تشغيله." to
                    listOf("Cyber Agent Code Editor", "AI Code Suggestion", "Firestore Project Save")
            }
        }
    }

    private fun appendAgentReply(text: String, model: String, sources: List<String>) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = text,
            modelUsed = model,
            timestampLabel = currentTimeLabel(),
            groundingSources = sources
        )
    }

    fun sendChatMessage(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _isBusy.value) return

        val currentHistory = _chatMessages.value
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanPrompt,
            modelUsed = "User",
            timestampLabel = currentTimeLabel()
        )
        _chatMessages.value = currentHistory + userMsg
        val agentTask = AgentTaskRouter.route(cleanPrompt)

        viewModelScope.launch {
            _isBusy.value = true
            try {
                if (agentTask != null) {
                    val (reply, sources) = executeAgentTask(agentTask, cleanPrompt)
                    appendAgentReply(reply, "Cyber Agent • ${agentTask.name}", sources)
                    _statusBanner.value = "اكتملت المهمة ونُفذت إجراءاتها داخل مساحة العمل."
                    return@launch
                }

                val (skillsBlock, appliedIds) = buildAutoSkillContextForPrompt(cleanPrompt)
                val enrichedPrompt = if (skillsBlock.isNotBlank()) {
                    "$cleanPrompt\n\n$skillsBlock"
                } else {
                    cleanPrompt
                }
                val (replyText, sources) = aiRepository.sendMultiTurnChat(
                    history = currentHistory,
                    userPrompt = enrichedPrompt,
                    selectedModel = _selectedChatModel.value,
                    persona = _selectedPersona.value,
                    useSearchGrounding = _useSearchGrounding.value,
                    useHighThinking = _useHighThinking.value
                )
                val effectiveModel = when {
                    GeminiAiRepository.isDualEngineModel(_selectedChatModel.value) ->
                        GeminiAiRepository.MODEL_DUAL_ENGINE
                    GeminiAiRepository.isOpenAiModel(_selectedChatModel.value) ->
                        _selectedChatModel.value
                    _useHighThinking.value -> GeminiAiRepository.MODEL_ARGON
                    _useSearchGrounding.value -> GeminiAiRepository.MODEL_FLASH
                    else -> _selectedChatModel.value
                }
                appendAgentReply(
                    replyText,
                    effectiveModel,
                    (appliedIds.map { "SkillMemory:$it" } + sources).distinct()
                )
                _statusBanner.value = "وصل رد من مزود الذكاء الاصطناعي."
            } catch (error: Exception) {
                Log.e(TAG, "Chat or agent task failed", error)
                val errorText = "لم تُنفذ المهمة. السبب: ${error.localizedMessage ?: "خطأ غير معروف"}"
                appendAgentReply(errorText, "Task Error", listOf("Execution failed"))
                _statusBanner.value = errorText
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun applySkillInAiChat(skill: ExtractedSkillItem) {
        val prompt = buildString {
            appendLine("قم بتطبيق مهارة «${skill.id} — ${skill.titleAr}» (${skill.category}) وتوليد كود تطبيقي متكامل يتبع جميع قواعدها ومراجعها:")
            appendLine("• الوصف: ${skill.descriptionEn}")
            if (skill.packages.isNotEmpty()) {
                appendLine("• الحزم المعتمدة: ${skill.packages.joinToString(", ")}")
            }
            if (skill.references.isNotEmpty()) {
                appendLine("• المراجع الفرعية المدمجة: ${skill.references.joinToString(", ") { it.fileName }}")
            }
        }
        sendChatMessage(prompt)
    }

    fun generateImageAsset(
        prompt: String,
        useStudioPro: Boolean,
        aspectRatio: String,
        imageSize: String,
        sourceBase64: String? = null,
        sourceMime: String? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val asset = aiRepository.generateOrEditImage(
                    prompt = prompt,
                    useStudioProModel = useStudioPro,
                    aspectRatio = aspectRatio,
                    imageSize = imageSize,
                    sourceImageBase64 = sourceBase64,
                    sourceMimeType = sourceMime
                )
                _generatedAssets.value = listOf(asset) + _generatedAssets.value
                _statusBanner.value = "تم توليد الصورة عبر ${asset.modelUsed} (${asset.metadataLabel})"
            } catch (error: Exception) {
                Log.e(TAG, "Image generation failed", error)
                _statusBanner.value = "تعذر توليد الصورة: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun generateOrAnimateVideoAsset(
        prompt: String,
        aspectRatio: String,
        sourcePhotoBase64: String? = null,
        sourcePhotoMime: String? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val asset = aiRepository.generateOrAnimateVideo(
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    sourcePhotoBase64 = sourcePhotoBase64,
                    sourcePhotoMime = sourcePhotoMime
                )
                _generatedAssets.value = listOf(asset) + _generatedAssets.value
                _statusBanner.value = "تم إعداد ملف الفيديو عبر ${asset.modelUsed} ($aspectRatio)"
            } catch (error: Exception) {
                Log.e(TAG, "Video generation failed", error)
                _statusBanner.value = "تعذر توليد الفيديو: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun analyzeUploadedMedia(
        prompt: String,
        base64Data: String,
        mimeType: String,
        isVideo: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                _mediaAnalysisOutput.value = aiRepository.analyzeVisualMediaWithArgon(
                    prompt = prompt,
                    mediaBase64 = base64Data,
                    mimeType = mimeType,
                    isVideo = isVideo
                )
                _statusBanner.value = "اكتمل تحليل الوسائط عبر ${GeminiAiRepository.MODEL_ARGON}"
            } catch (error: Exception) {
                Log.e(TAG, "Media analysis failed", error)
                _statusBanner.value = "تعذر تحليل الوسائط: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun transcribeRecordedAudio(audioBase64: String, onTranscribed: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val text = aiRepository.transcribeMicrophoneAudio(audioBase64)
                _lastTranscribedText.value = text
                onTranscribed(text)
                _statusBanner.value = "تم نسخ الصوت عبر ${GeminiAiRepository.MODEL_FLASH}"
            } catch (error: Exception) {
                Log.e(TAG, "Audio transcription failed", error)
                _statusBanner.value = "تعذر نسخ الصوت: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun sendLiveVoiceTurn(spokenText: String, audioBase64: String? = null, onReplyReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val reply = aiRepository.sendLiveVoiceConversationTurn(spokenText, audioBase64)
                _liveVoiceHistory.value = _liveVoiceHistory.value +
                    listOf("👤 أنت: $spokenText", "🤖 Gemini (رد صوتي): $reply")
                onReplyReady(reply)
            } catch (error: Exception) {
                Log.e(TAG, "Live voice turn failed", error)
                _statusBanner.value = "تعذرت المحادثة الصوتية: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun generateMusicTrack(prompt: String, useFullLengthPro: Boolean, onMusicReady: (String?) -> Unit) {
        viewModelScope.launch {
            _statusBanner.value = "توليد الموسيقى غير متاح حالياً لأن مزود إنتاج الملفات الصوتية غير مهيأ."
            onMusicReady(null)
        }
    }

    fun runBioscienceAnalysis(fastaSequence: String, researchGoal: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = aiRepository.analyzeBioscienceSequence(
                    fastaOrSequence = fastaSequence,
                    researchGoal = researchGoal,
                    preferredModel = _selectedChatModel.value
                )
                _bioscienceOutput.value = result
                repository.createBioExperiment(
                    title = researchGoal.take(80).ifBlank { "تحليل تسلسل حيوي (${_selectedChatModel.value})" },
                    workflowMode = "PROTEIN_FOLDING",
                    fastaSequence = fastaSequence,
                    researchGoal = researchGoal,
                    plddtScore = 0L,
                    freeEnergyKcal = 0.0,
                    alphaHelixPct = 0L,
                    betaSheetPct = 0L,
                    aiReportSummary = result
                ).onSuccess {
                    syncUserProfileToCloud()
                    _statusBanner.value = "اكتمل التحليل وحُفظت التجربة. المؤشرات البنيوية غير محسوبة ما لم يذكرها النموذج."
                }.onFailure { error ->
                    _statusBanner.value = "اكتمل التحليل لكن تعذر حفظه: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Bioscience analysis failed", error)
                _statusBanner.value = "تعذر تحليل التسلسل: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveBioExperimentToCloud(
        title: String,
        workflowMode: String,
        fastaSequence: String,
        researchGoal: String,
        plddtScore: Int,
        freeEnergyKcal: Double,
        alphaHelixPct: Int,
        betaSheetPct: Int,
        aiReportSummary: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.createBioExperiment(
                    title = title,
                    workflowMode = workflowMode,
                    fastaSequence = fastaSequence,
                    researchGoal = researchGoal,
                    plddtScore = plddtScore.toLong(),
                    freeEnergyKcal = freeEnergyKcal,
                    alphaHelixPct = alphaHelixPct.toLong(),
                    betaSheetPct = betaSheetPct.toLong(),
                    aiReportSummary = aiReportSummary
                ).onSuccess { expId ->
                    _lastSyncTimestampLabel.value = currentTimeLabel()
                    _statusBanner.value = "تم حفظ تجربة المختبر الحيوي ($expId) في Firestore."
                    syncUserProfileToCloud()
                }.onFailure { error ->
                    _statusBanner.value = "تعذر حفظ التجربة الحيوية: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            } catch (error: Exception) {
                Log.e(TAG, "Bio experiment save failed", error)
                _statusBanner.value = "تعذر حفظ التجربة الحيوية: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteBioExperimentFromCloud(experimentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBioExperiment(experimentId)
                    .onSuccess {
                        _lastSyncTimestampLabel.value = currentTimeLabel()
                        _statusBanner.value = "تم حذف التجربة الحيوية من السحابة."
                        syncUserProfileToCloud()
                    }
                    .onFailure { error ->
                        _statusBanner.value = "تعذر حذف التجربة الحيوية: ${error.localizedMessage ?: "خطأ غير معروف"}"
                    }
            } catch (error: Exception) {
                Log.e(TAG, "Bio experiment deletion failed", error)
                _statusBanner.value = "تعذر حذف التجربة الحيوية: ${error.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    fun syncUserProfileToCloud(
        projectsCountOverride: Int? = null,
        bioCountOverride: Int? = null
    ) {
        viewModelScope.launch {
            val projCount = projectsCountOverride
                ?: (projectsState.value as? UiState.Success)?.data?.size
                ?: 0
            val bioCount = bioCountOverride
                ?: (bioExperimentsState.value as? UiState.Success)?.data?.size
                ?: 0
            repository.upsertUserProfile(
                email = userEmail,
                displayName = userDisplayName,
                photoUrl = userPhotoUrl,
                activeBranch = _activeBranch.value,
                projectsCount = projCount.toLong(),
                bioExperimentsCount = bioCount.toLong(),
                preferredAiModel = if (hasRestoredPreferredModelFromCloud) _selectedChatModel.value else null
            ).onSuccess {
                _lastSyncTimestampLabel.value = currentTimeLabel()
            }
        }
    }

    fun forceManualCloudSync() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val latencyResult = repository.checkConnectionLatency()
                latencyResult.onSuccess { _syncLatencyMs.value = it }
                    .onFailure { error ->
                        Log.w(TAG, "Cloud latency check failed", error)
                        _statusBanner.value = "تعذر الاتصال بـ Firestore: ${error.localizedMessage ?: "خطأ غير معروف"}"
                        return@launch
                    }

                val profileCount = repository.upsertUserProfile(
                    email = userEmail,
                    displayName = userDisplayName,
                    photoUrl = userPhotoUrl,
                    activeBranch = _activeBranch.value,
                    projectsCount = (projectsState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L,
                    bioExperimentsCount = (bioExperimentsState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L,
                    preferredAiModel = if (hasRestoredPreferredModelFromCloud) _selectedChatModel.value else null
                )
                profileCount.onFailure { error ->
                    Log.w(TAG, "Cloud profile sync failed", error)
                    _statusBanner.value = "فشل تحديث الملف الشخصي: ${error.localizedMessage ?: "خطأ غير معروف"}"
                    return@launch
                }

                syncSkillsMemoryToFirestore()
                _lastSyncTimestampLabel.value = currentTimeLabel()
                _statusBanner.value = "اكتملت مزامنة الملف الشخصي والمهارات مع Firestore. زمن الاستجابة ${_syncLatencyMs.value}ms."
            } catch (error: Exception) {
                Log.e(TAG, "Manual cloud sync failed", error)
                _statusBanner.value = "فشلت المزامنة: ${error.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun currentTimeLabel(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    companion object {
        private const val TAG = "StudioViewModel"

        val DEFAULT_HTML = """
            <main class="workspace">
              <p class="eyebrow">مشروع جديد</p>
              <h1>مرحباً بك في مساحة العمل</h1>
              <p class="intro">ابدأ بتعديل هذه الصفحة أو اطلب من المساعد إنشاء واجهتك الأولى.</p>
              <section class="starter-card">
                <h2>خطوات البدء</h2>
                <ol>
                  <li>عدّل HTML وCSS وJavaScript في المحرر.</li>
                  <li>احفظ ملفات المشروع في حسابك.</li>
                  <li>استخدم المعاينة المحلية لمراجعة الواجهة.</li>
                </ol>
              </section>
            </main>
        """.trimIndent()

        val FINTECH_HTML = """
            <div class="nova-app">
              <header class="top-bar">
                <div>
                  <span class="pill">نموذج FinTech تجريبي • لا اتصال بمنصة تداول</span>
                  <h1>واجهة محاكاة محلية للتداول</h1>
                </div>
                <button class="action-btn" onclick="simulateTrade()">إضافة سجل محاكاة</button>
              </header>
              <div class="metrics-row">
                <div class="metric-box">
                  <span>السيولة الفعلية</span>
                  <strong id="liquidity-val">غير متاحة</strong>
                </div>
                <div class="metric-box">
                  <span>مؤشر المخاطر</span>
                  <strong id="risk-score">غير محسوب</strong>
                </div>
                <div class="metric-box">
                  <span>سرعة التسوية</span>
                  <strong>غير متصلة</strong>
                </div>
              </div>
              <div class="card">
                <h3>سجل المحاكاة المحلي</h3>
                <ul id="trades-list">
                  <li>لا توجد عمليات. الزر يضيف سجلاً تجريبياً فقط ولا ينفذ أوامر مالية.</li>
                </ul>
              </div>
            </div>
        """.trimIndent()

        val FINTECH_JS = """
            function simulateTrade() {
              const ul = document.getElementById('trades-list');
              const li = document.createElement('li');
              li.textContent = 'محاكاة محلية فقط: لم يتم إرسال أمر أو تنفيذ صفقة فعلية.';
              ul.prepend(li);
              console.info('Demo only: no trading order was sent.');
            }
        """.trimIndent()

        val FINTECH_BACKEND = """
            # Example only: this endpoint does not persist a ledger or settle funds.
            from fastapi import FastAPI
            app = FastAPI()

            @app.post("/api/v1/ledger/preview")
            async def preview_transaction(amount: float, asset: str):
                return {"status": "SIMULATED_ONLY", "amount": amount, "asset": asset}
        """.trimIndent()

        val BIOMED_HTML = """
            <div class="nova-app">
              <header class="top-bar">
                <div>
                  <span class="pill">واجهة BioMed تجريبية • لا يوجد تحليل متصل</span>
                  <h1>قائمة سجلات العينات</h1>
                </div>
                <button class="action-btn" onclick="addDemoSample()">إضافة سجل تجريبي</button>
              </header>
              <div class="metrics-row">
                <div class="metric-box">
                  <span>السجلات التجريبية</span>
                  <strong id="sample-count">0</strong>
                </div>
                <div class="metric-box">
                  <span>GC Content</span>
                  <strong>غير محسوب</strong>
                </div>
                <div class="metric-box">
                  <span>الثقة البنيوية</span>
                  <strong>غير متاحة</strong>
                </div>
              </div>
              <div class="card">
                <h3>سجل العرض التجريبي</h3>
                <ul id="bio-list">
                  <li>لا توجد سجلات بعد. إضافة سجل لا تشغّل Biopython أو أداة تحليل حيوية.</li>
                </ul>
              </div>
            </div>
        """.trimIndent()

        val BIOMED_JS = """
            let sampleRecords = 0;
            function addDemoSample() {
              sampleRecords++;
              document.getElementById('sample-count').textContent = String(sampleRecords);
              const ul = document.getElementById('bio-list');
              const li = document.createElement('li');
              li.textContent = 'سجل تجريبي #' + sampleRecords + ' • لم يتم تشغيل تحليل حيوي.';
              ul.prepend(li);
              console.info('Demo record added; no sequence analysis was performed.');
            }
        """.trimIndent()

        val BIOMED_BACKEND = """
            # Bioinformatics Sequence Pipeline (Python + Biopython)
            from Bio.Seq import Seq
            def analyze_fasta(raw_seq: str):
                s = Seq(raw_seq)
                return {"length": len(s), "protein": str(s.translate(to_stop=True))}
        """.trimIndent()

        val DEFAULT_CSS = """
            * { box-sizing: border-box; font-family: system-ui, sans-serif; }
            body { margin: 0; padding: 14px; background: #0B0F17; color: #F1F5F9; }
            .top-bar {
              display: flex; justify-content: space-between; align-items: center;
              background: linear-gradient(135deg, #131B2E, #1F1638);
              padding: 16px; border-radius: 14px; border: 1px solid rgba(0,229,255,0.35);
              margin-bottom: 14px;
            }
            .pill {
              background: rgba(0,229,255,0.15); color: #00E5FF;
              padding: 3px 10px; border-radius: 99px; font-size: 11px; font-weight: 700;
            }
            h1 { margin: 6px 0 0; font-size: 18px; }
            .action-btn, button {
              background: #00E5FF; color: #0B0F17; border: none;
              padding: 8px 14px; border-radius: 8px; font-weight: 700; cursor: pointer;
            }
            .metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 14px; }
            .metric-box {
              background: #131B2E; padding: 12px; border-radius: 12px;
              border: 1px solid rgba(255,255,255,0.08);
            }
            .metric-box span { display: block; font-size: 11px; color: #94A3B8; }
            .metric-box strong { font-size: 16px; color: #00E5FF; margin-top: 4px; display: block; }
            .metric-box strong.ok { color: #00E676; }
            .card { background: #131B2E; padding: 14px; border-radius: 14px; border: 1px solid #283552; }
            .form-row { display: flex; gap: 8px; margin-bottom: 10px; }
            input {
              flex: 1; background: #0B0F17; border: 1px solid #334155;
              color: #FFF; padding: 8px 12px; border-radius: 8px;
            }
            ul { list-style: none; padding: 0; margin: 0; }
            li {
              background: #0B0F17; padding: 9px 12px; border-radius: 8px;
              margin-bottom: 6px; border-right: 3px solid #7C4DFF; font-size: 13px;
            }
        """.trimIndent()

        val DEFAULT_JS = """
            // Local UI interactions only; this starter does not claim to sync with a cloud service.
            function addOperation() {
              const input = document.getElementById('op-title');
              const value = input?.value?.trim();
              if (!value) return;
              const item = document.createElement('li');
              item.textContent = value;
              document.getElementById('ops-list')?.prepend(item);
              input.value = '';
            }
        """.trimIndent()

        val DEFAULT_BACKEND = """
            // Example Kotlin service only; it is not wired to Firebase or a database.
            package com.novaforge.cloud

            data class ApiOperation(val id: String, val title: String, val status: String)

            class FullStackEngine {
                fun previewOperation(title: String): ApiOperation {
                    return ApiOperation(
                        id = "local_" + System.currentTimeMillis(),
                        title = title,
                        status = "LOCAL_ONLY_NOT_PERSISTED"
                    )
                }
            }
        """.trimIndent()
    }
}
