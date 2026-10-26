package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.remote.GeminiAiRepository
import com.example.data.repository.StudioRepository
import com.example.ui.screens.AiAndMediaStudioTabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BioscienceLabTabScreen
import com.example.ui.screens.DashboardTabScreen
import com.example.ui.screens.DatabaseAndVcsTabScreen
import com.example.ui.screens.IdePreviewTabScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudioViewModel
import com.example.util.AndroidTtsSpeaker
import com.example.util.AudioAndMediaHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
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
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        NovaForgeStudioShell(currentUserId = user.uid, userEmail = user.email ?: "Cloud Developer")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaForgeStudioShell(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsSpeaker = remember { AndroidTtsSpeaker(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsSpeaker.shutdown()
        }
    }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(
                    repository = StudioRepository(db),
                    aiRepository = GeminiAiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Mandatory BackHandler for secondary tabs when custom state switching is used
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val snapshotsState by viewModel.snapshotsState.collectAsStateWithLifecycle()
    val collectionsState by viewModel.collectionsState.collectAsStateWithLifecycle()
    val buildLogsState by viewModel.buildLogsState.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    val activeCodeTab by viewModel.activeCodeTab.collectAsStateWithLifecycle()
    val editorName by viewModel.editorName.collectAsStateWithLifecycle()
    val editorLanguage by viewModel.editorLanguage.collectAsStateWithLifecycle()
    val htmlBuffer by viewModel.htmlBuffer.collectAsStateWithLifecycle()
    val cssBuffer by viewModel.cssBuffer.collectAsStateWithLifecycle()
    val jsBuffer by viewModel.jsBuffer.collectAsStateWithLifecycle()
    val backendBuffer by viewModel.backendBuffer.collectAsStateWithLifecycle()

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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface,
                    titleContentColor = CyberCyan
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NovaForge AI Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            if (isBusy) {
                                Spacer(modifier = Modifier.width(10.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyberCyan,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOutUser(
                                context = context,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.sign_out),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_dashboard)) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_ide)) },
                    modifier = Modifier.testTag("nav_tab_ide")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_db_vcs)) },
                    modifier = Modifier.testTag("nav_tab_db_vcs")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_ai_media)) },
                    modifier = Modifier.testTag("nav_tab_ai_media")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Science, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_bioscience)) },
                    modifier = Modifier.testTag("nav_tab_bioscience")
                )
            }
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
                        TextButton(onClick = { viewModel.clearStatusBanner() }) {
                            Text("إغلاق", color = CyberCyan)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> DashboardTabScreen(
                        projectsState = projectsState,
                        snapshotsState = snapshotsState,
                        collectionsState = collectionsState,
                        buildLogsState = buildLogsState,
                        selectedProjectId = selectedProjectId,
                        onSelectProjectAndOpenIde = { project ->
                            viewModel.selectProject(project)
                            selectedTab = 1
                        },
                        onCreateNewProject = { name, desc, lang ->
                            viewModel.createNewProject(name, desc, lang)
                            selectedTab = 1
                        },
                        onDeleteProject = { id -> viewModel.deleteProject(id) },
                        onReadReportAloud = { reportText ->
                            ttsSpeaker.speak(reportText)
                            viewModel.synthesizeSpeechFromText(reportText, "Kore") { audioB64 ->
                                scope.launch {
                                    AudioAndMediaHelper.playAudioOrSynthesizedTrack(audioB64, isMusic = false)
                                }
                            }
                        }
                    )
                    1 -> IdePreviewTabScreen(
                        projectName = editorName,
                        projectLanguage = editorLanguage,
                        activeCodeTab = activeCodeTab,
                        htmlBuffer = htmlBuffer,
                        cssBuffer = cssBuffer,
                        jsBuffer = jsBuffer,
                        backendBuffer = backendBuffer,
                        combinedPreviewHtml = viewModel.buildCombinedLivePreviewHtml(),
                        onSelectCodeTab = { viewModel.selectCodeTab(it) },
                        onUpdateCodeBuffer = { viewModel.updateActiveBuffer(it) },
                        onTriggerInlineSuggestion = { viewModel.triggerInlineCodeSuggestion(it) },
                        onGenerateFullStackHighThinking = { viewModel.generateFullStackWithHighThinking(it) },
                        onSaveAndBuildPreview = { viewModel.saveAndBuildCurrentProject() }
                    )
                    2 -> DatabaseAndVcsTabScreen(
                        collectionsState = collectionsState,
                        snapshotsState = snapshotsState,
                        onCreateCollection = { name, fields, sample, idx ->
                            viewModel.createDatabaseCollection(name, fields, sample, idx)
                        },
                        onAppendRecord = { schema, json ->
                            viewModel.appendRecordToCollection(schema, json)
                        },
                        onGenerateBackendDao = { schema ->
                            viewModel.generateBackendCodeFromSchema(schema)
                            selectedTab = 1
                        },
                        onDeleteCollection = { id -> viewModel.deleteDatabaseCollection(id) },
                        onCommitSnapshot = { msg, tag -> viewModel.commitVersionSnapshot(msg, tag) },
                        onRestoreSnapshot = { snap ->
                            viewModel.restoreVersionSnapshot(snap)
                            selectedTab = 1
                        },
                        onComputeDiff = { snap -> viewModel.computeDiffAgainstCurrent(snap) }
                    )
                    3 -> AiAndMediaStudioTabScreen(
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
                            ttsSpeaker.speak(text)
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
                                ttsSpeaker.speak(reply)
                            }
                        },
                        onGenerateMusic = { prompt, isPro ->
                            viewModel.generateMusicTrack(prompt, isPro) { audioB64 ->
                                scope.launch {
                                    AudioAndMediaHelper.playAudioOrSynthesizedTrack(audioB64, isMusic = true)
                                }
                            }
                        },
                        onSpeakText = { text -> ttsSpeaker.speak(text) }
                    )
                    4 -> BioscienceLabTabScreen(
                        bioscienceOutput = bioscienceOutput,
                        onRunBioscienceAnalysis = { seq, goal ->
                            viewModel.runBioscienceAnalysis(seq, goal)
                        },
                        onExportPipelineToIde = { bioReport ->
                            viewModel.selectCodeTab("Backend")
                            viewModel.updateActiveBuffer(backendBuffer + "\n\n/*\n$bioReport\n*/")
                            selectedTab = 1
                        }
                    )
                }
            }
        }
    }
}
