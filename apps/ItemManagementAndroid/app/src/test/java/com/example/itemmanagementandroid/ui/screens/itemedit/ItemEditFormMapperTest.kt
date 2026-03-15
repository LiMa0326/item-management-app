package com.example.itemmanagementandroid.ui.screens.itemedit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemEditFormMapperTest {
    @Test
    fun parseTags_normalizesAndDeduplicates() {
        val parsed = ItemEditFormMapper.parseTags(" audio, commute ,audio, ,  work ")

        assertEquals(listOf("audio", "commute", "work"), parsed)
    }

    @Test
    fun parsePurchasePrice_invalidValue_throws() {
        val throwable = runCatching {
            ItemEditFormMapper.parsePurchasePrice("abc")
        }.exceptionOrNull()

        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Purchase price must be a valid number.", throwable?.message)
    }

    @Test
    fun parsePurchasePrice_negativeValue_throws() {
        val throwable = runCatching {
            ItemEditFormMapper.parsePurchasePrice("-0.01")
        }.exceptionOrNull()

        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Purchase price must be greater than or equal to 0.", throwable?.message)
    }

    @Test
    fun parsePurchasePrice_zeroValue_isAllowed() {
        val parsed = ItemEditFormMapper.parsePurchasePrice("0")

        assertEquals(0.0, parsed ?: -1.0, 0.000001)
    }

    @Test
    fun parsePurchasePrice_roundsToTwoDecimalPlaces() {
        val parsed = ItemEditFormMapper.parsePurchasePrice("10.125")

        assertEquals(10.13, parsed ?: 0.0, 0.000001)
    }

    @Test
    fun normalizePurchaseDate_supportsLooseInputs() {
        assertEquals("2026-03-07", ItemEditFormMapper.normalizePurchaseDate("2026-3-7"))
        assertEquals("2026-03-07", ItemEditFormMapper.normalizePurchaseDate("2026/03/07"))
        assertEquals("2026-03-07", ItemEditFormMapper.normalizePurchaseDate("2026.3.7"))
        assertEquals("2026-03-07", ItemEditFormMapper.normalizePurchaseDate("20260307"))
    }

    @Test
    fun normalizePurchaseDate_invalidDate_throws() {
        val throwable = runCatching {
            ItemEditFormMapper.normalizePurchaseDate("2026-02-30")
        }.exceptionOrNull()

        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Purchase date must be a valid date in YYYY-MM-DD format.", throwable?.message)
    }

    @Test
    fun parseCustomAttributes_parsesBooleanNumberAndString() {
        val parsed = ItemEditFormMapper.parseCustomAttributes(
            listOf(
                ItemEditCustomAttributeRowUiModel(
                    rowId = "r1",
                    key = "enabled",
                    value = "true"
                ),
                ItemEditCustomAttributeRowUiModel(
                    rowId = "r2",
                    key = "count",
                    value = "7"
                ),
                ItemEditCustomAttributeRowUiModel(
                    rowId = "r3",
                    key = "ratio",
                    value = "1.5"
                ),
                ItemEditCustomAttributeRowUiModel(
                    rowId = "r4",
                    key = "note",
                    value = "hello"
                )
            )
        )

        assertEquals(true, parsed["enabled"])
        assertEquals(7L, parsed["count"])
        assertEquals(1.5, parsed["ratio"])
        assertEquals("hello", parsed["note"])
    }

    @Test
    fun parseCustomAttributes_emptyKey_throws() {
        val throwable = runCatching {
            ItemEditFormMapper.parseCustomAttributes(
                listOf(
                    ItemEditCustomAttributeRowUiModel(
                        rowId = "r1",
                        key = "",
                        value = "abc"
                    )
                )
            )
        }.exceptionOrNull()

        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Custom attribute key cannot be empty.", throwable?.message)
    }
}
