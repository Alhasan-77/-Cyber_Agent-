package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.BioExperimentRecord
import com.example.data.model.UiState
import com.example.data.skills.BioSequenceMetrics
import com.example.data.skills.BioscienceSimulationEngine
import com.example.data.skills.BioscienceWorkflowMode
import com.example.ui.components.ProteinFoldingVisualizer
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCoral
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BioscienceLabTabScreen(
    bioscienceOutput: String?,
    onRunBioscienceAnalysis: (String, String) -> Unit,
    onExportPipelineToIde: (String) -> Unit,
    onLoad3dBioGameTemplateInIde: (() -> Unit)? = null,
    bioExperimentsState: UiState<List<BioExperimentRecord>> = UiState.Success(emptyList()),
    onSaveBioExperiment: (
        title: String,
        toolType: String,
        sequenceInput: String,
        targetParameters: String,
        plddtScore: Int,
        rmsdScore: Double,
        gcContentPercent: Int,
        crisprGuidesCount: Int,
        summaryReport: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteBioExperiment: (String) -> Unit = {}
) {
    var selectedSectionFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, GAME_3D, BIO_TOOLS
    var selected3dViewMode by rememberSaveable { mutableStateOf("GAME") } // GAME, INSPECTOR
    var selectedMoleculeId by rememberSaveable { mutableStateOf("DNA_HELIX") }
    var webViewReloadTick by rememberSaveable { mutableIntStateOf(0) }

    var activeWorkflow by rememberSaveable { mutableStateOf(BioscienceWorkflowMode.PROTEIN_FOLDING) }
    var fastaSequence by rememberSaveable {
        mutableStateOf(BioscienceWorkflowMode.PROTEIN_FOLDING.defaultSequence)
    }
    var researchGoal by rememberSaveable {
        mutableStateOf(BioscienceWorkflowMode.PROTEIN_FOLDING.defaultGoal)
    }

    val classicPresets = remember {
        listOf(
            "Human Hemoglobin (HBB)" to BioscienceWorkflowMode.PROTEIN_FOLDING.defaultSequence,
            "CRISPR Exon1 Target" to BioscienceWorkflowMode.CRISPR_EDITING.defaultSequence,
            "BRCA1 Amplicon (DNA)" to BioscienceWorkflowMode.GENOMIC_ALIGNMENT.defaultSequence,
            "GLP-1R Agonist (Clinical)" to BioscienceWorkflowMode.CLINICAL_TRIALS.defaultSequence,
            "GFP Fluorescent Protein" to ">GFP_Aequorea_victoria\nMSKGEELFTGVVPILVELDGDVNGHKFSVSGEGEGDATYGKLTLKFICTTGKLPVPWPTLVTTFSYGVQCFSRYPDHMKQHDFFKSAMPEGYVQERTIFFKDDGNYKTRAEVKFEGDTLVNRIELKGIDFKEDGNILGHKLEYNYNSHNVYIMADKQKNGIKVNFKIRHNIEDGSVQLADHYQQNTPIGDGPVLLPDNHYLSTQSALSKDPNEKRDHMVLLEFVTAAGITHGMDELYK"
        )
    }

    val liveMetrics: BioSequenceMetrics = remember(fastaSequence, activeWorkflow) {
        BioscienceSimulationEngine.analyzeSequenceMetrics(fastaSequence, activeWorkflow)
    }

    val interactive3dHtml = remember(selected3dViewMode, selectedMoleculeId, webViewReloadTick) {
        BioscienceSimulationEngine.buildInteractive3dBioGameHtml(
            initialMode = selected3dViewMode,
            initialMolecule = selectedMoleculeId
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("bioscience_lab_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Section Switcher & Overview Banner
        item {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🧬 مختبر Gemini الحيوي ومحرك الألعاب 3D",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    brush = com.example.ui.theme.GeminiSparkleGradient
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "دمج تفاعلي بين لعبة الدفاع الخلوي ثلاثية الأبعاد، مستعرض الهياكل الجزيئية، وأدوات طي البروتين وCRISPR والتجارب السريرية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSectionFilter == "ALL",
                            onClick = { selectedSectionFilter = "ALL" },
                            label = { Text("⚡ عرض متكامل (3D + المختبر)") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("bio_section_all")
                        )
                        FilterChip(
                            selected = selectedSectionFilter == "GAME_3D",
                            onClick = { selectedSectionFilter = "GAME_3D" },
                            label = { Text("🎮 لعبة الدفاع الخلوي ومستعرض 3D") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("bio_section_game3d")
                        )
                        FilterChip(
                            selected = selectedSectionFilter == "BIO_TOOLS",
                            onClick = { selectedSectionFilter = "BIO_TOOLS" },
                            label = { Text("🔬 المهارات الحيوية الأربعة") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("bio_section_tools")
                        )
                    }
                }
            }
        }

        // 2. Interactive 3D Cellular Defense Game & 3D Molecular Scene Viewer
        if (selectedSectionFilter == "ALL" || selectedSectionFilter == "GAME_3D") {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, com.example.ui.theme.CyberBorder, RoundedCornerShape(24.dp))
                        .testTag("bio_3d_simulation_card")
                ) {
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
                                    text = "🎮 لعبة الدفاع الخلوي ومستعرض المشاهد 3D (Three.js / WebGL)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تحكم بعصا التوجيه اللمسية وأزرار مهارات الأجسام المضادة ونبضات CRISPR مع مؤثرات صوتية WebAudio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { webViewReloadTick++ },
                                modifier = Modifier.testTag("reload_3d_simulation_button")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "إعادة تشغيل المشهد ثلاثي الأبعاد",
                                    tint = CyberCyan
                                )
                            }
                        }

                        // Mode Toggle: Cellular Defense Game vs 3D Molecular Inspector
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selected3dViewMode == "GAME",
                                onClick = { selected3dViewMode = "GAME" },
                                leadingIcon = {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = CyberEmerald)
                                },
                                label = { Text("وضع لعبة الدفاع الخلوي 3D") },
                                modifier = Modifier.testTag("bio3d_mode_game")
                            )
                            FilterChip(
                                selected = selected3dViewMode == "INSPECTOR",
                                onClick = { selected3dViewMode = "INSPECTOR" },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = CyberCyan)
                                },
                                label = { Text("مستعرض المجسمات الجزيئية 3D") },
                                modifier = Modifier.testTag("bio3d_mode_inspector")
                            )
                        }

                        // 3D Model Selector Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BioscienceSimulationEngine.scenePresets.forEach { preset ->
                                FilterChip(
                                    selected = selectedMoleculeId == preset.id,
                                    onClick = {
                                        selectedMoleculeId = preset.id
                                        selected3dViewMode = "INSPECTOR"
                                    },
                                    label = { Text("${preset.titleAr} (${preset.partsCount} أجزاء)") },
                                    modifier = Modifier.testTag("bio3d_preset_${preset.id}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (onLoad3dBioGameTemplateInIde != null) {
                                        onLoad3dBioGameTemplateInIde()
                                    } else {
                                        onExportPipelineToIde(BioscienceSimulationEngine.THREEJS_GAME_JS)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_3d_biogame_to_ide_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فتح مشروع 3D واللعبة في محرر IDE", color = CyberCyan)
                            }
                        }

                        // Interactive 3D Canvas Viewport
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .background(CyberBackground)
                                .testTag("bioscience_3d_webview_container")
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()
                                        loadDataWithBaseURL(
                                            "https://novaforge.local/",
                                            interactive3dHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                },
                                update = { webView ->
                                    val currentTag = "${selected3dViewMode}_${selectedMoleculeId}_$webViewReloadTick"
                                    if (webView.tag != currentTag) {
                                        webView.tag = currentTag
                                        webView.loadDataWithBaseURL(
                                            "https://novaforge.local/",
                                            interactive3dHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. The Four Interactive Bioscience Workflows & Sequence Input
        if (selectedSectionFilter == "ALL" || selectedSectionFilter == "BIO_TOOLS") {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberViolet.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🧬 أدوات ومهارات العلوم الحيوية التفاعلية (4 مسارات تخصصية)",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeWorkflow.subtitleAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Workflow Mode Chips (Protein Folding, CRISPR, Genomics, Clinical Trials)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BioscienceWorkflowMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = activeWorkflow == mode,
                                    onClick = {
                                        activeWorkflow = mode
                                        fastaSequence = mode.defaultSequence
                                        researchGoal = mode.defaultGoal
                                        selectedMoleculeId = when (mode) {
                                            BioscienceWorkflowMode.PROTEIN_FOLDING -> "PROTEIN_COMPLEX"
                                            BioscienceWorkflowMode.CRISPR_EDITING -> "CRISPR_CAS9"
                                            BioscienceWorkflowMode.GENOMIC_ALIGNMENT -> "DNA_HELIX"
                                            BioscienceWorkflowMode.CLINICAL_TRIALS -> "VIRAL_NANOBOT"
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberViolet.copy(alpha = 0.25f),
                                        selectedLabelColor = CyberCyan
                                    ),
                                    label = { Text(mode.titleAr) },
                                    modifier = Modifier.testTag("bio_workflow_${mode.id}")
                                )
                            }
                        }

                        HorizontalDivider(color = CyberViolet.copy(alpha = 0.2f))

                        Text(
                            text = "عينات جينية وبروتينية جاهزة للفحص الفوري:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberEmerald
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            classicPresets.forEachIndexed { idx, (title, seq) ->
                                FilterChip(
                                    selected = fastaSequence == seq,
                                    onClick = { fastaSequence = seq },
                                    label = { Text(title) },
                                    modifier = Modifier.testTag("bio_preset_chip_$idx")
                                )
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = fastaSequence,
                                onValueChange = { fastaSequence = it },
                                label = { Text("FASTA / DNA / RNA / Protein Sequence") },
                                minLines = 4,
                                maxLines = 7,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bioscience_sequence_input")
                            )
                        }

                        OutlinedTextField(
                            value = researchGoal,
                            onValueChange = { researchGoal = it },
                            label = { Text("الهدف البحثي أو الحسابي المطلوب") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bioscience_goal_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onRunBioscienceAnalysis(
                                        fastaSequence,
                                        "[${activeWorkflow.badgeText}] $researchGoal"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("run_bioscience_analysis_button")
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = CyberBackground)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تشغيل التحليل الحيوي (AI)",
                                    color = CyberBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val exportedCode = BioscienceSimulationEngine.generateExportablePipelineCode(
                                        workflowMode = activeWorkflow,
                                        metrics = liveMetrics
                                    )
                                    onExportPipelineToIde(exportedCode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_bio_pipeline_code_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = CyberEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير الكود للمحرر", color = CyberEmerald)
                            }
                        }

                        Button(
                            onClick = {
                                val summaryText = bioscienceOutput
                                    ?: "مؤشرات وصفية أولية (${activeWorkflow.badgeText}): نوع التسلسل=${liveMetrics.sequenceType}، الطول=${liveMetrics.sequenceLength}، GC=${liveMetrics.gcContentPct}%. لا توجد نتائج pLDDT أو ΔG أو IC50 أو جاهزية سريرية من نموذج تخصصي في هذه الجلسة."
                                onSaveBioExperiment(
                                    "${activeWorkflow.titleAr} (${liveMetrics.sequenceType})",
                                    activeWorkflow.name,
                                    fastaSequence,
                                    researchGoal,
                                    liveMetrics.plddtConfidence.toInt(),
                                    liveMetrics.bindingAffinityKcal,
                                    liveMetrics.alphaHelixPct,
                                    liveMetrics.betaSheetPct,
                                    summaryText
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_bio_experiment_cloud_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.save_bio_experiment_cloud),
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3b. Interactive Protein Folding Structural Visualizer
            item {
                ProteinFoldingVisualizer(metrics = liveMetrics)
            }

            // 4. Real-Time Computed Bioinformatics & Clinical Dashboard Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .testTag("bioscience_realtime_metrics_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 المؤشرات الحيوية والحسابية الفورية (${liveMetrics.sequenceType})",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeWorkflow.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmerald,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }

                        // Primary KPI Grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BioMetricBadge("الطول", "${liveMetrics.sequenceLength} ${if (liveMetrics.sequenceType == "PROTEIN") "aa" else "bp"}", CyberCyan)
                            BioMetricBadge("نسبة GC%", "${liveMetrics.gcContentPct}%", CyberEmerald)
                            BioMetricBadge("حرارة الانصهار Tm", "${liveMetrics.meltingTempC} °C", CyberAmber)
                            BioMetricBadge("الوزن الجزيئي", "${liveMetrics.molecularWeightKda} kDa", CyberViolet)
                            BioMetricBadge("مؤشر GRAVY", "${liveMetrics.gravyHydropathy}", CyberCyan)
                            BioMetricBadge(
                                "ثقة الطي pLDDT",
                                if (liveMetrics.plddtConfidence > 0) "${liveMetrics.plddtConfidence}%"
                                else "غير محسوبة",
                                CyberTextSecondary
                            )
                        }

                        // Section A: 3D Protein Folding Secondary Structure Distribution
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "مؤشرات تركيبية تقريبية (ليست تنبؤاً بنيوياً متخصصاً)",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            StructureBarRow("Alpha-Helix (حلزون ألفا)", liveMetrics.alphaHelixPct, CyberCyan)
                            StructureBarRow("Beta-Sheet (صفائح بيتا)", liveMetrics.betaSheetPct, CyberViolet)
                            StructureBarRow("Random Coil (حلقات مرنة)", liveMetrics.randomCoilPct, CyberAmber)
                        }

                        // Section B: CRISPR-Cas9 PAM (NGG) Target Sites
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✂️ مسح مواقع CRISPR-Cas9 PAM (NGG) وتقييم كفاءة الدليل sgRNA",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberViolet,
                                fontWeight = FontWeight.Bold
                            )
                            liveMetrics.pamSites.take(3).forEach { pam ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberSurface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pos ${pam.indexOneBased}: 5'-${pam.spacer20nt}-[${pam.pamTriplet}]-3'",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "GC: ${pam.gcPercent}% • المخاطر: ${pam.offTargetRisk}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Score ${pam.onTargetScore}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Section C: Clinical Trials PK/PD & ORF Translation Preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🏥 مؤشرات التجارب السريرية (IC50 / ΔG) وترجمة الإطار القرائي (ORF)",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "IC50: ${liveMetrics.predictedIc50Nm} nM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberCyan,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                                Text(
                                    text = "ΔG: ${liveMetrics.bindingAffinityKcal} kcal/mol",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberEmerald,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                                Text(
                                    text = "TI: ${liveMetrics.therapeuticIndex}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberAmber,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                            Text(
                                text = "الحالة السريرية: ${liveMetrics.clinicalPhaseReadiness}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ORF Protein: ${liveMetrics.translatedProteinPreview.take(64)}...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 5. AI Deep Bioscience Report Card
        if (bioscienceOutput != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔬 التقرير البيولوجي الحاسوبي وخط أنابيب الكود",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberEmerald
                            )
                            OutlinedButton(
                                onClick = { onExportPipelineToIde(bioscienceOutput) },
                                modifier = Modifier.testTag("export_bio_to_ide_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إرسال للمحرر", color = CyberCyan)
                            }
                        }

                        Text(
                            text = bioscienceOutput,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBackground)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        // 6. Cloud Firestore Synced Bio Experiments History & Restore Card
        item {
            val cloudExperiments = (bioExperimentsState as? UiState.Success)?.data.orEmpty()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberEmerald.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .testTag("cloud_bio_experiments_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CyberEmerald
                            )
                            Column {
                                Text(
                                    text = stringResource(id = R.string.cloud_bio_experiments_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyberEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تتم المزامنة الفورية لتجارب البروتين والجينوم مع حساب Google الخاص بك (${cloudExperiments.size} تجربة)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (cloudExperiments.isEmpty()) {
                        Text(
                            text = "لا توجد تجارب حيوية محفوظة سحابياً بعد. اضغط على «حفظ التجربة في السحابة» لحفظ التسلسل والمؤشرات الحالية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        cloudExperiments.take(6).forEach { exp ->
                            Surface(
                                color = CyberBackground,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .testTag("cloud_bio_exp_item_${exp.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exp.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = CyberCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${exp.workflowMode} • ملخص محفوظ: ${exp.aiReportSummary.take(90)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                color = CyberEmerald
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    fastaSequence = exp.fastaSequence
                                                    researchGoal = exp.researchGoal
                                                    BioscienceWorkflowMode.entries.find {
                                                        it.name.equals(exp.workflowMode, ignoreCase = true)
                                                    }?.let { activeWorkflow = it }
                                                },
                                                modifier = Modifier.testTag("restore_bio_exp_${exp.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = stringResource(id = R.string.restore_bio_experiment),
                                                    tint = CyberCyan
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(id = R.string.restore_bio_experiment),
                                                    color = CyberCyan,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteBioExperiment(exp.id) },
                                                modifier = Modifier.testTag("delete_bio_exp_${exp.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف التجربة",
                                                    tint = CyberCoral
                                                )
                                            }
                                        }
                                    }
                                    if (exp.aiReportSummary.isNotBlank()) {
                                        Text(
                                            text = exp.aiReportSummary.take(180),
                                            style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun BioMetricBadge(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBackground)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun StructureBarRow(
    label: String,
    percent: Int,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = barColor,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = CyberSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
        )
    }
}
