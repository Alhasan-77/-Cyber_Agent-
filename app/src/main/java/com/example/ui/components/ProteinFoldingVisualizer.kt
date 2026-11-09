package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.skills.BioSequenceMetrics
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class FoldingVisualizationMode(val id: String, val titleAr: String) {
    BACKBONE_3D("BACKBONE_3D", "الهيكل ثلاثي الأبعاد 3D"),
    RAMACHANDRAN("RAMACHANDRAN", "مخطط راماشاندران (φ/ψ)"),
    CONTACT_ENERGY("CONTACT_ENERGY", "خريطة التلامس التوضيحية")
}

enum class FoldingColorScheme(val id: String, val titleAr: String) {
    SECONDARY_STRUCTURE("STRUCTURE", "التركيب التوضيحي"),
    HYDROPATHY("HYDROPATHY", "الكراهية للماء GRAVY")
}

enum class SecondaryStructureKind(val labelAr: String, val shortCode: String, val color: Color) {
    ALPHA_HELIX("حلزون ألفا (α-Helix)", "H", CyberCyan),
    BETA_SHEET("صفيحة بيتا (β-Sheet)", "E", CyberViolet),
    RANDOM_COIL("حلقة مرنة (Coil/Loop)", "C", CyberAmber)
}

data class ResidueFoldingNode(
    val indexOneBased: Int,
    val oneLetterCode: Char,
    val threeLetterName: String,
    val structureKind: SecondaryStructureKind,
    val plddtScore: Double,
    val hydropathy: Double,
    val nativePhi: Float,
    val nativePsi: Float,
    val nativeX: Float,
    val nativeY: Float,
    val nativeZ: Float,
    val unfoldedX: Float,
    val unfoldedY: Float,
    val unfoldedZ: Float,
    val solventAccessibilityPct: Int,
    val isBindingPocket: Boolean
)

data class ResidueContactPair(
    val residueAIndex: Int, // 0-based
    val residueBIndex: Int, // 0-based
    val distanceAngstroms: Float,
    val isHydrogenBond: Boolean
)

data class SimulatedProteinStructure(
    val residues: List<ResidueFoldingNode>,
    val contacts: List<ResidueContactPair>,
    val nativeRadiusOfGyration: Float,
    val unfoldedRadiusOfGyration: Float
)

private val aminoAcidThreeLetterMap = mapOf(
    'A' to "Ala", 'R' to "Arg", 'N' to "Asn", 'D' to "Asp",
    'C' to "Cys", 'E' to "Glu", 'Q' to "Gln", 'G' to "Gly",
    'H' to "His", 'I' to "Ile", 'L' to "Leu", 'K' to "Lys",
    'M' to "Met", 'F' to "Phe", 'P' to "Pro", 'S' to "Ser",
    'T' to "Thr", 'W' to "Trp", 'Y' to "Tyr", 'V' to "Val"
)

private val residueHydropathyMap = mapOf(
    'I' to 4.5, 'V' to 4.2, 'L' to 3.8, 'F' to 2.8, 'C' to 2.5,
    'M' to 1.9, 'A' to 1.8, 'G' to -0.4, 'T' to -0.7, 'S' to -0.8,
    'W' to -0.9, 'Y' to -1.3, 'P' to -1.6, 'H' to -3.2, 'E' to -3.5,
    'Q' to -3.5, 'D' to -3.5, 'N' to -3.5, 'K' to -3.9, 'R' to -4.5
)

