package com.example.data.skills

import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BioscienceWorkflowMode(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val badgeText: String,
    val defaultSequence: String,
    val defaultGoal: String
) {
    PROTEIN_FOLDING(
        id = "PROTEIN_FOLDING",
        titleAr = "تحليل تسلسل البروتين",
        subtitleAr = "حساب مؤشرات وصفية للتسلسل مع عارض هندسي توضيحي؛ لا يتوفر تنبؤ بنيوي متخصص أو pLDDT.",
        badgeText = "Protein Sequence • Visual Demo",
        defaultSequence = ">Human_Hemoglobin_Subunit_Beta (HBB) | Deep Folding Target\nMVHLTPEEKSAVTALWGKVNVDEVGGEALGRLLVVYPWTQRFFESFGDLSTPDAVMGNPKVKAHGKKVLGAFSDGLAHLDNLKGTFATLSELHCDKLHVDPENFRLLGNVLVCVLAHHFGKEFTPPVQAAYQKVVAGVANALAHKYH",
        defaultGoal = "تحليل طول تسلسل البروتين والوزن الجزيئي التقريبي ومؤشر GRAVY؛ لا يتوفر هنا تنبؤ بنيوي أو كشف مثبت لجيوب الارتباط."
    ),
    CRISPR_EDITING(
        id = "CRISPR_EDITING",
        titleAr = "محاكي تعديل الجينات CRISPR-Cas9",
        subtitleAr = "اكتشاف مواقع PAM (NGG) الأمامية واستخراج مقطع الدليل؛ كفاءة القطع ومخاطر Off-Target غير محسوبة.",
        badgeText = "CRISPR-Cas9 • PAM Scan",
        defaultSequence = ">HBB_Exon1_Target_Region | Homo sapiens beta-globin locus\nATGGTGCACCTGACTCCTGAGGAGAAGTCTGCCGTTACTGCCCTGTGGGGCAAGGTGAACGTGGATGAAGTTGGTGGTGAGGCCCTGGGCAGGCTGCTGGTGGTCTACCCTTGGACCCAGAGGTTCTTTGAGTCCTTTGGGGATCTGTCCACTCCTGATGCTGTTATGGGCAACCCTAAGGTGAAGGCTCATGGCAAGAAAGTGCTCGGTGCCTTTAGTGATGGCCTGGCTCACCTGGACAACCTCAAGGGCACCTTTGCCACACTGAGTGAGCTGCACTGTGACAAGCTGCACGTGGATCCTGAGAACTTCAGG",
        defaultGoal = "حصر مواقع PAM (NGG) المحتملة في التسلسل. يتطلب تقييم فعالية الدليل والقطع خارج الهدف أداة تخصصية وبيانات إضافية غير متاحة هنا."
    ),
    GENOMIC_ALIGNMENT(
        id = "GENOMIC_ALIGNMENT",
        titleAr = "محاذاة الجينوم واكتشاف الطفرات",
        subtitleAr = "حساب GC% وTm التقريبية، واستخراج ORF يبدأ بـ ATG عند وجوده؛ لا يتوفر هنا تحليل متغيرات جينية.",
        badgeText = "Genomics • Sequence Metrics",
        defaultSequence = ">BRCA1_RING_Domain_Amplicon | Genomic Reference Segment\nATGGATTTATCTGCTCTTCGCGTTGAAGAAGTACAAAATGTCATTAATGCTATGCAGAAAATCTTAGAGTGTCCCATCTGTCTGGAGTTGATCAAGGAACCTGTCTCCACAAAGTGTGACCACATATTTTGCAAATTTTGCATGCTGAAACTTCTCAACCAGAAGAAAGGGCCTTCACAGTGTCCTTTATGTAAGAATGATATAACCAAAAGGAGCCTACAAGAAAGTACGAGATTTAGTCAACTTGTTGAAGAGCTATTGAAAATCATTTGTGCTTTTCAGCTTGACACAGGTTTGGAGTATGCAAACAGCTATAAT",
        defaultGoal = "تحليل GC% وTm التقريبية للتسلسل، واستخراج إطار قرائي يبدأ بـ ATG إن وجد؛ لا يتضمن هذا المسار Variant Calling أو تحليل ثبات البادئات."
    ),
    CLINICAL_TRIALS(
        id = "CLINICAL_TRIALS",
        titleAr = "ملاحظات المرشح الحيوي (دون تقييم سريري)",
        subtitleAr = "يمكن حفظ تسلسل المرشح وملاحظاته؛ IC50 وΔG ومؤشر الأمان والجاهزية السريرية غير محسوبة.",
        badgeText = "Clinical Notes • Unscored",
        defaultSequence = ">GLP1R_Peptide_Agonist_Candidate | Clinical Lead Sequence\nHAEGTFTSDVSSYLEGQAAKEFIAWLVKGRG",
        defaultGoal = "حفظ وتحليل وصفي لتسلسل المرشح الببتيدي. يتطلب حساب IC50 وΔG ومؤشر الأمان والجاهزية السريرية بيانات وتجارب وأدوات تخصصية غير مهيأة في التطبيق."
    )
}

data class PamTargetSite(
    val indexOneBased: Int,
    val spacer20nt: String,
    val pamTriplet: String,
    val strand: String,
    val gcPercent: Double,
    val onTargetScore: Int?,
    val offTargetRisk: String
)

data class BioSequenceMetrics(
    val headerTitle: String,
    val cleanedSequence: String,
    val sequenceType: String, // "DNA", "RNA", "PROTEIN"
    val sequenceLength: Int,
    val gcContentPct: Double,
    val meltingTempC: Double,
    val molecularWeightKda: Double,
    val gravyHydropathy: Double,
    val alphaHelixPct: Int,
    val betaSheetPct: Int,
    val randomCoilPct: Int,
    val plddtConfidence: Double,
    val translatedProteinPreview: String,
    val pamSites: List<PamTargetSite>,
    val predictedIc50Nm: Double,
    val bindingAffinityKcal: Double,
    val therapeuticIndex: Double,
    val clinicalPhaseReadiness: String
)

data class Bio3dScenePreset(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val partsCount: Int,
    val highlightColorHex: String
)

object BioscienceSimulationEngine {

    val scenePresets: List<Bio3dScenePreset> = listOf(
        Bio3dScenePreset(
            id = "DNA_HELIX",
            titleAr = "اللولب المزدوج DNA",
            subtitleAr = "سلم نوكليوتيدي ثلاثي الأبعاد مع روابط هيدروجينية مضيئة",
            partsCount = 12,
            highlightColorHex = "#00F5D4"
        ),
        Bio3dScenePreset(
            id = "PROTEIN_COMPLEX",
            titleAr = "معقد طي البروتين 3D",
            subtitleAr = "نطاقات حلزون ألفا وصفائح بيتا مع جيب الارتباط النشط",
            partsCount = 10,
            highlightColorHex = "#10B981"
        ),
        Bio3dScenePreset(
            id = "CRISPR_CAS9",
            titleAr = "معقد CRISPR-Cas9 RNP",
            subtitleAr = "إنزيم Cas9 ثنائي الفصوص مع شريط دليل gRNA وموقع PAM",
            partsCount = 9,
            highlightColorHex = "#8B5CF6"
        ),
        Bio3dScenePreset(
            id = "VIRAL_NANOBOT",
            titleAr = "كابسيد فيروسي وروبوت نانوي",
            subtitleAr = "قفيصة عشرونية الوجوه مع بروتينات شوكية ومسبار علاجي",
            partsCount = 11,
            highlightColorHex = "#F59E0B"
        )
    )

