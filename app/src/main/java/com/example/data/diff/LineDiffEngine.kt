package com.example.data.diff

import com.example.data.model.DiffLine
import com.example.data.model.DiffType

/**
 * Bounded LCS diff for editor snapshots. It preserves alignment after insertions
 * and deletions while keeping the UI-thread cost predictable.
 */
object LineDiffEngine {
    fun compute(
        oldLines: List<String>,
        newLines: List<String>,
        maxLinesPerSide: Int = 300
    ): List<DiffLine> {
        require(maxLinesPerSide > 0) { "maxLinesPerSide must be greater than zero." }

        val old = oldLines.take(maxLinesPerSide)
        val new = newLines.take(maxLinesPerSide)
        val oldCount = old.size
        val newCount = new.size

        val lcs = Array(oldCount + 1) { IntArray(newCount + 1) }
        for (i in oldCount - 1 downTo 0) {
            for (j in newCount - 1 downTo 0) {
                lcs[i][j] = if (old[i] == new[j]) {
                    lcs[i + 1][j + 1] + 1
                } else {
                    maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        val diff = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < oldCount || j < newCount) {
            when {
                i < oldCount && j < newCount && old[i] == new[j] -> {
                    diff += DiffLine(i + 1, "  ${old[i]}", DiffType.UNCHANGED)
                    i++
                    j++
                }
                i < oldCount && (j >= newCount || lcs[i + 1][j] >= lcs[i][j + 1]) -> {
                    diff += DiffLine(i + 1, "- ${old[i]}", DiffType.DELETED)
                    i++
                }
                j < newCount -> {
                    diff += DiffLine(j + 1, "+ ${new[j]}", DiffType.ADDED)
                    j++
                }
            }
        }

        if (oldLines.size > maxLinesPerSide || newLines.size > maxLinesPerSide) {
            diff += DiffLine(
                maxLinesPerSide + 1,
                "Diff limited to the first $maxLinesPerSide lines of each version.",
                DiffType.UNCHANGED
            )
        }
        return diff
    }
}
