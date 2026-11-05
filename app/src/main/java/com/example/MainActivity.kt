package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.UiState
import com.example.data.remote.GeminiAiRepository
import com.example.data.repository.StudioRepository
import com.example.data.skills.SkillsMemoryEngine
import com.example.ui.components.ErrorBoundary
import com.example.ui.components.GlobalExceptionHandler
import com.example.ui.components.MainScreenLayout
import com.example.ui.components.SKILLS_HUB_TAB_INDEX
import com.example.ui.components.StudioBottomTab
import androidx.credentials.CredentialManager
import com.example.ui.screens.AiAndMediaStudioTabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BioscienceLabTabScreen
import com.example.ui.screens.DashboardTabScreen
import com.example.ui.screens.DatabaseAndVcsTabScreen
import com.example.ui.screens.IdePreviewTabScreen
import com.example.ui.screens.SkillsMemoryHubTabScreen
import com.example.ui.screens.onGoogleSignInClicked
import com.example.ui.screens.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DataViewModel
import com.example.ui.viewmodel.StudioViewModel
import com.example.util.AndroidTtsSpeaker
import com.example.util.AudioAndMediaHelper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            if (!debugToken.isNullOrEmpty()) {
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                // Clear any stale cached App Check JWT token so DebugAppCheckProviderFactory mints a fresh token
                context.getSharedPreferences(
                    "com.google.firebase.appcheck.store.$persistenceKey",
                    Context.MODE_PRIVATE
                ).edit().clear().commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
        GlobalExceptionHandler.reportError(
            phase = "Firebase App Check Initialization",
            throwable = e,
            isFatal = false,
            serviceName = "Firebase"
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        GlobalExceptionHandler.install()
        GlobalExceptionHandler.verifyStartupInitialization(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ErrorBoundary(
                    onRetryInitialization = {
                        configureAppCheck(this@MainActivity, intent)
                        GlobalExceptionHandler.verifyStartupInitialization(this@MainActivity)
                    }
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth? = null) {
    val resolvedAuthResult = remember(auth) {
        runCatching { auth ?: Firebase.auth }
    }

    val resolvedAuth = resolvedAuthResult.getOrNull()
    val authInitException = resolvedAuthResult.exceptionOrNull()

    if (authInitException != null || resolvedAuth == null) {
        val throwable = authInitException
            ?: IllegalStateException("تعذر الوصول إلى خدمة المصادقة السحابية FirebaseAuth")
        LaunchedEffect(throwable) {
            GlobalExceptionHandler.reportError(
                phase = "FirebaseAuth Initialization",
                throwable = throwable,
                isFatal = false,
                serviceName = "Firebase"
            )
        }
        AuthScreen(onAuthSuccess = {})
        return
    }

    val authFlow = remember(resolvedAuth) { resolvedAuth.authStateFlow() }
    val flowUser by authFlow.collectAsStateWithLifecycle(initialValue = resolvedAuth.currentUser)
    var currentUser by remember(resolvedAuth) { mutableStateOf(resolvedAuth.currentUser) }

    DisposableEffect(resolvedAuth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        resolvedAuth.addAuthStateListener(listener)
        onDispose { resolvedAuth.removeAuthStateListener(listener) }
    }

    val user = currentUser ?: flowUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = resolvedAuth.currentUser })
    } else {
        NovaForgeStudioShell(
            currentUserId = user.uid,
            userEmail = user.email ?: "Cloud Developer",
            userDisplayName = user.displayName ?: "NovaForge Developer",
            userPhotoUrl = user.photoUrl?.toString().orEmpty()
        )
    }
}