    private val codonTable = mapOf(
        "TTT" to 'F', "TTC" to 'F', "TTA" to 'L', "TTG" to 'L',
        "CTT" to 'L', "CTC" to 'L', "CTA" to 'L', "CTG" to 'L',
        "ATT" to 'I', "ATC" to 'I', "ATA" to 'I', "ATG" to 'M',
        "GTT" to 'V', "GTC" to 'V', "GTA" to 'V', "GTG" to 'V',
        "TCT" to 'S', "TCC" to 'S', "TCA" to 'S', "TCG" to 'S',
        "CCT" to 'P', "CCC" to 'P', "CCA" to 'P', "CCG" to 'P',
        "ACT" to 'T', "ACC" to 'T', "ACA" to 'T', "ACG" to 'T',
        "GCT" to 'A', "GCC" to 'A', "GCA" to 'A', "GCG" to 'A',
        "TAT" to 'Y', "TAC" to 'Y', "TAA" to '*', "TAG" to '*',
        "CAT" to 'H', "CAC" to 'H', "CAA" to 'Q', "CAG" to 'Q',
        "AAT" to 'N', "AAC" to 'N', "AAA" to 'K', "AAG" to 'K',
        "GAT" to 'D', "GAC" to 'D', "GAA" to 'E', "GAG" to 'E',
        "TGT" to 'C', "TGC" to 'C', "TGA" to '*', "TGG" to 'W',
        "CGT" to 'R', "CGC" to 'R', "CGA" to 'R', "CGG" to 'R',
        "AGT" to 'S', "AGC" to 'S', "AGA" to 'R', "AGG" to 'R',
        "GGT" to 'G', "GGC" to 'G', "GGA" to 'G', "GGG" to 'G'
    )

    private val kyteDoolittle = mapOf(
        'I' to 4.5, 'V' to 4.2, 'L' to 3.8, 'F' to 2.8, 'C' to 2.5,
        'M' to 1.9, 'A' to 1.8, 'G' to -0.4, 'T' to -0.7, 'S' to -0.8,
        'W' to -0.9, 'Y' to -1.3, 'P' to -1.6, 'H' to -3.2, 'E' to -3.5,
        'Q' to -3.5, 'D' to -3.5, 'N' to -3.5, 'K' to -3.9, 'R' to -4.5
    )

    fun analyzeSequenceMetrics(
        rawInput: String,
        workflowMode: BioscienceWorkflowMode = BioscienceWorkflowMode.PROTEIN_FOLDING
    ): BioSequenceMetrics {
        val lines = rawInput.lines()
        val header = lines.firstOrNull { it.trim().startsWith(">") }
            ?.removePrefix(">")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: workflowMode.titleAr

        val cleaned = lines
            .filterNot { it.trim().startsWith(">") }
            .joinToString("")
            .uppercase(Locale.US)
            .filterNot { it.isWhitespace() || it.isDigit() }

        require(cleaned.isNotEmpty()) { "أدخل تسلسلاً حيوياً صالحاً للتحليل." }
        val validProteinSymbols = "ACDEFGHIKLMNPQRSTVWY"
        val nucleotideWorkflow = workflowMode == BioscienceWorkflowMode.CRISPR_EDITING ||
            workflowMode == BioscienceWorkflowMode.GENOMIC_ALIGNMENT
        val seqType = if (nucleotideWorkflow) {
            require(cleaned.all { it in "ATGCUN" }) {
                "هذا المسار يتطلب تسلسلاً نووياً يحتوي فقط على A/T/G/C/U/N."
            }
            require(!(cleaned.contains('T') && cleaned.contains('U'))) {
                "لا يمكن مزج T و U في التسلسل نفسه."
            }
            if (cleaned.contains('U')) "RNA" else "DNA"
        } else {
            require(cleaned.all { it in validProteinSymbols }) {
                "هذا المسار يتطلب تسلسلاً بروتينياً بأحرف الأحماض الأمينية القياسية."
            }
            "PROTEIN"
        }
        val isNucleic = seqType == "DNA" || seqType == "RNA"

        val length = cleaned.length
        val gcCount = cleaned.count { it == 'G' || it == 'C' }
        val atCount = cleaned.count { it == 'A' || it == 'T' || it == 'U' }
        val gcPct = if (isNucleic && length > 0) {
            ((gcCount.toDouble() / length.toDouble()) * 1000.0).roundToInt() / 10.0
        } else {
            0.0
        }

        val meltingTemp = if (isNucleic) {
            if (length < 14) {
                (2.0 * atCount) + (4.0 * gcCount)
            } else {
                val rawTm = 64.9 + 41.0 * (gcCount - 16.4) / length.toDouble()
                ((rawTm * 10.0).roundToInt()) / 10.0
            }
        } else {
            0.0
        }

        val mwDaltons = if (isNucleic) {
            length * 327.0
        } else {
            length * 110.5
        }
        val mwKda = ((mwDaltons / 1000.0) * 100.0).roundToInt() / 100.0

        val translatedProtein = if (isNucleic) {
            val dnaSeq = cleaned.replace('U', 'T')
            val startIndex = dnaSeq.indexOf("ATG")
            if (startIndex < 0) {
                ""
            } else {
                val sb = StringBuilder()
                var i = startIndex
                while (i + 2 < dnaSeq.length && sb.length < 180) {
                    val aa = codonTable[dnaSeq.substring(i, i + 3)] ?: 'X'
                    if (aa == '*') break
                    sb.append(aa)
                    i += 3
                }
                sb.toString()
            }
        } else {
            cleaned.take(180)
        }

        val gravy = if (seqType == "PROTEIN" && translatedProtein.isNotEmpty()) {
            val sum = translatedProtein.sumOf { kyteDoolittle[it] ?: 0.0 }
            ((sum / translatedProtein.length.toDouble()) * 100.0).roundToInt() / 100.0
        } else {
            0.0
        }

        // Structure fractions require a real structure-prediction tool and are not inferred here.
        val rawHelix = 0
        val normalizedSheet = 0
        val coilPct = 0

        // pLDDT is a model confidence score and cannot be inferred from sequence composition.
        val plddt = 0.0

        // Detect forward-strand NGG motifs only; no off-target or activity score is inferred.
        val dnaForPam = if (seqType == "DNA") cleaned else ""

        val discoveredPams = mutableListOf<PamTargetSite>()
        for (idx in 20..(dnaForPam.length - 3)) {
            val window3 = dnaForPam.substring(idx, idx + 3)
            if (window3.endsWith("GG")) {
                val spacer = dnaForPam.substring(idx - 20, idx)
                val spacerGc = ((spacer.count { it == 'G' || it == 'C' } / 20.0) * 1000.0).roundToInt() / 10.0
                discoveredPams.add(
                    PamTargetSite(
                        indexOneBased = idx - 19,
                        spacer20nt = spacer,
                        pamTriplet = window3,
                        strand = "+ (Sense)",
                        gcPercent = spacerGc,
                        onTargetScore = null,
                        offTargetRisk = "غير محسوب — يلزم تحليل خارج الهدف"
                    )
                )
            }
            if (discoveredPams.size >= 6) break
        }

        // No synthetic PAM site or clinical/binding prediction is inserted when no model is run.
        val ic50Nm = 0.0
        val deltaG = 0.0
        val therapeuticIdx = 0.0
        val phaseStatus = "غير محسوب — يتطلب أدوات تخصصية وبيانات تجريبية"

        return BioSequenceMetrics(
            headerTitle = header,
            cleanedSequence = cleaned,
            sequenceType = seqType,
            sequenceLength = length,
            gcContentPct = gcPct,
            meltingTempC = meltingTemp,
            molecularWeightKda = mwKda,
            gravyHydropathy = gravy,
            alphaHelixPct = rawHelix,
            betaSheetPct = normalizedSheet,
            randomCoilPct = coilPct,
            plddtConfidence = plddt,
            translatedProteinPreview = translatedProtein,
            pamSites = discoveredPams,
            predictedIc50Nm = ic50Nm,
            bindingAffinityKcal = deltaG,
            therapeuticIndex = therapeuticIdx,
            clinicalPhaseReadiness = phaseStatus
        )
    }

