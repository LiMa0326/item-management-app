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
