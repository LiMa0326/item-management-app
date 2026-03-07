package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.ItemDao
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.repository.json.ItemJsonCodec
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ItemRepositoryImplTest {
    private lateinit var fakeItemDao: FakeItemDao
    private lateinit var clock: MutableClock
    private lateinit var repository: ItemRepositoryImpl
    private var nextId: Int = 0

    @Before
    fun setUp() {
        fakeItemDao = FakeItemDao()
        clock = MutableClock(Instant.parse("2026-03-05T10:00:00Z"))
        repository = ItemRepositoryImpl(
            itemDao = fakeItemDao,
            clock = clock,
            idGenerator = { "item_${nextId++}" },
            jsonCodec = FakeItemJsonCodec()
        )
    }

    @Test
    fun createAndGet_roundTripFields() = runBlocking {
        val created = repository.create(
            draft = ItemDraft(
                categoryId = "  cat_electronics  ",
                name = "  Sony WH-1000XM5  ",
                purchaseDate = "2025-10-12",
                purchasePrice = 329.99,
                purchaseCurrency = "  USD  ",
                purchasePlace = "  Amazon  ",
                description = "  Noise cancelling headphone  ",
                tags = listOf("  audio ", " commute ", "audio"),
                customAttributes = mapOf(
                    "color" to " Black ",
                    "warrantyYears" to 2,
                    "active" to true
                )
            )
        )

        val fetched = repository.get(created.id)

        assertNotNull(fetched)
        assertEquals(created, fetched)
        assertEquals("cat_electronics", created.categoryId)
        assertEquals("Sony WH-1000XM5", created.name)
        assertEquals(listOf("audio", "commute"), created.tags)
        assertEquals("Black", created.customAttributes["color"])
        assertEquals(2L, created.customAttributes["warrantyYears"])
        assertEquals(true, created.customAttributes["active"])
        assertNull(created.deletedAt)
        assertEquals("2026-03-05T10:00:00Z", created.createdAt)
        assertEquals("2026-03-05T10:00:00Z", created.updatedAt)
    }

    @Test
    fun update_changesUpdatedAtButKeepsCreatedAt() = runBlocking {
        val created = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Old Name"
            )
        )

        clock.advanceSeconds(60)
        val updated = repository.update(
            itemId = created.id,
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "New Name",
                customAttributes = mapOf("version" to 2)
            )
        )

        assertEquals(created.createdAt, updated.createdAt)
        assertEquals("2026-03-05T10:01:00Z", updated.updatedAt)
        assertEquals("New Name", updated.name)
        assertEquals(2L, updated.customAttributes["version"])
        assertNull(updated.deletedAt)
    }

    @Test
    fun softDeleteAndRestore_changeVisibility() = runBlocking {
        val first = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Item A"
            )
        )
        clock.advanceSeconds(10)
        val second = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Item B"
            )
        )

        clock.advanceSeconds(10)
        val deleted = repository.softDelete(first.id)

        val activeOnlyAfterDelete = repository.list()
        val allAfterDelete = repository.list(includeDeleted = true)

        assertEquals(1, activeOnlyAfterDelete.size)
        assertEquals(second.id, activeOnlyAfterDelete.first().id)
        assertEquals(2, allAfterDelete.size)
        assertNotNull(deleted.deletedAt)

        clock.advanceSeconds(10)
        val restored = repository.restore(first.id)
        val activeOnlyAfterRestore = repository.list()

        assertNull(restored.deletedAt)
        assertEquals(2, activeOnlyAfterRestore.size)
        assertTrue(activeOnlyAfterRestore.any { it.id == first.id })
    }

    @Test
    fun softDeleteAndRestore_areIdempotent() = runBlocking {
        val created = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Idempotent Item"
            )
        )

        val firstDelete = repository.softDelete(created.id)
        val secondDelete = repository.softDelete(created.id)
        assertEquals(firstDelete.deletedAt, secondDelete.deletedAt)
        assertEquals(firstDelete.updatedAt, secondDelete.updatedAt)

        val firstRestore = repository.restore(created.id)
        val secondRestore = repository.restore(created.id)
        assertNull(firstRestore.deletedAt)
        assertNull(secondRestore.deletedAt)
        assertEquals(firstRestore.updatedAt, secondRestore.updatedAt)
    }

    @Test
    fun create_rejectsNestedCustomAttributes() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create(
                    draft = ItemDraft(
                        categoryId = "cat_electronics",
                        name = "Invalid",
                        customAttributes = mapOf(
                            "nested" to mapOf("k" to "v")
                        )
                    )
                )
            }
        }

        assertTrue(exception.message!!.contains("customAttributes[nested]"))
    }

    @Test
    fun create_rejectsDuplicateName_caseAndTrimInsensitive() = runBlocking {
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "My Headphone"
            )
        )

        val exception = assertThrows(DuplicateItemNameException::class.java) {
            runBlocking {
                repository.create(
                    draft = ItemDraft(
                        categoryId = "cat_other",
                        name = "  my headphone  "
                    )
                )
            }
        }

        assertEquals("Item name already exists.", exception.message)
    }

    @Test
    fun create_allowsDuplicateNameWhenExistingIsSoftDeleted() = runBlocking {
        val created = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Reusable Name"
            )
        )
        repository.softDelete(created.id)

        val recreated = repository.create(
            draft = ItemDraft(
                categoryId = "cat_other",
                name = "  reusable name "
            )
        )

        assertEquals("reusable name", recreated.name)
        assertNull(recreated.deletedAt)
    }

    @Test
    fun update_duplicateNamePolicy_excludesSelfAndRejectsOthers() = runBlocking {
        val first = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Unique A"
            )
        )
        val second = repository.create(
            draft = ItemDraft(
                categoryId = "cat_other",
                name = "Unique B"
            )
        )

        val selfUpdate = repository.update(
            itemId = first.id,
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "  unique a "
            )
        )
        assertEquals("unique a", selfUpdate.name.lowercase())

        val exception = assertThrows(DuplicateItemNameException::class.java) {
            runBlocking {
                repository.update(
                    itemId = second.id,
                    draft = ItemDraft(
                        categoryId = "cat_other",
                        name = "UNIQUE A"
                    )
                )
            }
        }

        assertEquals("Item name already exists.", exception.message)
    }

    @Test
    fun tagsAndCustomAttributes_roundTripWorks() = runBlocking {
        val created = repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "RoundTrip",
                tags = listOf("a", " b ", "a", "   "),
                customAttributes = mapOf(
                    "intValue" to 1,
                    "doubleValue" to 1.5,
                    "flag" to false,
                    "note" to " text "
                )
            )
        )

        val fetched = repository.get(created.id)!!

        assertEquals(listOf("a", "b"), fetched.tags)
        assertEquals(1L, fetched.customAttributes["intValue"])
        assertEquals(1.5, fetched.customAttributes["doubleValue"])
        assertEquals(false, fetched.customAttributes["flag"])
        assertEquals("text", fetched.customAttributes["note"])
    }

    @Test
    fun list_filtersByCategory() = runBlocking {
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "A-1"
            )
        )
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_b",
                name = "B-1"
            )
        )

        val result = repository.list(
            query = ItemListQuery(
                categoryId = "cat_a",
                sortOption = ItemListSortOption.RECENTLY_UPDATED
            )
        )

        assertEquals(1, result.size)
        assertEquals("cat_a", result.first().categoryId)
        assertEquals("A-1", result.first().name)
    }

    @Test
    fun list_purchaseDateSort_descendingAndNullsLast() = runBlocking {
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "No Date",
                purchaseDate = null
            )
        )
        clock.advanceSeconds(1)
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Date 2024",
                purchaseDate = "2024-01-01"
            )
        )
        clock.advanceSeconds(1)
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Date 2025",
                purchaseDate = "2025-12-31"
            )
        )

        val result = repository.list(
            query = ItemListQuery(
                sortOption = ItemListSortOption.PURCHASE_DATE
            )
        )

        assertEquals(
            listOf("Date 2025", "Date 2024", "No Date"),
            result.map { item -> item.name }
        )
    }

    @Test
    fun list_purchasePriceSort_descendingAndNullsLast() = runBlocking {
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "No Price",
                purchasePrice = null
            )
        )
        clock.advanceSeconds(1)
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Price 12",
                purchasePrice = 12.0
            )
        )
        clock.advanceSeconds(1)
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_electronics",
                name = "Price 200",
                purchasePrice = 200.0
            )
        )

        val result = repository.list(
            query = ItemListQuery(
                sortOption = ItemListSortOption.PURCHASE_PRICE
            )
        )

        assertEquals(
            listOf("Price 200", "Price 12", "No Price"),
            result.map { item -> item.name }
        )
    }

    @Test
    fun list_includeDeletedFalseWithCategoryFilter_hidesSoftDeleted() = runBlocking {
        val active = repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Active A"
            )
        )
        clock.advanceSeconds(1)
        val deleted = repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Deleted A"
            )
        )
        clock.advanceSeconds(1)
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_b",
                name = "Active B"
            )
        )
        repository.softDelete(deleted.id)

        val activeOnly = repository.list(
            query = ItemListQuery(
                includeDeleted = false,
                categoryId = "cat_a"
            )
        )
        val includeDeleted = repository.list(
            query = ItemListQuery(
                includeDeleted = true,
                categoryId = "cat_a"
            )
        )

        assertEquals(listOf(active.id), activeOnly.map { it.id })
        assertEquals(2, includeDeleted.size)
    }

    private class FakeItemDao : ItemDao {
        private val items: LinkedHashMap<String, ItemEntity> = linkedMapOf()

        override suspend fun listAllOrdered(): List<ItemEntity> {
            return items.values.sortedWith(
                compareByDescending<ItemEntity> { it.updatedAt }
                    .thenByDescending { it.createdAt }
            )
        }

        override suspend fun listActiveOrdered(): List<ItemEntity> {
            return listAllOrdered().filter { it.deletedAt == null }
        }

        override suspend fun getById(itemId: String): ItemEntity? {
            return items[itemId]
        }

        override suspend fun countActiveByNormalizedName(name: String): Int {
            val normalized = name.trim().lowercase()
            return items.values.count { entity ->
                entity.deletedAt == null && entity.name.trim().lowercase() == normalized
            }
        }

        override suspend fun countActiveByNormalizedNameExcludingId(
            name: String,
            excludeItemId: String
        ): Int {
            val normalized = name.trim().lowercase()
            return items.values.count { entity ->
                entity.id != excludeItemId &&
                    entity.deletedAt == null &&
                    entity.name.trim().lowercase() == normalized
            }
        }

        override suspend fun insert(item: ItemEntity): Long {
            require(!items.containsKey(item.id)) {
                "Item already exists: ${item.id}"
            }
            items[item.id] = item
            return 1L
        }

        override suspend fun update(item: ItemEntity): Int {
            require(items.containsKey(item.id)) {
                "Item does not exist: ${item.id}"
            }
            items[item.id] = item
            return 1
        }
    }

    private class FakeItemJsonCodec : ItemJsonCodec {
        override fun encodeTags(tags: List<String>): String {
            return tags.joinToString(separator = "|")
        }

        override fun decodeTags(tagsJson: String): List<String> {
            if (tagsJson.isBlank()) {
                return emptyList()
            }
            return tagsJson.split('|')
                .map(String::trim)
                .filter(String::isNotEmpty)
        }

        override fun encodeCustomAttributes(customAttributes: Map<String, Any>): String {
            return customAttributes.entries.joinToString(separator = ";") { (key, value) ->
                when (value) {
                    is String -> "$key=s:$value"
                    is Boolean -> "$key=b:$value"
                    is Number -> "$key=n:${value.toString()}"
                    else -> throw IllegalArgumentException(
                        "customAttributes[$key] must be string|number|boolean."
                    )
                }
            }
        }

        override fun decodeCustomAttributes(customAttributesJson: String): Map<String, Any> {
            if (customAttributesJson.isBlank()) {
                return emptyMap()
            }

            val result = linkedMapOf<String, Any>()
            customAttributesJson.split(";").forEach { token ->
                val keyAndPayload = token.split("=", limit = 2)
                require(keyAndPayload.size == 2) { "Invalid token: $token" }
                val key = keyAndPayload[0]
                val payload = keyAndPayload[1]
                when {
                    payload.startsWith("s:") -> result[key] = payload.removePrefix("s:")
                    payload.startsWith("b:") -> result[key] = payload.removePrefix("b:").toBooleanStrict()
                    payload.startsWith("n:") -> {
                        val numberString = payload.removePrefix("n:")
                        val number = numberString.toLongOrNull() ?: numberString.toDouble()
                        result[key] = number
                    }
                    else -> throw IllegalArgumentException("Unsupported payload: $payload")
                }
            }
            return result
        }
    }

    private class MutableClock(
        private var currentInstant: Instant
    ) : Clock() {
        override fun instant(): Instant = currentInstant

        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId?): Clock = this

        fun advanceSeconds(seconds: Long) {
            currentInstant = currentInstant.plusSeconds(seconds)
        }
    }
}