    fun generateExportablePipelineCode(
        workflowMode: BioscienceWorkflowMode,
        metrics: BioSequenceMetrics
    ): String {
        val topPam = metrics.pamSites.firstOrNull()
        return """
# ============================================================================
# NovaForge Bioscience & Genomic Pipeline (${workflowMode.badgeText})
# Target: ${metrics.headerTitle}
# Sequence Type: ${metrics.sequenceType} | Length: ${metrics.sequenceLength} | MW: ${metrics.molecularWeightKda} kDa
# ============================================================================

from dataclasses import dataclass
from typing import Dict, Optional

@dataclass
class BioTargetReport:
    target_name: str = "${metrics.headerTitle.replace("\"", "'")}"
    sequence_type: str = "${metrics.sequenceType}"
    length: int = ${metrics.sequenceLength}
    gc_percent: Optional[float] = ${if (metrics.sequenceType == "PROTEIN") "None" else metrics.gcContentPct.toString()}
    melting_temp_c: Optional[float] = ${if (metrics.sequenceType == "PROTEIN") "None" else metrics.meltingTempC.toString()}
    molecular_weight_kda: float = ${metrics.molecularWeightKda}
    plddt_confidence: Optional[float] = None  # Not predicted by this pipeline
    predicted_ic50_nm: Optional[float] = None  # Requires experimental validation
    binding_energy_kcal: Optional[float] = None  # Requires a validated structure/model

def run_novaforge_bio_pipeline() -> Dict[str, object]:
    seq = "${metrics.cleanedSequence.take(120)}"
    orf_protein = "${metrics.translatedProteinPreview.take(80)}"
    secondary_structure = {
        "alpha_helix_pct": None,  # No secondary-structure prediction was run
        "beta_sheet_pct": None,
        "random_coil_pct": None,
        "gravy_hydropathy": ${if (metrics.sequenceType == "PROTEIN") metrics.gravyHydropathy.toString() else "None"}
    }
    crispr_lead_guide = {
        "spacer_20nt": ${topPam?.spacer20nt?.let { "\"$it\"" } ?: "None"},
        "pam": ${topPam?.pamTriplet?.let { "\"$it\"" } ?: "None"},
        "on_target_score": ${topPam?.onTargetScore ?: "None"},
        "off_target_risk": ${topPam?.offTargetRisk?.let { "\"$it\"" } ?: "None"}
    }
    return {
        "report": BioTargetReport().__dict__,
        "orf_translation": orf_protein,
        "secondary_structure": secondary_structure,
        "crispr_lead_guide": crispr_lead_guide,
        "clinical_readiness": None
    }

if __name__ == "__main__":
    result = run_novaforge_bio_pipeline()
    print("[NovaForge BioLab] Descriptive sequence summary created:", result["report"]["target_name"])
""".trimIndent()
    }

    val THREEJS_GAME_HTML: String = """
<div id="nova-bio3d-app" class="bio3d-shell">
  <!-- Viewport Canvas -->
  <canvas id="bio3d-canvas"></canvas>

  <!-- Low-Occlusion HUD Overlay (pointer-events: none by default) -->
  <div class="hud-layer">
    <div class="hud-top-bar">
      <div class="hud-pill brand-pill">
        <span class="pulse-dot"></span>
        <span id="hud-mode-label">CELLULAR DEFENSE 3D</span>
      </div>
      <div class="hud-stats" id="hud-game-stats">
        <div class="stat-chip"><span>WAVE</span><strong id="hud-wave">1 / 3</strong></div>
        <div class="stat-chip"><span>SCORE</span><strong id="hud-score">0</strong></div>
        <div class="stat-chip"><span>COMBO</span><strong id="hud-combo">x1</strong></div>
        <div class="stat-chip hp-chip">
          <span>CELL HP</span>
          <div class="hp-bar-outer"><div id="hud-hp-fill" class="hp-bar-fill"></div></div>
          <strong id="hud-hp-text">100%</strong>
        </div>
      </div>
      <div class="hud-actions">
        <button id="btn-switch-view" class="hud-btn" title="Switch Game / 3D Inspector">🔬 3D Inspector</button>
        <button id="btn-audio-mute" class="hud-btn" title="Toggle WebAudio Synth">🔊 SFX</button>
        <button id="btn-pause-game" class="hud-btn" title="Pause Simulation">⏸</button>
      </div>
    </div>

    <!-- 3D Scene Inspector Selector Bar (visible in Inspector mode or quick model switch) -->
    <div id="inspector-toolbar" class="inspector-toolbar hidden">
      <button class="model-chip active" data-model="DNA_HELIX">🧬 DNA Helix</button>
      <button class="model-chip" data-model="PROTEIN_COMPLEX">🧪 Protein Fold</button>
      <button class="model-chip" data-model="CRISPR_CAS9">✂️ CRISPR-Cas9</button>
      <button class="model-chip" data-model="VIRAL_NANOBOT">🛡️ Viral & Nanobot</button>
      <button id="btn-explode-view" class="model-chip accent">💥 تفكيك الأجزاء</button>
      <button id="btn-reset-cam" class="model-chip">🎯 ضبط الكاميرا</button>
    </div>

    <!-- Dynamic Telemetry / Telegraph Banner -->
    <div id="telegraph-banner" class="telegraph-banner hidden">⚠️ هجوم فيروسي وشيك! استخدم درع النانو أو نبضة CRISPR</div>

    <!-- Bottom Touch Controls for Game Mode -->
    <div id="hud-touch-controls" class="hud-bottom-controls">
      <!-- Virtual Touch Joystick -->
      <div id="joystick-zone" class="joystick-zone">
        <div id="joystick-base" class="joystick-base">
          <div id="joystick-stick" class="joystick-stick"></div>
        </div>
        <span class="joy-caption">تحريك المسبار النانوي</span>
      </div>

      <!-- Interactive Skill Buttons with Cooldown Indicators -->
      <div class="skill-buttons-group">
        <button id="btn-skill-shield" class="skill-btn shield-btn">
          <span class="skill-icon">🛡️</span>
          <span class="skill-name">Shield</span>
          <span id="cd-shield" class="cd-badge">READY</span>
        </button>
        <button id="btn-skill-crispr" class="skill-btn crispr-btn">
          <span class="skill-icon">🧬</span>
          <span class="skill-name">CRISPR Wave</span>
          <span id="cd-crispr" class="cd-badge">READY</span>
        </button>
        <button id="btn-skill-fire" class="skill-btn fire-btn">
          <span class="skill-icon">⚡</span>
          <span class="skill-name">Antibody</span>
          <span class="cd-badge">AUTO/TAP</span>
        </button>
      </div>
    </div>
  </div>

  <!-- Title / Pause / Victory / Game Over Modal State Machine -->
  <div id="game-modal-overlay" class="modal-overlay">
    <div class="modal-card">
      <div class="modal-badge" id="modal-badge">NOVAFORGE 3D BIO-ENGINE</div>
      <h2 id="modal-title">الدفاع الخلوي الجزيئي 3D</h2>
      <p id="modal-desc">قد المسبار النانوي لحماية نواة الخلية البشرية من الكابسيدات الفيروسية عبر موجات الأجسام المضادة ونبضات CRISPR-Cas9.</p>
      <div id="modal-summary-stats" class="modal-summary-stats hidden">
        <div><span>النقاط النهائية</span><strong id="final-score">0</strong></div>
        <div><span>أعلى كومبو</span><strong id="final-combo">x1</strong></div>
        <div><span>سلامة النواة</span><strong id="final-hp">100%</strong></div>
      </div>
      <div class="modal-btn-row">
        <button id="btn-start-play" class="primary-play-btn">🚀 ابدأ المحاكاة القتالية</button>
        <button id="btn-open-inspector" class="secondary-play-btn">🔬 مستعرض الهياكل 3D</button>
      </div>
    </div>
  </div>
</div>
""".trimIndent()

