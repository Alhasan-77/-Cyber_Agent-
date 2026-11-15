package com.example.data.diff

import com.example.data.model.DiffType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineDiffEngineTest {
    @Test
    fun insertionKeepsFollowingLinesAligned() {
        val result = LineDiffEngine.compute(
            oldLines = listOf("A", "B", "C"),
            newLines = listOf("A", "X", "B", "C")
        )

        assertEquals(
            listOf(
                DiffType.UNCHANGED,
                DiffType.ADDED,
                DiffType.UNCHANGED,
                DiffType.UNCHANGED
            ),
            result.map { it.type }
        )
        assertEquals("  B", result[2].content)
        assertEquals("  C", result[3].content)
    }

    @Test
    fun deletionDoesNotMarkLaterLinesAsReplacements() {
        val result = LineDiffEngine.compute(
            oldLines = listOf("A", "B", "C"),
            newLines = listOf("A", "C")
        )

        assertEquals(
            listOf(DiffType.UNCHANGED, DiffType.DELETED, DiffType.UNCHANGED),
            result.map { it.type }
        )
    }

    @Test
    fun duplicateLinesAreComparedByOccurrenceCount() {
        val result = LineDiffEngine.compute(
            oldLines = listOf("A", "A", "B"),
            newLines = listOf("A", "B")
        )

        assertEquals(1, result.count { it.type == DiffType.DELETED })
        assertEquals(0, result.count { it.type == DiffType.ADDED })
        assertEquals(2, result.count { it.type == DiffType.UNCHANGED })
    }

    @Test
    fun largeDiffIsBoundedAndClearlyMarked() {
        val result = LineDiffEngine.compute(
            oldLines = (1..8).map { "old-$it" },
            newLines = (1..8).map { "new-$it" },
            maxLinesPerSide = 5
        )

        assertTrue(result.last().content.contains("limited to the first 5 lines"))
        assertEquals(6, result.last().lineNumber)
    }
}