fun buildSimulatedProteinStructure(metrics: BioSequenceMetrics): SimulatedProteinStructure {
    val rawProtein = metrics.translatedProteinPreview
        .uppercase(Locale.US)
        .filter { it in 'A'..'Z' }
        .take(72)
    require(rawProtein.isNotBlank()) {
        "لا يوجد تسلسل بروتيني لبناء العرض التوضيحي. أدخل تسلسلاً بروتينياً صالحاً."
    }

    val n = rawProtein.length
    val nodes = ArrayList<ResidueFoldingNode>(n)

    // Assign secondary structure in realistic contiguous segments based on sequence propensities
    val rawKinds = Array(n) { idx ->
        val windowStart = max(0, idx - 2)
        val windowEnd = min(n - 1, idx + 2)
        val window = rawProtein.substring(windowStart..windowEnd)
        val helixScore = window.count { it in "EAQLMKRH" }
        val sheetScore = window.count { it in "VIFYWTC" }
        val turnBreak = rawProtein[idx] in "PGDSN"
        when {
            turnBreak && idx % 5 == 0 -> SecondaryStructureKind.RANDOM_COIL
            helixScore >= sheetScore + 1 && helixScore >= 2 -> SecondaryStructureKind.ALPHA_HELIX
            sheetScore >= 2 -> SecondaryStructureKind.BETA_SHEET
            idx % 11 in 0..4 -> SecondaryStructureKind.ALPHA_HELIX
            idx % 11 in 6..8 -> SecondaryStructureKind.BETA_SHEET
            else -> SecondaryStructureKind.RANDOM_COIL
        }
    }

    var curX = 0f
    var curY = 0f
    var curZ = 0f
    var strandDirection = 1f

    for (i in 0 until n) {
        val aa = rawProtein[i]
        val kind = rawKinds[i]
        val hydro = residueHydropathyMap[aa] ?: -0.3

        // Flip direction at coil boundaries to create a compact globular 3D fold
        if (i > 0 && kind != rawKinds[i - 1] && kind == SecondaryStructureKind.BETA_SHEET) {
            strandDirection *= -1f
        }

        val t = i.toFloat()
        when (kind) {
            SecondaryStructureKind.ALPHA_HELIX -> {
                val angle = t * (100f * PI.toFloat() / 180f)
                val domainFold = sin(t * 0.14f) * 4.2f
                curX += cos(angle) * 2.1f + 0.45f * cos(t * 0.12f)
                curY += sin(angle) * 2.1f + domainFold * 0.18f
                curZ += 1.45f * cos(t * 0.18f)
            }
            SecondaryStructureKind.BETA_SHEET -> {
                val pleat = if (i % 2 == 0) 0.85f else -0.85f
                curX += 2.6f * strandDirection
                curY += pleat + 0.55f * sin(t * 0.25f)
                curZ += 1.25f * cos(t * 0.22f)
            }
            SecondaryStructureKind.RANDOM_COIL -> {
                curX += 1.9f * cos(t * 0.65f)
                curY += 1.9f * sin(t * 0.52f)
                curZ += 1.8f * sin(t * 0.41f)
            }
        }

        // Unfolded denatured polypeptide coordinates (extended flexible chain)
        val unfoldedX = (i - n / 2f) * 2.85f
        val unfoldedY = sin(i * 0.55f) * 3.2f
        val unfoldedZ = cos(i * 0.45f) * 2.6f

        val (phi, psi) = when (kind) {
            SecondaryStructureKind.ALPHA_HELIX -> {
                (-62f + (sin(i * 1.1f) * 9f)) to (-43f + (cos(i * 0.9f) * 8f))
            }
            SecondaryStructureKind.BETA_SHEET -> {
                (-124f + (cos(i * 1.3f) * 14f)) to (132f + (sin(i * 1.1f) * 14f))
            }
            SecondaryStructureKind.RANDOM_COIL -> {
                val basePhi = if (aa == 'G') 78f else -76f + sin(i * 1.7f) * 45f
                val basePsi = 35f + cos(i * 1.5f) * 85f
                basePhi to basePsi
            }
        }

        val solventAcc = (48 - (hydro * 7.5) + (if (kind == SecondaryStructureKind.RANDOM_COIL) 18 else -6))
            .roundToInt()
            .coerceIn(8, 94)

        val isPocket = (aa in "HDECYFW" && hydro > -3.6 && i in (n / 5)..(4 * n / 5) && i % 6 == 0)

        nodes.add(
            ResidueFoldingNode(
                indexOneBased = i + 1,
                oneLetterCode = aa,
                threeLetterName = aminoAcidThreeLetterMap[aa] ?: "Xaa",
                structureKind = kind,
                plddtScore = 0.0,
                hydropathy = hydro,
                nativePhi = phi,
                nativePsi = psi,
                nativeX = curX,
                nativeY = curY,
                nativeZ = curZ,
                unfoldedX = unfoldedX,
                unfoldedY = unfoldedY,
                unfoldedZ = unfoldedZ,
                solventAccessibilityPct = solventAcc,
                isBindingPocket = isPocket
            )
        )
    }

    // Center native coordinates around origin (0, 0, 0)
    val meanX = nodes.map { it.nativeX }.average().toFloat()
    val meanY = nodes.map { it.nativeY }.average().toFloat()
    val meanZ = nodes.map { it.nativeZ }.average().toFloat()

    val centeredNodes = nodes.map { node ->
        node.copy(
            nativeX = node.nativeX - meanX,
            nativeY = node.nativeY - meanY,
            nativeZ = node.nativeZ - meanZ
        )
    }

    // Compute intramolecular contacts (residues separated by >= 3 positions along sequence)
    val contacts = mutableListOf<ResidueContactPair>()
    for (i in centeredNodes.indices) {
        for (j in i + 3 until centeredNodes.size) {
            val a = centeredNodes[i]
            val b = centeredNodes[j]
            val dx = a.nativeX - b.nativeX
            val dy = a.nativeY - b.nativeY
            val dz = a.nativeZ - b.nativeZ
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            val isHelixHBond = (j == i + 4 && a.structureKind == SecondaryStructureKind.ALPHA_HELIX && b.structureKind == SecondaryStructureKind.ALPHA_HELIX)
            if (dist < 8.8f || isHelixHBond) {
                contacts.add(
                    ResidueContactPair(
                        residueAIndex = i,
                        residueBIndex = j,
                        distanceAngstroms = (dist.coerceIn(3.6f, 9.2f) * 10f).roundToInt() / 10f,
                        isHydrogenBond = isHelixHBond || dist < 5.6f
                    )
                )
            }
        }
    }

    val nativeRg = sqrt(
        centeredNodes.map { it.nativeX * it.nativeX + it.nativeY * it.nativeY + it.nativeZ * it.nativeZ }
            .average()
            .toFloat()
    ).coerceAtLeast(8.5f)
    val unfoldedRg = (nativeRg * 2.15f)

    return SimulatedProteinStructure(
        residues = centeredNodes,
        contacts = contacts,
        nativeRadiusOfGyration = (nativeRg * 10f).roundToInt() / 10f,
        unfoldedRadiusOfGyration = (unfoldedRg * 10f).roundToInt() / 10f
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProteinFoldingVisualizer(
    metrics: BioSequenceMetrics,
    modifier: Modifier = Modifier
) {
    val structure = remember(metrics.translatedProteinPreview, metrics.plddtConfidence) {
        buildSimulatedProteinStructure(metrics)
    }

    var viewMode by remember { mutableStateOf(FoldingVisualizationMode.BACKBONE_3D) }
    var colorScheme by remember { mutableStateOf(FoldingColorScheme.SECONDARY_STRUCTURE) }
    var foldingProgress by remember { mutableFloatStateOf(1.0f) } // 0f = denatured, 1f = native folded
    var selectedResidueIndex by remember(structure.residues.size) {
        mutableIntStateOf(0)
    }
    var autoRotate by remember { mutableStateOf(true) }
    var rotationYaw by remember { mutableFloatStateOf(28f) }
    var rotationPitch by remember { mutableFloatStateOf(16f) }

    val infiniteTransition = rememberInfiniteTransition(label = "protein_folding_orbit")
    val animatedSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_spin"
    )

    val effectiveYawDeg = if (autoRotate) (rotationYaw + animatedSpinDeg) % 360f else rotationYaw
    val safeSelectedIdx = selectedResidueIndex.coerceIn(0, max(0, structure.residues.lastIndex))
    val selectedNode = structure.residues.getOrNull(safeSelectedIdx)

    // Live folding thermodynamics derived from foldingProgress
    val currentRmsd = remember(foldingProgress) {
        (((1f - foldingProgress) * 14.8f + 0.45f) * 100f).roundToInt() / 100f
    }
    val currentRg = remember(foldingProgress, structure) {
        val rg = structure.unfoldedRadiusOfGyration +
            (structure.nativeRadiusOfGyration - structure.unfoldedRadiusOfGyration) * foldingProgress
        (rg * 10f).roundToInt() / 10f
    }
    val currentDeltaG = remember(foldingProgress, metrics.bindingAffinityKcal) {
        val unfoldedG = 4.2
        val nativeG = metrics.bindingAffinityKcal
        val g = unfoldedG + (nativeG - unfoldedG) * foldingProgress.toDouble()
        (g * 10.0).roundToInt() / 10.0
    }
    val activeContactsCount = remember(foldingProgress, structure.contacts.size) {
        (structure.contacts.size * (foldingProgress * foldingProgress)).roundToInt()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("protein_folding_visualizer_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🧊 عارض توضيحي ثلاثي الأبعاد لتسلسل البروتين",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تصور هندسي تعليمي مبني على قواعد مبسطة؛ لا يمثل تنبؤاً بنيوياً أو قياسات طاقة/ارتباط حقيقية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { autoRotate = !autoRotate },
                        modifier = Modifier.testTag("folding_animate_button")
                    ) {
                        Icon(
                            imageVector = if (autoRotate) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (autoRotate) "إيقاف التدوير التلقائي" else "تشغيل التدوير التلقائي",
                            tint = CyberEmerald
                        )
                    }
                    IconButton(
                        onClick = {
                            rotationYaw = 28f
                            rotationPitch = 16f
                            foldingProgress = 1.0f
                        },
                        modifier = Modifier.testTag("folding_reset_view_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "إعادة ضبط زاوية العرض",
                            tint = CyberCyan
                        )
                    }
                }
            }

            // Visualization Mode Switcher
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FoldingVisualizationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                            selectedLabelColor = CyberCyan
                        ),
                        label = { Text(mode.titleAr) },
                        modifier = Modifier.testTag("folding_view_${mode.id}")
                    )
                }
            }

            // Color Scheme Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "نمط التلوين:",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberEmerald
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FoldingColorScheme.entries.forEach { scheme ->
                        FilterChip(
                            selected = colorScheme == scheme,
                            onClick = { colorScheme = scheme },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberViolet.copy(alpha = 0.25f),
                                selectedLabelColor = CyberEmerald
                            ),
                            label = {
                                Text(
                                    text = scheme.titleAr,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("folding_color_${scheme.id}")
                        )
                    }
                }
            }

            // Interactive Canvas Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberBackground)
                    .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .testTag("protein_folding_canvas_container")
            ) {
                when (viewMode) {
                    FoldingVisualizationMode.BACKBONE_3D -> {
                        Backbone3dCanvas(
                            structure = structure,
                            foldingProgress = foldingProgress,
                            yawDeg = effectiveYawDeg,
                            pitchDeg = rotationPitch,
                            colorScheme = colorScheme,
                            selectedResidueIndex = safeSelectedIdx,
                            onRotateDrag = { dx, dy ->
                                autoRotate = false
                                rotationYaw = (rotationYaw + dx * 0.45f) % 360f
                                rotationPitch = (rotationPitch + dy * 0.35f).coerceIn(-75f, 75f)
                            }
                        )
                    }
                    FoldingVisualizationMode.RAMACHANDRAN -> {
                        RamachandranPlotCanvas(
                            structure = structure,
                            foldingProgress = foldingProgress,
                            colorScheme = colorScheme,
                            selectedResidueIndex = safeSelectedIdx,
                            onSelectResidue = { selectedResidueIndex = it }
                        )
                    }
                    FoldingVisualizationMode.CONTACT_ENERGY -> {
                        ContactMapAndEnergyCanvas(
                            structure = structure,
                            foldingProgress = foldingProgress,
                            selectedResidueIndex = safeSelectedIdx,
                            onSelectResidue = { selectedResidueIndex = it }
                        )
                    }
                }

                // Overlay HUD Legend inside Canvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = CyberCyan, text = "α-Helix (توضيحي)")
                    LegendDot(color = CyberViolet, text = "β-Sheet (توضيحي)")
                    LegendDot(color = CyberAmber, text = "Coil (توضيحي)")
                }

                Text(
                    text = "الإحداثيات والتلامسات المعروضة توضيحية؛ لا تمثل RMSD أو Rg أو ΔG محسوبة.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = CyberEmerald,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurface.copy(alpha = 0.88f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Folding Trajectory Progress Slider (Unfolded Coil -> Native Folded Conformation)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تقدم الحركة التوضيحية: ${(foldingProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "التلامسات المرسومة: $activeContactsCount / ${structure.contacts.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = CyberEmerald
                    )
                }
                Slider(
                    value = foldingProgress,
                    onValueChange = { foldingProgress = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberEmerald,
                        inactiveTrackColor = CyberSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folding_progress_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0% الشكل المفكوك في العرض",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "100% الشكل المرسوم في العرض",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberEmerald
                    )
                }
            }

            HorizontalDivider(color = CyberCyan.copy(alpha = 0.2f))

            // Interactive Sequence Track & Per-Residue Structural Inspector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBackground)
                    .padding(12.dp)
                    .testTag("folding_residue_inspector"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 فاحص الأحماض الأمينية الموضعي (${structure.residues.size} حمض أميني)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (selectedResidueIndex > 0) selectedResidueIndex--
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("folding_prev_residue_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "الحمض الأميني السابق",
                                tint = CyberCyan
                            )
                        }
                        Text(
                            text = "#${safeSelectedIdx + 1}/${structure.residues.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = CyberCyan
                        )
                        IconButton(
                            onClick = {
                                if (selectedResidueIndex < structure.residues.lastIndex) selectedResidueIndex++
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("folding_next_residue_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "الحمض الأميني التالي",
                                tint = CyberCyan
                            )
                        }
                    }
                }

                // Scrollable Sequence Ribbon Track (LTR for standard N->C sequence order)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        structure.residues.forEachIndexed { idx, node ->
                            val isSelected = idx == safeSelectedIdx
                            val chipColor = resolveResidueColor(node, colorScheme)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .widthIn(min = 34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) chipColor.copy(alpha = 0.28f)
                                        else CyberSurface
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) chipColor else chipColor.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedResidueIndex = idx }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("folding_residue_chip_${node.indexOneBased}")
                            ) {
                                Text(
                                    text = node.oneLetterCode.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Text(
                                    text = "${node.indexOneBased}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Selected Residue Detail Panel
                if (selectedNode != null) {
                    val residueContacts = structure.contacts.count {
                        it.residueAIndex == safeSelectedIdx || it.residueBIndex == safeSelectedIdx
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurface)
                            .padding(10.dp)
                            .testTag("folding_selected_residue_detail"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "الحمض الأميني: ${selectedNode.threeLetterName}-${selectedNode.indexOneBased} (${selectedNode.oneLetterCode}) • ${selectedNode.structureKind.labelAr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = selectedNode.structureKind.color,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "زوايا φ/ψ والتلامسات المعروضة اصطناعية لأغراض التوضيح: $residueContacts تلامسات",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مؤشر الكراهية للماء: ${selectedNode.hydropathy} • التعرض للمذيب (تقدير توضيحي): ${selectedNode.solventAccessibilityPct}% " +
                                    if (selectedNode.isBindingPocket) "• موضع مرشح بصري غير مثبت" else "• لا يوجد موضع مرشح بصري",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedNode.isBindingPocket) CyberAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "pLDDT غير متاح دون تشغيل نموذج تنبؤ بنيوي متخصص.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun resolveResidueColor(
    node: ResidueFoldingNode,
    scheme: FoldingColorScheme
): Color {
    return when (scheme) {
        FoldingColorScheme.SECONDARY_STRUCTURE -> node.structureKind.color
        FoldingColorScheme.HYDROPATHY -> when {
            node.hydropathy >= 1.5 -> CyberAmber
            node.hydropathy >= -0.8 -> CyberEmerald
            else -> CyberCyan
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Backbone3dCanvas(
    structure: SimulatedProteinStructure,
    foldingProgress: Float,
    yawDeg: Float,
    pitchDeg: Float,
    colorScheme: FoldingColorScheme,
    selectedResidueIndex: Int,
    onRotateDrag: (Float, Float) -> Unit
) {
    val yawRad = (yawDeg * PI / 180.0).toFloat()
    val pitchRad = (pitchDeg * PI / 180.0).toFloat()
    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    val cosP = cos(pitchRad)
    val sinP = sin(pitchRad)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onRotateDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("folding_3d_backbone_canvas")
    ) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val centerY = h * 0.52f
        val scale = min(w, h) * 0.022f

        // Subtle molecular coordinate grid
        val gridColor = CyberCyan.copy(alpha = 0.07f)
        for (i in 1..5) {
            val y = h * (i / 6f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            val x = w * (i / 6f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }

        // Compute interpolated and 3D-rotated coordinates for each residue
        val projectedPoints = structure.residues.map { node ->
            val x = node.unfoldedX + (node.nativeX - node.unfoldedX) * foldingProgress
            val y = node.unfoldedY + (node.nativeY - node.unfoldedY) * foldingProgress
            val z = node.unfoldedZ + (node.nativeZ - node.unfoldedZ) * foldingProgress

            // Rotate around Y axis (yaw) then X axis (pitch)
            val rx = x * cosY + z * sinY
            val rz1 = -x * sinY + z * cosY
            val ry = y * cosP - rz1 * sinP
            val rz = y * sinP + rz1 * cosP

            val perspective = (42f / (42f - rz.coerceIn(-24f, 24f))).coerceIn(0.65f, 1.55f)
            val screenX = centerX + rx * scale * perspective
            val screenY = centerY + ry * scale * perspective
            Triple(Offset(screenX, screenY), rz, perspective)
        }

        // Draw intramolecular hydrogen bonds & contacts when folded
        if (foldingProgress > 0.25f) {
            val contactAlpha = ((foldingProgress - 0.25f) / 0.75f).coerceIn(0f, 1f) * 0.45f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            structure.contacts.forEach { contact ->
                val pA = projectedPoints.getOrNull(contact.residueAIndex)?.first
                val pB = projectedPoints.getOrNull(contact.residueBIndex)?.first
                if (pA != null && pB != null) {
                    drawLine(
                        color = if (contact.isHydrogenBond) CyberEmerald.copy(alpha = contactAlpha)
                        else CyberViolet.copy(alpha = contactAlpha * 0.7f),
                        start = pA,
                        end = pB,
                        strokeWidth = if (contact.isHydrogenBond) 2f else 1.2f,
                        pathEffect = dashEffect
                    )
                }
            }
        }

        // Draw C-alpha polypeptide backbone segments
        for (i in 0 until projectedPoints.size - 1) {
            val (ptA, _, perspA) = projectedPoints[i]
            val (ptB, _, perspB) = projectedPoints[i + 1]
            val node = structure.residues[i]
            val segColor = resolveResidueColor(node, colorScheme)
            val widthFactor = when (node.structureKind) {
                SecondaryStructureKind.ALPHA_HELIX -> 6.5f
                SecondaryStructureKind.BETA_SHEET -> 5.8f
                SecondaryStructureKind.RANDOM_COIL -> 3.0f
            } * ((perspA + perspB) * 0.5f)

            drawLine(
                color = segColor.copy(alpha = 0.88f),
                start = ptA,
                end = ptB,
                strokeWidth = widthFactor,
                cap = StrokeCap.Round
            )
        }

        // Draw residue spheres sorted back-to-front by Z
        val sortedIndices = projectedPoints.indices.sortedBy { projectedPoints[it].second }
        for (idx in sortedIndices) {
            val (pt, _, persp) = projectedPoints[idx]
            val node = structure.residues[idx]
            val nodeColor = resolveResidueColor(node, colorScheme)
            val isSelected = idx == selectedResidueIndex

            if (node.isBindingPocket) {
                drawCircle(
                    color = CyberAmber.copy(alpha = 0.25f),
                    radius = 12f * persp,
                    center = pt
                )
            }

            if (isSelected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 15f * persp,
                    center = pt
                )
                drawCircle(
                    color = CyberCyan,
                    radius = 11f * persp,
                    center = pt,
                    style = Stroke(width = 2.4f)
                )
            }

            drawCircle(
                color = nodeColor,
                radius = (if (isSelected) 6.5f else 4.5f) * persp,
                center = pt
            )
        }
    }
}

@Composable
private fun RamachandranPlotCanvas(
    structure: SimulatedProteinStructure,
    foldingProgress: Float,
    colorScheme: FoldingColorScheme,
    selectedResidueIndex: Int,
    onSelectResidue: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(structure, foldingProgress) {
                detectTapGestures { tapOffset ->
                    val pad = 28f
                    val plotW = (size.width - pad * 2f).coerceAtLeast(1f)
                    val plotH = (size.height - pad * 2f).coerceAtLeast(1f)
                    var bestIdx = selectedResidueIndex
                    var bestDist = Float.MAX_VALUE
                    structure.residues.forEachIndexed { idx, node ->
                        val phi = node.nativePhi * foldingProgress
                        val psi = node.nativePsi * foldingProgress
                        val px = pad + ((phi + 180f) / 360f) * plotW
                        val py = pad + ((180f - psi) / 360f) * plotH
                        val d = hypot(tapOffset.x - px, tapOffset.y - py)
                        if (d < bestDist) {
                            bestDist = d
                            bestIdx = idx
                        }
                    }
                    onSelectResidue(bestIdx)
                }
            }
            .testTag("folding_ramachandran_canvas")
    ) {
        val pad = 28f
        val plotW = (size.width - pad * 2f).coerceAtLeast(1f)
        val plotH = (size.height - pad * 2f).coerceAtLeast(1f)

        fun degToOffset(phi: Float, psi: Float): Offset {
            val x = pad + ((phi.coerceIn(-180f, 180f) + 180f) / 360f) * plotW
            val y = pad + ((180f - psi.coerceIn(-180f, 180f)) / 360f) * plotH
            return Offset(x, y)
        }

        // Draw favored conformational quadrants (Beta-sheet top-left, Right-handed Alpha-helix mid-left)
        val betaTopLeft = degToOffset(-165f, 170f)
        val betaBottomRight = degToOffset(-55f, 85f)
        drawRect(
            color = CyberViolet.copy(alpha = 0.16f),
            topLeft = betaTopLeft,
            size = Size(betaBottomRight.x - betaTopLeft.x, betaBottomRight.y - betaTopLeft.y)
        )

        val alphaTopLeft = degToOffset(-140f, -10f)
        val alphaBottomRight = degToOffset(-40f, -75f)
        drawRect(
            color = CyberCyan.copy(alpha = 0.16f),
            topLeft = alphaTopLeft,
            size = Size(alphaBottomRight.x - alphaTopLeft.x, alphaBottomRight.y - alphaTopLeft.y)
        )

        // Zero axes (Phi = 0, Psi = 0)
        val center = degToOffset(0f, 0f)
        drawLine(
            color = CyberCyan.copy(alpha = 0.25f),
            start = Offset(pad, center.y),
            end = Offset(pad + plotW, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = CyberCyan.copy(alpha = 0.25f),
            start = Offset(center.x, pad),
            end = Offset(center.x, pad + plotH),
            strokeWidth = 1.5f
        )

        // Plot each residue's (Phi, Psi)
        structure.residues.forEachIndexed { idx, node ->
            val phi = node.nativePhi * foldingProgress + (1f - foldingProgress) * sin(idx * 1.3f) * 140f
            val psi = node.nativePsi * foldingProgress + (1f - foldingProgress) * cos(idx * 1.1f) * 140f
            val pt = degToOffset(phi, psi)
            val color = resolveResidueColor(node, colorScheme)
            val isSelected = idx == selectedResidueIndex

            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = pt,
                    style = Stroke(width = 2.2f)
                )
            }
            drawCircle(
                color = color,
                radius = if (isSelected) 6.5f else 4.2f,
                center = pt
            )
        }
    }
}

@Composable
private fun ContactMapAndEnergyCanvas(
    structure: SimulatedProteinStructure,
    foldingProgress: Float,
    selectedResidueIndex: Int,
    onSelectResidue: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(structure) {
                detectTapGestures { tapOffset ->
                    val n = max(1, structure.residues.size)
                    val ratio = (tapOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 0.999f)
                    onSelectResidue((ratio * n).toInt().coerceIn(0, n - 1))
                }
            }
            .testTag("folding_contact_energy_canvas")
    ) {
        val w = size.width
        val h = size.height
        val n = max(1, structure.residues.size)

        // Top half: Per-Residue pLDDT confidence & folding stability profile
        val chartHeight = h * 0.45f
        val chartTop = 24f
        val path = Path()

        structure.residues.forEachIndexed { idx, node ->
            val x = (idx.toFloat() / max(1, n - 1)) * (w - 32f) + 16f
            val effectivePlddt = 45.0 + (node.plddtScore - 45.0) * foldingProgress
            val normalized = ((effectivePlddt - 40.0) / 60.0).toFloat().coerceIn(0f, 1f)
            val y = chartTop + chartHeight * (1f - normalized)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)

            if (idx == selectedResidueIndex) {
                drawLine(
                    color = CyberCyan.copy(alpha = 0.5f),
                    start = Offset(x, chartTop),
                    end = Offset(x, h - 16f),
                    strokeWidth = 1.8f
                )
                drawCircle(
                    color = CyberEmerald,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }

        drawPath(
            path = path,
            color = CyberEmerald,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Bottom half: Intramolecular Contact Arc Diagram (H-bonds & Tertiary Contacts)
        val baselineY = h * 0.58f
        drawLine(
            color = CyberCyan.copy(alpha = 0.35f),
            start = Offset(16f, baselineY),
            end = Offset(w - 16f, baselineY),
            strokeWidth = 2f
        )

        structure.contacts.forEach { contact ->
            val xA = (contact.residueAIndex.toFloat() / max(1, n - 1)) * (w - 32f) + 16f
            val xB = (contact.residueBIndex.toFloat() / max(1, n - 1)) * (w - 32f) + 16f
            val midX = (xA + xB) * 0.5f
            val span = abs(xB - xA)
            val arcDepth = min(h * 0.36f, span * 0.45f) * foldingProgress
            val isTouchingSelected = contact.residueAIndex == selectedResidueIndex ||
                contact.residueBIndex == selectedResidueIndex

            val arcPath = Path().apply {
                moveTo(xA, baselineY)
                quadraticTo(midX, baselineY + arcDepth, xB, baselineY)
            }
            drawPath(
                path = arcPath,
                color = when {
                    isTouchingSelected -> CyberAmber
                    contact.isHydrogenBond -> CyberCyan.copy(alpha = 0.55f)
                    else -> CyberViolet.copy(alpha = 0.4f)
                },
                style = Stroke(width = if (isTouchingSelected) 2.6f else 1.4f)
            )
        }
    }
}