    val THREEJS_GAME_CSS: String = """
* { box-sizing: border-box; user-select: none; -webkit-user-select: none; }
html, body {
  margin: 0; padding: 0; width: 100%; height: 100%;
  background: #050914; color: #F0F6FC;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden; touch-action: none;
}
.bio3d-shell {
  position: relative; width: 100%; height: 100vh; min-height: 440px;
  background: radial-gradient(circle at 50% 35%, #0D1B38 0%, #050914 78%);
  overflow: hidden;
}
#bio3d-canvas {
  width: 100%; height: 100%; display: block;
}
.hud-layer {
  position: absolute; inset: 0;
  pointer-events: none;
  display: flex; flex-direction: column; justify-content: space-between;
  padding: 10px 12px;
}
.hud-top-bar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px; flex-wrap: wrap;
}
.hud-pill {
  display: inline-flex; align-items: center; gap: 6px;
  background: rgba(10, 18, 36, 0.78);
  border: 1px solid rgba(0, 245, 212, 0.45);
  padding: 5px 10px; border-radius: 999px;
  font-size: 11px; font-weight: 800; letter-spacing: 0.6px; color: #00F5D4;
  backdrop-filter: blur(8px);
}
.pulse-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #10B981; box-shadow: 0 0 8px #10B981;
}
.hud-stats {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}
.stat-chip {
  background: rgba(13, 21, 38, 0.78);
  border: 1px solid rgba(139, 92, 246, 0.35);
  border-radius: 8px; padding: 4px 8px;
  display: flex; align-items: center; gap: 6px;
  font-size: 11px;
}
.stat-chip span { color: #8B949E; font-size: 10px; font-weight: 700; }
.stat-chip strong { color: #F0F6FC; font-family: monospace; font-size: 12px; }
.hp-bar-outer {
  width: 56px; height: 7px; background: rgba(255,255,255,0.12);
  border-radius: 99px; overflow: hidden;
}
.hp-bar-fill {
  width: 100%; height: 100%;
  background: linear-gradient(90deg, #10B981, #00F5D4);
  transition: width 0.18s ease;
}
.hud-actions {
  display: flex; align-items: center; gap: 6px;
  pointer-events: auto;
}
.hud-btn {
  pointer-events: auto;
  background: rgba(17, 26, 46, 0.88);
  color: #F0F6FC;
  border: 1px solid rgba(0, 245, 212, 0.4);
  border-radius: 8px; padding: 6px 10px;
  font-size: 11px; font-weight: 700; cursor: pointer;
}
.hud-btn:active { transform: scale(0.95); }
.inspector-toolbar {
  pointer-events: auto;
  display: flex; gap: 6px; flex-wrap: wrap;
  justify-content: center; margin-top: 6px;
}
.inspector-toolbar.hidden { display: none; }
.model-chip {
  background: rgba(13, 21, 38, 0.88);
  color: #C9D1D9;
  border: 1px solid rgba(255,255,255,0.16);
  border-radius: 999px; padding: 6px 11px;
  font-size: 11px; font-weight: 700; cursor: pointer;
}
.model-chip.active {
  background: rgba(0, 245, 212, 0.18);
  color: #00F5D4; border-color: #00F5D4;
}
.model-chip.accent {
  background: rgba(139, 92, 246, 0.22);
  color: #C4B5FD; border-color: #8B5CF6;
}
.telegraph-banner {
  align-self: center;
  background: rgba(239, 68, 68, 0.24);
  border: 1px solid #EF4444;
  color: #FCA5A5; padding: 5px 14px; border-radius: 999px;
  font-size: 11px; font-weight: 800;
  animation: pulseWarn 0.6s infinite alternate;
}
.telegraph-banner.hidden { display: none; }
@keyframes pulseWarn {
  from { opacity: 0.65; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1.03); }
}
.hud-bottom-controls {
  display: flex; align-items: flex-end; justify-content: space-between;
  padding-bottom: 4px;
}
.hud-bottom-controls.hidden { display: none; }
.joystick-zone {
  pointer-events: auto;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.joystick-base {
  width: 86px; height: 86px; border-radius: 50%;
  background: rgba(13, 21, 38, 0.62);
  border: 2px solid rgba(0, 245, 212, 0.45);
  position: relative; display: flex; align-items: center; justify-content: center;
  box-shadow: inset 0 0 16px rgba(0, 245, 212, 0.18);
}
.joystick-stick {
  width: 36px; height: 36px; border-radius: 50%;
  background: radial-gradient(circle, #00F5D4 0%, #0EA5E9 100%);
  box-shadow: 0 0 12px rgba(0, 245, 212, 0.75);
  transform: translate(0px, 0px);
}
.joy-caption { font-size: 10px; color: #8B949E; font-weight: 600; }
.skill-buttons-group {
  pointer-events: auto;
  display: flex; align-items: center; gap: 8px;
}
.skill-btn {
  width: 66px; height: 66px; border-radius: 16px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 2px; border: 1.5px solid rgba(255,255,255,0.22);
  color: #FFFFFF; cursor: pointer;
  background: rgba(15, 23, 42, 0.82);
  backdrop-filter: blur(6px);
}
.skill-btn:active { transform: scale(0.92); }
.fire-btn { border-color: #00F5D4; box-shadow: 0 0 14px rgba(0, 245, 212, 0.25); }
.crispr-btn { border-color: #8B5CF6; box-shadow: 0 0 14px rgba(139, 92, 246, 0.25); }
.shield-btn { border-color: #10B981; box-shadow: 0 0 14px rgba(16, 185, 129, 0.25); }
.skill-icon { font-size: 18px; line-height: 1; }
.skill-name { font-size: 10px; font-weight: 800; }
.cd-badge { font-size: 9px; font-family: monospace; color: #00F5D4; }
.modal-overlay {
  position: absolute; inset: 0;
  background: rgba(5, 9, 20, 0.76);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  padding: 16px; z-index: 20;
}
.modal-overlay.hidden { display: none; }
.modal-card {
  max-width: 390px; width: 100%;
  background: linear-gradient(160deg, #111B30 0%, #0A1020 100%);
  border: 1px solid rgba(0, 245, 212, 0.45);
  border-radius: 18px; padding: 20px; text-align: center;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.65);
}
.modal-badge {
  display: inline-block; padding: 4px 10px; border-radius: 999px;
  background: rgba(0, 245, 212, 0.14); color: #00F5D4;
  font-size: 10px; font-weight: 800; letter-spacing: 0.8px;
}
.modal-card h2 { margin: 10px 0 6px; font-size: 20px; color: #F0F6FC; }
.modal-card p { margin: 0 0 14px; font-size: 12.5px; color: #9CA3AF; line-height: 1.5; }
.modal-summary-stats {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
  margin-bottom: 14px; background: rgba(5, 9, 20, 0.65);
  padding: 10px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.08);
}
.modal-summary-stats.hidden { display: none; }
.modal-summary-stats span { display: block; font-size: 10px; color: #8B949E; }
.modal-summary-stats strong { font-size: 14px; color: #00F5D4; font-family: monospace; }
.modal-btn-row { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.primary-play-btn {
  background: linear-gradient(90deg, #00F5D4, #10B981);
  color: #041019; border: none; border-radius: 10px;
  padding: 10px 16px; font-size: 13px; font-weight: 900; cursor: pointer;
}
.secondary-play-btn {
  background: rgba(139, 92, 246, 0.22);
  color: #DDD6FE; border: 1px solid #8B5CF6; border-radius: 10px;
  padding: 10px 14px; font-size: 12px; font-weight: 800; cursor: pointer;
}
""".trimIndent()