@Composable
fun NovaForgeStudioShell(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String = "NovaForge Developer",
    userPhotoUrl: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val ttsSpeaker = remember {
        runCatching { AndroidTtsSpeaker(context) }
            .onFailure { e ->
                GlobalExceptionHandler.reportError(
                    phase = "TTS Audio Engine Initialization",
                    throwable = e,
                    isFatal = false,
                    serviceName = "System"
                )
            }
            .getOrNull()
    }

    DisposableEffect(ttsSpeaker) {
        onDispose {
            ttsSpeaker?.shutdown()
        }
    }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                val databaseId = runCatching {
                    app?.getString(R.string.firestore_database_id)
                        ?: context.getString(R.string.firestore_database_id)
                }.onFailure { e ->
                    GlobalExceptionHandler.reportError(
                        phase = "StudioViewModel Database Resource Init",
                        throwable = e,
                        isFatal = false,
                        serviceName = "Firebase"
                    )
                }.getOrDefault(context.getString(R.string.firestore_database_id))

                val db = runCatching {
                    FirebaseFirestore.getInstance(databaseId)
                }.getOrElse { e ->
                    GlobalExceptionHandler.reportError(
                        phase = "StudioViewModel Firestore Init",
                        throwable = e,
                        isFatal = false,
                        serviceName = "Firebase"
                    )
                    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                }

                val aiRepo = runCatching {
                    GeminiAiRepository()
                }.onFailure { e ->
                    GlobalExceptionHandler.reportError(
                        phase = "StudioViewModel Gemini Init",
                        throwable = e,
                        isFatal = false,
                        serviceName = "Gemini"
                    )
                }.getOrElse { GeminiAiRepository() }

                StudioViewModel(
                    repository = StudioRepository(db),
                    aiRepository = aiRepo,
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl
                )
            }
        }
    )

    val dataViewModel: DataViewModel = viewModel(
        key = "data_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                val databaseId = runCatching {
                    app?.getString(R.string.firestore_database_id)
                        ?: context.getString(R.string.firestore_database_id)
                }.onFailure { e ->
                    GlobalExceptionHandler.reportError(
                        phase = "DataViewModel Database Resource Init",
                        throwable = e,
                        isFatal = false,
                        serviceName = "Firebase"
                    )
                }.getOrDefault(context.getString(R.string.firestore_database_id))

                val db = runCatching {
                    FirebaseFirestore.getInstance(databaseId)
                }.getOrElse { e ->
                    GlobalExceptionHandler.reportError(
                        phase = "DataViewModel Firestore Init",
                        throwable = e,
                        isFatal = false,
                        serviceName = "Firebase"
                    )
                    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                }

                DataViewModel(
                    repository = StudioRepository(db),
                    databaseId = databaseId,
                    currentUserId = currentUserId
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(StudioBottomTab.DASHBOARD.index) }

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val bioExperimentsState by viewModel.bioExperimentsState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val syncLatencyMs by viewModel.syncLatencyMs.collectAsStateWithLifecycle()
    val lastSyncTimestampLabel by viewModel.lastSyncTimestampLabel.collectAsStateWithLifecycle()
    val snapshotsState by viewModel.snapshotsState.collectAsStateWithLifecycle()
    val collectionsState by viewModel.collectionsState.collectAsStateWithLifecycle()
    val buildLogsState by viewModel.buildLogsState.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    LaunchedEffect(projectsState) {
        val state = projectsState
        if (state is UiState.Error) {
            GlobalExceptionHandler.reportError(
                phase = "Firestore Projects Stream",
                throwable = IllegalStateException(state.message),
                isFatal = false
            )
        }
    }

    val activeCodeTab by viewModel.activeCodeTab.collectAsStateWithLifecycle()
    val editorName by viewModel.editorName.collectAsStateWithLifecycle()
    val editorLanguage by viewModel.editorLanguage.collectAsStateWithLifecycle()
    val htmlBuffer by viewModel.htmlBuffer.collectAsStateWithLifecycle()
    val cssBuffer by viewModel.cssBuffer.collectAsStateWithLifecycle()
    val jsBuffer by viewModel.jsBuffer.collectAsStateWithLifecycle()
    val backendBuffer by viewModel.backendBuffer.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val activeBranch by viewModel.activeBranch.collectAsStateWithLifecycle()

    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedChatModel by viewModel.selectedChatModel.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val useSearchGrounding by viewModel.useSearchGrounding.collectAsStateWithLifecycle()
    val useHighThinking by viewModel.useHighThinking.collectAsStateWithLifecycle()

    val generatedAssets by viewModel.generatedAssets.collectAsStateWithLifecycle()
    val mediaAnalysisOutput by viewModel.mediaAnalysisOutput.collectAsStateWithLifecycle()
    val liveVoiceHistory by viewModel.liveVoiceHistory.collectAsStateWithLifecycle()
    val lastTranscribedText by viewModel.lastTranscribedText.collectAsStateWithLifecycle()
    val bioscienceOutput by viewModel.bioscienceOutput.collectAsStateWithLifecycle()

    val skillsCatalog by viewModel.skillsCatalog.collectAsStateWithLifecycle()
    val pinnedSkillIds by viewModel.pinnedSkillIds.collectAsStateWithLifecycle()
    val autoInjectSkillsEnabled by viewModel.autoInjectSkillsEnabled.collectAsStateWithLifecycle()
    val lastAppliedSkillIds by viewModel.lastAppliedSkillIds.collectAsStateWithLifecycle()
    val isSkillsSyncedToFirestore by viewModel.isSkillsSyncedToFirestore.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val loadedCatalog = SkillsMemoryEngine.loadCatalog(context)
        viewModel.initializeSkillsMemory(loadedCatalog)
    }

    val resolvedProfile = (userProfileState as? UiState.Success)?.data
    val syncedProjectsCount = (projectsState as? UiState.Success)?.data?.size
        ?: resolvedProfile?.projectsCount?.toInt()
        ?: 0
    val syncedBioExperimentsCount = (bioExperimentsState as? UiState.Success)?.data?.size
        ?: resolvedProfile?.bioExperimentsCount?.toInt()
        ?: 0

    MainScreenLayout(
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        userEmail = resolvedProfile?.email?.ifBlank { userEmail } ?: userEmail,
        activeBranch = activeBranch,
        skillsCount = skillsCatalog.totalSkills,
        pinnedSkillsCount = pinnedSkillIds.size,
        isBusy = isBusy,
        statusBanner = statusBanner,
        onDismissStatusBanner = { viewModel.clearStatusBanner() },
        onSignOut = {
            signOutUser(
                context = context,
                onSignOutComplete = {},
                scope = scope
            )
        },
        userDisplayName = resolvedProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName,
        userId = currentUserId,
        syncedProjectsCount = syncedProjectsCount,
        syncedBioExperimentsCount = syncedBioExperimentsCount,
        syncLatencyMs = syncLatencyMs,
        lastSyncTimestampLabel = lastSyncTimestampLabel,
        selectedAiModel = selectedChatModel,
        onSelectAiModel = { viewModel.setChatModel(it) },
        onToggleAiProvider = { viewModel.toggleAiProvider(it) },
        onForceCloudSync = { viewModel.forceManualCloudSync() },
        onSwitchGoogleAccount = {
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { viewModel.forceManualCloudSync() },
                onAuthError = { err ->
                    GlobalExceptionHandler.reportError(
                        phase = "Switch Google Account",
                        throwable = IllegalStateException(err),
                        isFatal = false,
                        serviceName = "Firebase"
                    )
                },
                scope = scope
            )
        },
        onSubmitPrompt = { promptText ->
            viewModel.sendChatMessage(promptText)
        }
    ) { currentTab ->
        when (currentTab) {
            StudioBottomTab.DASHBOARD.index -> DashboardTabScreen(
                projectsState = projectsState,
                snapshotsState = snapshotsState,
                collectionsState = collectionsState,
                buildLogsState = buildLogsState,
                selectedProjectId = selectedProjectId,
                activeBranch = activeBranch,
                onSelectProjectAndOpenIde = { project ->
                    viewModel.selectProject(project)
                    selectedTab = StudioBottomTab.IDE.index
                },
                onCreateNewProject = { name, desc, lang ->
                    viewModel.createNewProject(name, desc, lang)
                    selectedTab = StudioBottomTab.IDE.index
                },
                onDeleteProject = { id -> viewModel.deleteProject(id) },
                onReadReportAloud = { reportText ->
                    ttsSpeaker?.speak(reportText)
                    viewModel.synthesizeSpeechFromText(reportText, "Kore") { audioB64 ->
                        scope.launch {
                            AudioAndMediaHelper.playAudioOrSynthesizedTrack(audioB64, isMusic = false)
                        }
                    }
                },
                onTriggerCloudDeploy = { viewModel.triggerCloudDeploymentPipeline() },
                onRunSecurityAudit = { viewModel.runAiSecurityAndPerformanceAudit() }
            )
            StudioBottomTab.IDE.index -> IdePreviewTabScreen(
                projectName = editorName,
                projectLanguage = editorLanguage,
                activeCodeTab = activeCodeTab,
                htmlBuffer = htmlBuffer,
                cssBuffer = cssBuffer,
                jsBuffer = jsBuffer,
                backendBuffer = backendBuffer,
                combinedPreviewHtml = viewModel.buildCombinedLivePreviewHtml(),
                consoleLogs = consoleLogs,
                onSelectCodeTab = { viewModel.selectCodeTab(it) },
                onUpdateCodeBuffer = { viewModel.updateActiveBuffer(it) },
                onTriggerInlineSuggestion = { viewModel.triggerInlineCodeSuggestion(it) },
                onGenerateFullStackHighThinking = { viewModel.generateFullStackWithHighThinking(it) },
                onSaveAndBuildPreview = { viewModel.saveAndBuildCurrentProject() },
                onLoadTemplate = { viewModel.loadStarterTemplate(it) },
                onRunSecurityAudit = { viewModel.runAiSecurityAndPerformanceAudit() },
                onAppendConsoleLog = { viewModel.appendConsoleLog(it) },
                onClearConsoleLogs = { viewModel.clearConsoleLogs() }
            )
            StudioBottomTab.DB_VCS.index -> DatabaseAndVcsTabScreen(
                dataViewModel = dataViewModel,
                collectionsState = collectionsState,
                snapshotsState = snapshotsState,
                activeBranch = activeBranch,
                onSelectBranch = { viewModel.setActiveBranch(it) },
                onCreateCollection = { name, fields, sample, idx ->
                    viewModel.createDatabaseCollection(name, fields, sample, idx)
                },
                onAppendRecord = { schema, json ->
                    viewModel.appendRecordToCollection(schema, json)
                },
                onGenerateBackendDao = { schema ->
                    viewModel.generateBackendCodeFromSchema(schema)
                    selectedTab = StudioBottomTab.IDE.index
                },
                onDeleteCollection = { id -> viewModel.deleteDatabaseCollection(id) },
                onCommitSnapshot = { msg, tag -> viewModel.commitVersionSnapshot(msg, tag) },
                onRestoreSnapshot = { snap ->
                    viewModel.restoreVersionSnapshot(snap)
                    selectedTab = StudioBottomTab.IDE.index
                },
                onComputeDiff = { snap -> viewModel.computeDiffAgainstCurrent(snap) }
            )
            StudioBottomTab.AI_MEDIA.index -> AiAndMediaStudioTabScreen(
                chatMessages = chatMessages,
                selectedChatModel = selectedChatModel,
                selectedPersona = selectedPersona,
                useSearchGrounding = useSearchGrounding,
                useHighThinking = useHighThinking,
                generatedAssets = generatedAssets,
                mediaAnalysisOutput = mediaAnalysisOutput,
                liveVoiceHistory = liveVoiceHistory,
                lastTranscribedText = lastTranscribedText,
                onSelectChatModel = { viewModel.setChatModel(it) },
                onSelectPersona = { viewModel.setChatPersona(it) },
                onToggleSearch = { viewModel.toggleSearchGrounding(it) },
                onToggleHighThinking = { viewModel.toggleHighThinking(it) },
                onSendChatMessage = { viewModel.sendChatMessage(it) },
                onGenerateImage = { prompt, pro, ratio, size, b64, mime ->
                    viewModel.generateImageAsset(prompt, pro, ratio, size, b64, mime)
                },
                onGenerateVideo = { prompt, ratio, b64, mime ->
                    viewModel.generateOrAnimateVideoAsset(prompt, ratio, b64, mime)
                },
                onAnalyzeMedia = { prompt, b64, mime, isVideo ->
                    viewModel.analyzeUploadedMedia(prompt, b64, mime, isVideo)
                },
                onSynthesizeSpeech = { text, voice ->
                    ttsSpeaker?.speak(text)
                    viewModel.synthesizeSpeechFromText(text, voice) { audioB64 ->
                        scope.launch {
                            AudioAndMediaHelper.playAudioOrSynthesizedTrack(audioB64, isMusic = false)
                        }
                    }
                },
                onTranscribeAudio = { b64, cb ->
                    viewModel.transcribeRecordedAudio(b64, cb)
                },
                onSendLiveVoiceTurn = { text, b64 ->
                    viewModel.sendLiveVoiceTurn(text, b64) { reply ->
                        ttsSpeaker?.speak(reply)
                    }
                },
                onGenerateMusic = { prompt, isPro ->
                    viewModel.generateMusicTrack(prompt, isPro) { audioB64 ->
                        scope.launch {
                            AudioAndMediaHelper.playAudioOrSynthesizedTrack(audioB64, isMusic = true)
                        }
                    }
                },
                onSpeakText = { text -> ttsSpeaker?.speak(text) }
            )
            StudioBottomTab.BIOSCIENCE.index -> BioscienceLabTabScreen(
                bioscienceOutput = bioscienceOutput,
                onRunBioscienceAnalysis = { seq, goal ->
                    viewModel.runBioscienceAnalysis(seq, goal)
                },
                onExportPipelineToIde = { bioReport ->
                    viewModel.selectCodeTab("Backend")
                    viewModel.updateActiveBuffer(backendBuffer + "\n\n/*\n$bioReport\n*/")
                    selectedTab = StudioBottomTab.IDE.index
                },
                onLoad3dBioGameTemplateInIde = {
                    viewModel.loadStarterTemplate("THREEJS_BIOGAME")
                    selectedTab = StudioBottomTab.IDE.index
                },
                bioExperimentsState = bioExperimentsState,
                onSaveBioExperiment = { title, workflowMode, seq, goal, plddt, freeEnergy, alphaPct, betaPct, summary ->
                    viewModel.saveBioExperimentToCloud(
                        title = title,
                        workflowMode = workflowMode,
                        fastaSequence = seq,
                        researchGoal = goal,
                        plddtScore = plddt,
                        freeEnergyKcal = freeEnergy,
                        alphaHelixPct = alphaPct,
                        betaSheetPct = betaPct,
                        aiReportSummary = summary
                    )
                },
                onDeleteBioExperiment = { expId ->
                    viewModel.deleteBioExperimentFromCloud(expId)
                }
            )
            SKILLS_HUB_TAB_INDEX -> SkillsMemoryHubTabScreen(
                catalog = skillsCatalog,
                pinnedSkillIds = pinnedSkillIds,
                autoInjectSkillsEnabled = autoInjectSkillsEnabled,
                lastAppliedSkillIds = lastAppliedSkillIds,
                isSyncedWithFirestore = isSkillsSyncedToFirestore,
                onToggleAutoInjectSkills = { viewModel.toggleAutoInjectSkills(it) },
                onTogglePinSkill = { viewModel.togglePinSkillInMemory(it) },
                onSyncSkillsToFirestore = { viewModel.syncSkillsMemoryToFirestore(silent = false) },
                onInjectCodeIntoIde = { code, tab, label ->
                    viewModel.injectSkillCodeIntoEditor(code, tab, label)
                    selectedTab = StudioBottomTab.IDE.index
                },
                onApplySkillInAiChat = { skill ->
                    viewModel.applySkillInAiChat(skill)
                    selectedTab = StudioBottomTab.AI_MEDIA.index
                }
            )
        }
    }
}
