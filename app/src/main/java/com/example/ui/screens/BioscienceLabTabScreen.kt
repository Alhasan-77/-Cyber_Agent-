package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BioscienceLabTabScreen(
    bioscienceOutput: String?,
    onRunBioscienceAnalysis: (String, String) -> Unit,
    onExportPipelineToIde: (String) -> Unit
) {
    val presets = listOf(
        "Human Insulin (INS DNA)" to ">NM_000207.3 Human Insulin\nATGGCCCTGTGGATGCGCCTCCTGCCCCTGCTGGCGCTGCTGGCCCTCTGGGGACCTGACCCAGCCGCAGCCTTTGTGAACCAACACCTGTGCGGCTCACACCTGGTGGAAGCTCTCTACCTAGTGTGCGGGGAACGAGGCTTCTTCTACACACCCAAGACCCGCCGGGAGGCAGAGGACCTGCAGGTGGGGCAGGTGGAGCTGGGCGGGGGCCCTGGTGCAGGCAGCCTGCAGCCCTTGGCCCTGGAGGGGTCCCTGCAGAAGCGTGGCATTGTGGAACAATGCTGTACCAGCATCTGCTCCCTCTACCAGCTGGAGAACTACTGCAACTAG",
        "CRISPR-Cas9 Guide RNA" to ">CRISPR_sgRNA_Target\nGTTTTAGAGCTAGAAATAGCAAGTTAAAATAAGGCTAGTCCGTTATCAACTTGAAAAAGTGGCACCGAGTCGGTGCTTTTTT",
        "GFP Protein Sequence" to ">GFP_Aequorea_victoria\nMSKGEELFTGVVPILVELDGDVNGHKFSVSGEGEGDATYGKLTLKFICTTGKLPVPWPTLVTTFSYGVQCFSRYPDHMKQHDFFKSAMPEGYVQERTIFFKDDGNYKTRAEVKFEGDTLVNRIELKGIDFKEDGNILGHKLEYNYNSHNVYIMADKQKNGIKVNFKIRHNIEDGSVQLADHYQQNTPIGDGPVLLPDNHYLSTQSALSKDPNEKRDHMVLLEFVTAAGITHGMDELYK"
    )

    var fastaSequence by remember { mutableStateOf(presets.first().second) }
    var researchGoal by remember {
        mutableStateOf("تحليل نسبة GC، حساب الوزن الجزيئي، وتوليد خط أنابيب Biopython للترجمة البروتينية")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        text = "🧬 مختبر مهارات العلوم الحيوية والمعلوماتية (Bioscience & Genomic Skills)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = "تحليل تسلسلات الحمض النووي والبروتينات (FASTA) وتوليد خوارزميات Biopython عبر gemini-4-argon (ThinkingLevel.HIGH)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (title, seq) ->
                            FilterChip(
                                selected = fastaSequence == seq,
                                onClick = { fastaSequence = seq },
                                label = { Text(title) }
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

                    Button(
                        onClick = { onRunBioscienceAnalysis(fastaSequence, researchGoal) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_bioscience_analysis_button")
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تشغيل التحليل الحيوي وتوليد الكود العلمي",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                            horizontalArrangement = Arrangement.SpaceBetween
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
    }
}