    val THREEJS_GAME_JS: String = """
(function() {
  const canvas = document.getElementById('bio3d-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // UI Elements
  const modalOverlay = document.getElementById('game-modal-overlay');
  const modalBadge = document.getElementById('modal-badge');
  const modalTitle = document.getElementById('modal-title');
  const modalDesc = document.getElementById('modal-desc');
  const modalStats = document.getElementById('modal-summary-stats');
  const finalScoreEl = document.getElementById('final-score');
  const finalComboEl = document.getElementById('final-combo');
  const finalHpEl = document.getElementById('final-hp');

  const hudModeLabel = document.getElementById('hud-mode-label');
  const hudGameStats = document.getElementById('hud-game-stats');
  const hudWave = document.getElementById('hud-wave');
  const hudScore = document.getElementById('hud-score');
  const hudCombo = document.getElementById('hud-combo');
  const hudHpFill = document.getElementById('hud-hp-fill');
  const hudHpText = document.getElementById('hud-hp-text');
  const inspectorToolbar = document.getElementById('inspector-toolbar');
  const hudTouchControls = document.getElementById('hud-touch-controls');
  const telegraphBanner = document.getElementById('telegraph-banner');

  const btnStartPlay = document.getElementById('btn-start-play');
  const btnOpenInspector = document.getElementById('btn-open-inspector');
  const btnSwitchView = document.getElementById('btn-switch-view');
  const btnAudioMute = document.getElementById('btn-audio-mute');
  const btnPauseGame = document.getElementById('btn-pause-game');
  const btnExplodeView = document.getElementById('btn-explode-view');
  const btnResetCam = document.getElementById('btn-reset-cam');

  const btnSkillFire = document.getElementById('btn-skill-fire');
  const btnSkillCrispr = document.getElementById('btn-skill-crispr');
  const btnSkillShield = document.getElementById('btn-skill-shield');
  const cdCrisprEl = document.getElementById('cd-crispr');
  const cdShieldEl = document.getElementById('cd-shield');

  // Procedural Web Audio Synthesizer
  let audioCtx = null;
  let isMuted = false;
  function playTone(freq, type, duration, endFreq) {
    if (isMuted) return;
    try {
      if (!audioCtx) {
        const AC = window.AudioContext || window.webkitAudioContext;
        if (AC) audioCtx = new AC();
      }
      if (!audioCtx) return;
      if (audioCtx.state === 'suspended') audioCtx.resume();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = type || 'sine';
      osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
      if (endFreq) {
        osc.frequency.exponentialRampToValueAtTime(endFreq, audioCtx.currentTime + duration);
      }
      gain.gain.setValueAtTime(0.12, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + duration);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + duration);
    } catch (e) {}
  }

  // State Machine: TITLE | PLAYING | PAUSED | VICTORY | GAMEOVER | INSPECTOR
  let appMode = window.INITIAL_BIO3D_MODE || 'GAME'; // 'GAME' or 'INSPECTOR'
  let activeMolecule = window.INITIAL_BIO3D_MOLECULE || 'DNA_HELIX';
  let gameState = appMode === 'INSPECTOR' ? 'INSPECTOR' : 'TITLE';

  // 3D Camera & Scene Parameters
  let rotX = 0.35;
  let rotY = 0.65;
  let targetRotX = 0.35;
  let targetRotY = 0.65;
  let cameraZoom = 1.0;
  let autoRotate = true;
  let explodeFactor = 0.0;
  let targetExplode = 0.0;

  // Game Entities & Pools
  let wave = 1;
  let score = 0;
  let combo = 1;
  let maxCombo = 1;
  let cellHp = 100;
  let shieldTimer = 0;
  let crisprCd = 0;
  let shieldCd = 0;
  let fireTimer = 0;
  let hitstopFrames = 0;
  let shakeIntensity = 0;

  const player = { x: 0, y: 0, z: 0, vx: 0, vy: 0, angle: 0 };
  const joyVector = { x: 0, y: 0 };
  const keys = {};
  const projectiles = [];
  const pathogens = [];
  const particles = [];
  const bgOrganelles = [];

  for (let i = 0; i < 28; i++) {
    bgOrganelles.push({
      x: (Math.random() - 0.5) * 900,
      y: (Math.random() - 0.5) * 900,
      z: (Math.random() - 0.5) * 700,
      r: 3 + Math.random() * 8,
      hue: Math.random() > 0.5 ? '#00F5D4' : '#8B5CF6'
    });
  }

  function resizeCanvas() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = canvas.clientWidth * dpr;
    canvas.height = canvas.clientHeight * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }
  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  function spawnWave(waveNum) {
    pathogens.length = 0;
    const count = 5 + waveNum * 3;
    for (let i = 0; i < count; i++) {
      const ang = (Math.PI * 2 * i) / count;
      const dist = 165 + (i % 3) * 38;
      pathogens.push({
        x: Math.cos(ang) * dist,
        y: Math.sin(ang) * dist,
        z: (Math.random() - 0.5) * 90,
        hp: 2 + waveNum,
        maxHp: 2 + waveNum,
        radius: 15 + (i % 2) * 4,
        speed: 0.42 + waveNum * 0.12,
        telegraphTimer: 100 + (i * 27) % 120,
        lungeTimer: 0,
        phase: i * 0.9
      });
    }
  }

  function spawnParticles(x, y, z, color, count) {
    for (let i = 0; i < count; i++) {
      if (particles.length > 140) particles.shift();
      const a = Math.random() * Math.PI * 2;
      const spd = 1.5 + Math.random() * 4.5;
      particles.push({
        x, y, z,
        vx: Math.cos(a) * spd,
        vy: Math.sin(a) * spd,
        vz: (Math.random() - 0.5) * 4,
        life: 1.0,
        color
      });
    }
  }

  function startNewGame() {
    wave = 1;
    score = 0;
    combo = 1;
    maxCombo = 1;
    cellHp = 100;
    shieldTimer = 0;
    crisprCd = 0;
    shieldCd = 0;
    player.x = 0;
    player.y = 65;
    projectiles.length = 0;
    particles.length = 0;
    spawnWave(1);
    gameState = 'PLAYING';
    appMode = 'GAME';
    syncUiForMode();
    playTone(440, 'triangle', 0.22, 880);
  }

  function fireAntibodyBurst() {
    if (gameState !== 'PLAYING') return;
    // Aim toward nearest pathogen or forward
    let tx = 0, ty = -1;
    let bestDist = 999999;
    for (const p of pathogens) {
      const dx = p.x - player.x;
      const dy = p.y - player.y;
      const d = dx * dx + dy * dy;
      if (d < bestDist) {
        bestDist = d;
        const len = Math.max(1, Math.hypot(dx, dy));
        tx = dx / len;
        ty = dy / len;
      }
    }
    projectiles.push({
      x: player.x, y: player.y, z: 0,
      vx: tx * 7.5, vy: ty * 7.5,
      color: '#00F5D4', life: 70
    });
    playTone(720, 'sine', 0.09, 340);
  }

  function triggerCrisprWave() {
    if (gameState !== 'PLAYING' || crisprCd > 0) return;
    crisprCd = 5.0;
    shakeIntensity = 9;
    hitstopFrames = 4;
    playTone(280, 'sawtooth', 0.35, 920);
    spawnParticles(player.x, player.y, 0, '#8B5CF6', 34);
    for (let i = pathogens.length - 1; i >= 0; i--) {
      const p = pathogens[i];
      const dist = Math.hypot(p.x - player.x, p.y - player.y);
      if (dist < 175) {
        p.hp -= 3;
        spawnParticles(p.x, p.y, p.z, '#00F5D4', 12);
        if (p.hp <= 0) {
          pathogens.splice(i, 1);
          score += 150 * combo;
          combo = Math.min(12, combo + 1);
          maxCombo = Math.max(maxCombo, combo);
        }
      }
    }
    checkWaveProgression();
  }

  function triggerNanoShield() {
    if (gameState !== 'PLAYING' || shieldCd > 0) return;
    shieldTimer = 4.0;
    shieldCd = 7.0;
    playTone(360, 'sine', 0.28, 640);
    spawnParticles(0, 0, 0, '#10B981', 24);
  }

  function checkWaveProgression() {
    if (pathogens.length === 0 && gameState === 'PLAYING') {
      if (wave < 3) {
        wave++;
        cellHp = Math.min(100, cellHp + 15);
        playTone(520, 'triangle', 0.3, 1040);
        spawnWave(wave);
      } else {
        gameState = 'VICTORY';
        playTone(587, 'triangle', 0.45, 1174);
        showEndModal(true);
      }
    }
  }

  function showEndModal(isVictory) {
    modalOverlay.classList.remove('hidden');
    modalStats.classList.remove('hidden');
    finalScoreEl.textContent = String(score);
    finalComboEl.textContent = 'x' + maxCombo;
    finalHpEl.textContent = Math.max(0, Math.round(cellHp)) + '%';
    if (isVictory) {
      modalBadge.textContent = 'MISSION ACCOMPLISHED • CRISPR IMMUNITY';
      modalTitle.textContent = 'انتصار خلوي كامل! 🎉';
      modalDesc.textContent = 'تم تحييد كافة الكابسيدات الفيروسية وحماية الجينوم البشري بنجاح.';
      btnStartPlay.textContent = '🔄 العب مرة أخرى (Play Again)';
    } else {
      modalBadge.textContent = 'CELLULAR BREACH DETECTED';
      modalTitle.textContent = 'انتهت المحاكاة • تضررت النواة';
      modalDesc.textContent = 'اخترقت الكابسيدات الغشاء النووي. أعد تشغيل الموجة فوراً بتكتيك دفاعي جديد.';
      btnStartPlay.textContent = '⚡ إعادة فورية (Instant Restart)';
    }
  }

  function syncUiForMode() {
    if (appMode === 'INSPECTOR') {
      gameState = 'INSPECTOR';
      modalOverlay.classList.add('hidden');
      inspectorToolbar.classList.remove('hidden');
      hudTouchControls.classList.add('hidden');
      hudGameStats.style.display = 'none';
      telegraphBanner.classList.add('hidden');
      hudModeLabel.textContent = '3D MOLECULAR INSPECTOR • ' + activeMolecule;
      btnSwitchView.textContent = '🎮 لعبة الدفاع الخلوي';
    } else {
      inspectorToolbar.classList.add('hidden');
      hudTouchControls.classList.remove('hidden');
      hudGameStats.style.display = 'flex';
      hudModeLabel.textContent = 'CELLULAR DEFENSE 3D';
      btnSwitchView.textContent = '🔬 مستعرض 3D';
      if (gameState === 'TITLE') {
        modalOverlay.classList.remove('hidden');
      } else if (gameState === 'PLAYING') {
        modalOverlay.classList.add('hidden');
      }
    }
  }

  // Event Listeners
  btnStartPlay.addEventListener('click', () => startNewGame());
  btnOpenInspector.addEventListener('click', () => {
    appMode = 'INSPECTOR';
    syncUiForMode();
  });
  btnSwitchView.addEventListener('click', () => {
    if (appMode === 'INSPECTOR') {
      appMode = 'GAME';
      if (gameState === 'INSPECTOR') startNewGame();
    } else {
      appMode = 'INSPECTOR';
    }
    syncUiForMode();
  });
  btnAudioMute.addEventListener('click', () => {
    isMuted = !isMuted;
    btnAudioMute.textContent = isMuted ? '🔇 Muted' : '🔊 SFX';
  });
  btnPauseGame.addEventListener('click', () => {
    if (appMode === 'INSPECTOR') {
      autoRotate = !autoRotate;
      btnPauseGame.textContent = autoRotate ? '⏸' : '▶️';
      return;
    }
    if (gameState === 'PLAYING') {
      gameState = 'PAUSED';
      modalOverlay.classList.remove('hidden');
      modalBadge.textContent = 'SIMULATION PAUSED';
      modalTitle.textContent = 'اللعبة متوقفة مؤقتاً';
      modalDesc.textContent = 'يمكنك استئناف المعركة الخلوية أو التبديل إلى مستعرض الهياكل ثلاثية الأبعاد.';
      btnStartPlay.textContent = '▶️ استئناف اللعب';
    } else if (gameState === 'PAUSED') {
      gameState = 'PLAYING';
      modalOverlay.classList.add('hidden');
    }
  });

  document.querySelectorAll('.model-chip[data-model]').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.model-chip[data-model]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeMolecule = btn.getAttribute('data-model');
      hudModeLabel.textContent = '3D MOLECULAR INSPECTOR • ' + activeMolecule;
      playTone(520, 'sine', 0.1, 680);
    });
  });

  btnExplodeView.addEventListener('click', () => {
    targetExplode = targetExplode > 0.1 ? 0.0 : 1.0;
    btnExplodeView.textContent = targetExplode > 0.1 ? '🔗 دمج الأجزاء' : '💥 تفكيك الأجزاء';
    playTone(400, 'triangle', 0.15, 600);
  });

  btnResetCam.addEventListener('click', () => {
    targetRotX = 0.35;
    targetRotY = 0.65;
    cameraZoom = 1.0;
    targetExplode = 0.0;
  });

  btnSkillFire.addEventListener('click', fireAntibodyBurst);
  btnSkillCrispr.addEventListener('click', triggerCrisprWave);
  btnSkillShield.addEventListener('click', triggerNanoShield);

  // Virtual Joystick Touch/Mouse Logic
  const joyBase = document.getElementById('joystick-base');
  const joyStick = document.getElementById('joystick-stick');
  let joyActive = false;

  function updateJoystick(clientX, clientY) {
    const rect = joyBase.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    let dx = clientX - cx;
    let dy = clientY - cy;
    const maxR = 28;
    const dist = Math.hypot(dx, dy);
    if (dist > maxR) {
      dx = (dx / dist) * maxR;
      dy = (dy / dist) * maxR;
    }
    joyStick.style.transform = 'translate(' + dx + 'px, ' + dy + 'px)';
    joyVector.x = dx / maxR;
    joyVector.y = dy / maxR;
  }

  function resetJoystick() {
    joyActive = false;
    joyStick.style.transform = 'translate(0px, 0px)';
    joyVector.x = 0;
    joyVector.y = 0;
  }

  joyBase.addEventListener('pointerdown', e => {
    joyActive = true;
    joyBase.setPointerCapture(e.pointerId);
    updateJoystick(e.clientX, e.clientY);
  });
  joyBase.addEventListener('pointermove', e => {
    if (joyActive) updateJoystick(e.clientX, e.clientY);
  });
  joyBase.addEventListener('pointerup', resetJoystick);
  joyBase.addEventListener('pointercancel', resetJoystick);

  // Orbit Camera Drag on Canvas
  let draggingCanvas = false;
  let lastPx = 0, lastPy = 0;
  canvas.addEventListener('pointerdown', e => {
    draggingCanvas = true;
    lastPx = e.clientX;
    lastPy = e.clientY;
  });
  window.addEventListener('pointermove', e => {
    if (!draggingCanvas) return;
    const dx = e.clientX - lastPx;
    const dy = e.clientY - lastPy;
    lastPx = e.clientX;
    lastPy = e.clientY;
    targetRotY += dx * 0.009;
    targetRotX = Math.max(-1.2, Math.min(1.2, targetRotX + dy * 0.009));
  });
  window.addEventListener('pointerup', () => { draggingCanvas = false; });

  // Keyboard fallback controls
  window.addEventListener('keydown', e => {
    keys[e.code] = true;
    if (e.code === 'Space') fireAntibodyBurst();
    if (e.code === 'KeyE') triggerCrisprWave();
    if (e.code === 'KeyQ') triggerNanoShield();
  });
  window.addEventListener('keyup', e => { keys[e.code] = false; });

  // 3D Projection Helper with 3-Point Lighting & Atmospheric Depth Fog
  function project3D(x, y, z, cx, cy, fovScale) {
    // Rotate around Y then X
    const cosY = Math.cos(rotY), sinY = Math.sin(rotY);
    const cosX = Math.cos(rotX), sinX = Math.sin(rotX);
    const x1 = x * cosY - z * sinY;
    const z1 = z * cosY + x * sinY;
    const y2 = y * cosX - z1 * sinX;
    const z2 = z1 * cosX + y * sinX;
    const depth = 460 + z2;
    const scale = (fovScale * cameraZoom) / Math.max(80, depth);
    return {
      sx: cx + x1 * scale,
      sy: cy + y2 * scale,
      scale: scale,
      z: z2
    };
  }

  function drawShadedSphere(sx, sy, radius, baseColor, rimColor, depthZ) {
    if (radius <= 0.5) return;
    const fogAlpha = Math.max(0.18, Math.min(1.0, 1.0 - (depthZ + 220) / 720));
    ctx.save();
    ctx.globalAlpha = fogAlpha;
    const grad = ctx.createRadialGradient(
      sx - radius * 0.35, sy - radius * 0.35, radius * 0.1,
      sx, sy, radius
    );
    grad.addColorStop(0, '#FFFFFF');
    grad.addColorStop(0.35, baseColor);
    grad.addColorStop(0.88, '#091024');
    grad.addColorStop(1, rimColor || baseColor);
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(sx, sy, radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }

  function renderInspectorMolecule(cx, cy, timeSec) {
    const nodes = [];
    const links = [];
    const exp = 1 + explodeFactor * 0.55;

    if (activeMolecule === 'DNA_HELIX') {
      const pairs = 14;
      for (let i = 0; i < pairs; i++) {
        const t = i - pairs / 2;
        const ang = i * 0.52 + timeSec * 0.6;
        const y = t * 18 * exp;
        const r = 68 * exp;
        const xA = Math.cos(ang) * r, zA = Math.sin(ang) * r;
        const xB = Math.cos(ang + Math.PI) * r, zB = Math.sin(ang + Math.PI) * r;
        const pA = project3D(xA, y, zA, cx, cy, 420);
        const pB = project3D(xB, y, zB, cx, cy, 420);
        nodes.push({ p: pA, r: 9, color: '#00F5D4', rim: '#8B5CF6' });
        nodes.push({ p: pB, r: 9, color: '#8B5CF6', rim: '#00F5D4' });
        links.push({ a: pA, b: pB, color: i % 2 === 0 ? '#10B981' : '#F59E0B', width: 2.5 });
      }
    } else if (activeMolecule === 'PROTEIN_COMPLEX') {
      // Multi-domain Alpha-Helix + Beta-Sheet + Ligand Pocket
      for (let i = 0; i < 24; i++) {
        const ang = i * 0.45;
        const x = (Math.cos(ang) * 55 + Math.sin(i * 0.25) * 40) * exp;
        const y = ((i - 12) * 10) * exp;
        const z = (Math.sin(ang) * 55) * exp;
        const p = project3D(x, y, z, cx, cy, 420);
        const isPocket = i >= 10 && i <= 14;
        nodes.push({
          p,
          r: isPocket ? 12 : 10,
          color: isPocket ? '#F59E0B' : (i % 2 === 0 ? '#10B981' : '#00F5D4'),
          rim: '#00F5D4'
        });
        if (i > 0) {
          links.push({ a: nodes[i - 1].p, b: p, color: '#10B981', width: 4 });
        }
      }
    } else if (activeMolecule === 'CRISPR_CAS9') {
      // Cas9 Bilobed RNP + sgRNA guide strand + PAM target site
      for (let i = 0; i < 18; i++) {
        const ang = (i / 18) * Math.PI * 2;
        const lobeX = Math.cos(ang) * 85 * exp;
        const lobeY = Math.sin(ang * 2) * 48 * exp;
        const lobeZ = Math.sin(ang) * 65 * exp;
        const p = project3D(lobeX, lobeY, lobeZ, cx, cy, 420);
        nodes.push({ p, r: 13, color: '#8B5CF6', rim: '#00F5D4' });
      }
      // Guide RNA & PAM NGG highlight
      for (let j = -5; j <= 5; j++) {
        const gx = j * 18 * exp;
        const gy = Math.sin(j * 0.6 + timeSec * 2) * 16;
        const p = project3D(gx, gy, 0, cx, cy, 420);
        const isPam = j >= 3;
        nodes.push({
          p,
          r: isPam ? 11 : 8,
          color: isPam ? '#EF4444' : '#00F5D4',
          rim: '#F59E0B'
        });
      }
    } else {
      // VIRAL_NANOBOT Capsid + Spike glycoproteins
      const golden = Math.PI * (3 - Math.sqrt(5));
      for (let i = 0; i < 22; i++) {
        const y = 1 - (i / 21) * 2;
        const radius = Math.sqrt(1 - y * y);
        const theta = golden * i;
        const R = 82 * exp;
        const vx = Math.cos(theta) * radius * R;
        const vy = y * R;
        const vz = Math.sin(theta) * radius * R;
        const pCore = project3D(vx, vy, vz, cx, cy, 420);
        const pSpike = project3D(vx * 1.32, vy * 1.32, vz * 1.32, cx, cy, 420);
        links.push({ a: pCore, b: pSpike, color: '#F59E0B', width: 2.2 });
        nodes.push({ p: pCore, r: 10, color: '#EF4444', rim: '#F59E0B' });
        nodes.push({ p: pSpike, r: 6, color: '#F59E0B', rim: '#00F5D4' });
      }
    }

    // Draw structural bonds
    for (const l of links) {
      ctx.strokeStyle = l.color;
      ctx.lineWidth = l.width;
      ctx.globalAlpha = 0.65;
      ctx.beginPath();
      ctx.moveTo(l.a.sx, l.a.sy);
      ctx.lineTo(l.b.sx, l.b.sy);
      ctx.stroke();
    }
    ctx.globalAlpha = 1.0;

    // Painter's algorithm sort by Z depth
    nodes.sort((a, b) => b.p.z - a.p.z);
    for (const n of nodes) {
      drawShadedSphere(n.p.sx, n.p.sy, n.r * n.p.scale, n.color, n.rim, n.p.z);
    }
  }

  let lastTime = performance.now();
  function animate(now) {
    requestAnimationFrame(animate);
    const dt = Math.min(0.05, (now - lastTime) / 1000);
    lastTime = now;
    const timeSec = now * 0.001;

    if (hitstopFrames > 0) {
      hitstopFrames--;
      return;
    }

    const w = canvas.clientWidth;
    const h = canvas.clientHeight;
    let cx = w / 2;
    let cy = h / 2;

    if (shakeIntensity > 0.2) {
      cx += (Math.random() - 0.5) * shakeIntensity;
      cy += (Math.random() - 0.5) * shakeIntensity;
      shakeIntensity *= 0.85;
    }

    ctx.clearRect(0, 0, w, h);

    // Smooth Damped Camera
    if (autoRotate && !draggingCanvas && appMode === 'INSPECTOR') {
      targetRotY += dt * 0.55;
    }
    rotX += (targetRotX - rotX) * 0.12;
    rotY += (targetRotY - rotY) * 0.12;
    explodeFactor += (targetExplode - explodeFactor) * 0.12;

    // Background Atmospheric Organelles
    for (const org of bgOrganelles) {
      const p = project3D(org.x, org.y, org.z, cx, cy, 380);
      ctx.fillStyle = org.hue;
      ctx.globalAlpha = 0.16;
      ctx.beginPath();
      ctx.arc(p.sx, p.sy, Math.max(1.5, org.r * p.scale), 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1.0;

    if (appMode === 'INSPECTOR') {
      renderInspectorMolecule(cx, cy, timeSec);
      return;
    }

    // GAME MODE UPDATE & RENDER
    if (gameState === 'PLAYING') {
      // Cooldowns
      crisprCd = Math.max(0, crisprCd - dt);
      shieldCd = Math.max(0, shieldCd - dt);
      shieldTimer = Math.max(0, shieldTimer - dt);
      cdCrisprEl.textContent = crisprCd > 0 ? crisprCd.toFixed(1) + 's' : 'READY';
      cdShieldEl.textContent = shieldCd > 0 ? shieldCd.toFixed(1) + 's' : 'READY';

      // Input movement
      let mx = joyVector.x;
      let my = joyVector.y;
      if (keys['ArrowLeft'] || keys['KeyA']) mx -= 1;
      if (keys['ArrowRight'] || keys['KeyD']) mx += 1;
      if (keys['ArrowUp'] || keys['KeyW']) my -= 1;
      if (keys['ArrowDown'] || keys['KeyS']) my += 1;

      player.x = Math.max(-170, Math.min(170, player.x + mx * 3.8));
      player.y = Math.max(-150, Math.min(150, player.y + my * 3.8));

      // Auto-fire antibodies every 0.45s
      fireTimer += dt;
      if (fireTimer >= 0.45 && pathogens.length > 0) {
        fireTimer = 0;
        fireAntibodyBurst();
      }
    }

    // Draw Protected Host Cell Nucleus at Center (0,0,0)
    const coreProj = project3D(0, 0, 0, cx, cy, 420);
    const coreRadius = 34 * coreProj.scale;
    drawShadedSphere(coreProj.sx, coreProj.sy, coreRadius, '#10B981', '#00F5D4', coreProj.z);

    if (shieldTimer > 0) {
      ctx.strokeStyle = '#00F5D4';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(coreProj.sx, coreProj.sy, coreRadius + 14 + Math.sin(timeSec * 8) * 3, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Update & Draw Pathogens with Attack Telegraphs
    let anyTelegraph = false;
    for (let i = pathogens.length - 1; i >= 0; i--) {
      const p = pathogens[i];
      if (gameState === 'PLAYING') {
        const distToCore = Math.max(1, Math.hypot(p.x, p.y));
        p.telegraphTimer--;
        if (p.telegraphTimer <= 30 && p.telegraphTimer > 0) {
          anyTelegraph = true;
        }
        const spdMult = p.telegraphTimer <= 0 ? 2.4 : 1.0;
        if (p.telegraphTimer <= -25) p.telegraphTimer = 110;

        p.x -= (p.x / distToCore) * p.speed * spdMult;
        p.y -= (p.y / distToCore) * p.speed * spdMult;

        // Check collision with Host Nucleus
        if (distToCore < 34) {
          if (shieldTimer <= 0) {
            cellHp = Math.max(0, cellHp - 14);
            combo = 1;
            shakeIntensity = 11;
            playTone(150, 'sawtooth', 0.25, 90);
          } else {
            score += 50;
          }
          spawnParticles(p.x, p.y, p.z, '#EF4444', 16);
          pathogens.splice(i, 1);
          if (cellHp <= 0) {
            gameState = 'GAMEOVER';
            showEndModal(false);
          } else {
            checkWaveProgression();
          }
          continue;
        }
      }

      const proj = project3D(p.x, p.y, p.z, cx, cy, 420);
      const isTelegraphing = p.telegraphTimer <= 30 && p.telegraphTimer > 0;
      const baseCol = isTelegraphing ? '#F59E0B' : '#EF4444';
      drawShadedSphere(proj.sx, proj.sy, p.radius * proj.scale, baseCol, '#8B5CF6', proj.z);

      if (isTelegraphing) {
        ctx.strokeStyle = '#EF4444';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(proj.sx, proj.sy, (p.radius + 8) * proj.scale, 0, Math.PI * 2);
        ctx.stroke();
      }
    }

    if (anyTelegraph && gameState === 'PLAYING') {
      telegraphBanner.classList.remove('hidden');
    } else {
      telegraphBanner.classList.add('hidden');
    }

    // Update & Draw Projectiles
    for (let i = projectiles.length - 1; i >= 0; i--) {
      const b = projectiles[i];
      if (gameState === 'PLAYING') {
        b.x += b.vx;
        b.y += b.vy;
        b.life--;
        if (b.life <= 0) {
          projectiles.splice(i, 1);
          continue;
        }
        // Hit detection
        let hit = false;
        for (let j = pathogens.length - 1; j >= 0; j--) {
          const p = pathogens[j];
          if (Math.hypot(b.x - p.x, b.y - p.y) < p.radius + 7) {
            p.hp--;
            hit = true;
            spawnParticles(p.x, p.y, p.z, '#00F5D4', 6);
            if (p.hp <= 0) {
              pathogens.splice(j, 1);
              score += 100 * combo;
              combo = Math.min(12, combo + 1);
              maxCombo = Math.max(maxCombo, combo);
              hitstopFrames = 2;
              shakeIntensity = 5;
              playTone(620, 'triangle', 0.14, 980);
              spawnParticles(p.x, p.y, p.z, '#F59E0B', 14);
              checkWaveProgression();
            }
            break;
          }
        }
        if (hit) {
          projectiles.splice(i, 1);
          continue;
        }
      }
      const bp = project3D(b.x, b.y, b.z, cx, cy, 420);
      drawShadedSphere(bp.sx, bp.sy, 5 * bp.scale, b.color, '#FFFFFF', bp.z);
    }

    // Draw Player Bio-Nanobot Probe
    const plProj = project3D(player.x, player.y, 18, cx, cy, 420);
    drawShadedSphere(plProj.sx, plProj.sy, 14 * plProj.scale, '#00F5D4', '#8B5CF6', plProj.z);

    // Draw Pooled Particles
    for (let i = particles.length - 1; i >= 0; i--) {
      const pt = particles[i];
      pt.x += pt.vx;
      pt.y += pt.vy;
      pt.z += pt.vz;
      pt.life -= 0.04;
      if (pt.life <= 0) {
        particles.splice(i, 1);
        continue;
      }
      const pp = project3D(pt.x, pt.y, pt.z, cx, cy, 420);
      ctx.fillStyle = pt.color;
      ctx.globalAlpha = Math.max(0, pt.life);
      ctx.beginPath();
      ctx.arc(pp.sx, pp.sy, Math.max(1.5, 4 * pp.scale * pt.life), 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1.0;

    // Sync HUD Numbers
    hudWave.textContent = wave + ' / 3';
    hudScore.textContent = String(score);
    hudCombo.textContent = 'x' + combo;
    hudHpFill.style.width = Math.max(0, cellHp) + '%';
    hudHpText.textContent = Math.max(0, Math.round(cellHp)) + '%';
  }

  syncUiForMode();
  requestAnimationFrame(animate);
})();
""".trimIndent()

    val THREEJS_GAME_BACKEND: String = """
/**
 * NovaForge 3D BioGame & Molecular Telemetry Backend Service (TypeScript / Node.js)
 * Persists high scores, CRISPR PAM efficiency logs, and 3D molecular snapshots.
 */
export interface BioDefenseSessionLog {
  sessionId: string;
  userId: string;
  waveReached: number;
  finalScore: number;
  maxCombo: number;
  cellIntegrityPct: number;
  activeMoleculePreset: 'DNA_HELIX' | 'PROTEIN_COMPLEX' | 'CRISPR_CAS9' | 'VIRAL_NANOBOT';
  timestampIso: string;
}

export class BioSimulationTelemetryController {
  private sessions: BioDefenseSessionLog[] = [];

  public recordGameSession(payload: Omit<BioDefenseSessionLog, 'timestampIso'>): BioDefenseSessionLog {
    const entry: BioDefenseSessionLog = {
      ...payload,
      timestampIso: new Date().toISOString(),
    };
    this.sessions.push(entry);
    return entry;
  }

  public getLeaderboard(limit = 10): BioDefenseSessionLog[] {
    return [...this.sessions]
      .sort((a, b) => b.finalScore - a.finalScore)
      .slice(0, limit);
  }
}
""".trimIndent()

    fun buildInteractive3dBioGameHtml(
        initialMode: String = "GAME",
        initialMolecule: String = "DNA_HELIX"
    ): String {
        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>NovaForge 3D BioGame & Molecular Inspector</title>
  <style>
$THREEJS_GAME_CSS
  </style>
</head>
<body>
$THREEJS_GAME_HTML
  <script>
    window.INITIAL_BIO3D_MODE = "$initialMode";
    window.INITIAL_BIO3D_MOLECULE = "$initialMolecule";
$THREEJS_GAME_JS
  </script>
</body>
</html>
""".trimIndent()
    }
}
