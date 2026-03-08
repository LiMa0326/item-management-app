package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.ItemPhotoDao
import com.example.itemmanagementandroid.data.local.dao.model.DeferredPhotoCleanupRow
import com.example.itemmanagementandroid.data.local.dao.model.ItemPhotoCoverRow
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity
import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PhotoRepositoryImplTest {
    private lateinit var fakeItemPhotoDao: FakeItemPhotoDao
    private lateinit var clock: MutableClock
    private lateinit var repository: PhotoRepositoryImpl
    private var nextId: Int = 0

    @Before
    fun setUp() {
        fakeItemPhotoDao = FakeItemPhotoDao()
        clock = MutableClock(Instant.parse("2026-03-06T09:00:00Z"))
        repository = PhotoRepositoryImpl(
            itemPhotoDao = fakeItemPhotoDao,
            clock = clock,
            idGenerator = { "photo_${nextId++}" }
        )
    }

    @Test
    fun addListAndGet_roundTripFieldsWithCreatedAtAscending() = runBlocking {
        val firstPhoto = repository.add(
            draft = ItemPhotoDraft(
                itemId = " item_001 ",
                localUri = " file:///photos/first.jpg ",
                thumbnailUri = " file:///thumbnails/first_thumb.jpg ",
                contentType = " image/jpeg ",
                width = 2000,
                height = 1200
            )
        )
        clock.advanceSeconds(10)
        val secondPhoto = repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/second.jpg",
                thumbnailUri = "file:///thumbnails/second_thumb.jpg",
                contentType = "image/jpeg",
                width = 1600,
                height = 900
            )
        )

        val listed = repository.listByItem("item_001")
        val fetched = repository.get(secondPhoto.id)

        assertEquals(listOf(firstPhoto.id, secondPhoto.id), listed.map { it.id })
        assertNotNull(fetched)
        assertEquals(secondPhoto, fetched)
        assertEquals("item_001", firstPhoto.itemId)
        assertEquals("file:///photos/first.jpg", firstPhoto.localUri)
        assertEquals("file:///thumbnails/first_thumb.jpg", firstPhoto.thumbnailUri)
        assertEquals("image/jpeg", firstPhoto.contentType)
        assertEquals("2026-03-06T09:00:00Z", firstPhoto.createdAt)
        assertEquals("2026-03-06T09:00:10Z", secondPhoto.createdAt)
    }

    @Test
    fun remove_isIdempotentAndReturnsBoolean() = runBlocking {
        val created = repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/remove.jpg",
                contentType = "image/jpeg"
            )
        )

        val firstRemove = repository.remove(created.id)
        val secondRemove = repository.remove(created.id)

        assertTrue(firstRemove)
        assertFalse(secondRemove)
    }

    @Test
    fun deferredCleanupCandidates_onlyAppearWhenItemSoftDeleted() = runBlocking {
        repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/deferred_1.jpg",
                contentType = "image/jpeg"
            )
        )
        clock.advanceSeconds(5)
        repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/deferred_2.jpg",
                contentType = "image/jpeg"
            )
        )

        assertTrue(repository.listDeferredCleanupCandidates().isEmpty())

        fakeItemPhotoDao.markItemDeleted(
            itemId = "item_001",
            deletedAt = "2026-03-06T09:05:00Z"
        )

        val candidatesAfterDelete = repository.listDeferredCleanupCandidates()
        assertEquals(2, candidatesAfterDelete.size)
        assertTrue(
            candidatesAfterDelete.all {
                it.marker == DeferredPhotoCleanupCandidate.ITEM_SOFT_DELETED
            }
        )
        assertTrue(candidatesAfterDelete.all { it.itemDeletedAt == "2026-03-06T09:05:00Z" })

        fakeItemPhotoDao.restoreItem("item_001")
        assertTrue(repository.listDeferredCleanupCandidates().isEmpty())
    }

    @Test
    fun add_rejectsInvalidInput() {
        val blankItemId = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.add(
                    draft = ItemPhotoDraft(
                        itemId = " ",
                        localUri = "file:///photos/1.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
        assertTrue(blankItemId.message!!.contains("itemId"))

        val blankLocalUri = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.add(
                    draft = ItemPhotoDraft(
                        itemId = "item_001",
                        localUri = " ",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
        assertTrue(blankLocalUri.message!!.contains("localUri"))

        val blankContentType = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.add(
                    draft = ItemPhotoDraft(
                        itemId = "item_001",
                        localUri = "file:///photos/1.jpg",
                        contentType = " "
                    )
                )
            }
        }
        assertTrue(blankContentType.message!!.contains("contentType"))

        val invalidWidth = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.add(
                    draft = ItemPhotoDraft(
                        itemId = "item_001",
                        localUri = "file:///photos/1.jpg",
                        contentType = "image/jpeg",
                        width = 0
                    )
                )
            }
        }
        assertTrue(invalidWidth.message!!.contains("width"))

        val invalidHeight = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.add(
                    draft = ItemPhotoDraft(
                        itemId = "item_001",
                        localUri = "file:///photos/1.jpg",
                        contentType = "image/jpeg",
                        height = -1
                    )
                )
            }
        }
        assertTrue(invalidHeight.message!!.contains("height"))
    }

    @Test
    fun listCoversByItemIds_returnsFirstPhotoPerItem() = runBlocking {
        repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/001_first.jpg",
                thumbnailUri = "file:///thumbs/001_first.jpg",
                contentType = "image/jpeg"
            )
        )
        clock.advanceSeconds(2)
        repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_001",
                localUri = "file:///photos/001_second.jpg",
                thumbnailUri = "file:///thumbs/001_second.jpg",
                contentType = "image/jpeg"
            )
        )
        repository.add(
            draft = ItemPhotoDraft(
                itemId = "item_002",
                localUri = "file:///photos/002_first.jpg",
                thumbnailUri = "file:///thumbs/002_first.jpg",
                contentType = "image/jpeg"
            )
        )

        val covers = repository.listCoversByItemIds(
            itemIds = listOf("item_001", "item_002", "item_not_exist")
        )

        assertEquals(2, covers.size)
        assertTrue(
            covers.any { cover ->
                cover.itemId == "item_001" &&
                    cover.thumbnailUri == "file:///thumbs/001_first.jpg"
            }
        )
        assertTrue(
            covers.any { cover ->
                cover.itemId == "item_002" &&
                    cover.localUri == "file:///photos/002_first.jpg"
            }
        )
    }

    private class FakeItemPhotoDao : ItemPhotoDao {
        private val photos: LinkedHashMap<String, ItemPhotoEntity> = linkedMapOf()
        private val itemDeletedAt: LinkedHashMap<String, String?> = linkedMapOf()

        override suspend fun listAll(): List<ItemPhotoEntity> {
            return photos.values.toList()
        }

        override suspend fun listByItemOrdered(itemId: String): List<ItemPhotoEntity> {
            return photos.values
                .filter { it.itemId == itemId }
                .sortedBy { it.createdAt }
        }

        override suspend fun getById(photoId: String): ItemPhotoEntity? {
            return photos[photoId]
        }

        override suspend fun insert(photo: ItemPhotoEntity): Long {
            require(!photos.containsKey(photo.id)) {
                "Photo already exists: ${photo.id}"
            }
            photos[photo.id] = photo
            itemDeletedAt.putIfAbsent(photo.itemId, null)
            return 1L
        }

        override suspend fun insertOrReplace(photo: ItemPhotoEntity): Long {
            photos[photo.id] = photo
            itemDeletedAt.putIfAbsent(photo.itemId, null)
            return 1L
        }

        override suspend fun deleteById(photoId: String): Int {
            return if (photos.remove(photoId) != null) {
                1
            } else {
                0
            }
        }

        override suspend fun deleteAll(): Int {
            val count = photos.size
            photos.clear()
            itemDeletedAt.clear()
            return count
        }

        override suspend fun listDeferredCleanupRows(): List<DeferredPhotoCleanupRow> {
            return photos.values
                .mapNotNull { photo ->
                    val deletedAt = itemDeletedAt[photo.itemId]
                    if (deletedAt == null) {
                        null
                    } else {
                        DeferredPhotoCleanupRow(
                            photoId = photo.id,
                            itemId = photo.itemId,
                            localUri = photo.localUri,
                            thumbnailUri = photo.thumbnailUri,
                            contentType = photo.contentType,
                            itemDeletedAt = deletedAt
                        )
                    }
                }
                .sortedWith(
                    compareBy<DeferredPhotoCleanupRow> { it.itemDeletedAt }
                        .thenBy { photos.getValue(it.photoId).createdAt }
                )
        }

        override suspend fun listCoversByItemIds(itemIds: List<String>): List<ItemPhotoCoverRow> {
            if (itemIds.isEmpty()) {
                return emptyList()
            }
            return itemIds.mapNotNull { itemId ->
                photos.values
                    .filter { photo -> photo.itemId == itemId }
                    .minByOrNull { photo -> photo.createdAt }
                    ?.let { firstPhoto ->
                        ItemPhotoCoverRow(
                            itemId = itemId,
                            thumbnailUri = firstPhoto.thumbnailUri,
                            localUri = firstPhoto.localUri
                        )
                    }
            }
        }

        fun markItemDeleted(itemId: String, deletedAt: String) {
            itemDeletedAt[itemId] = deletedAt
        }

        fun restoreItem(itemId: String) {
            itemDeletedAt[itemId] = null
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
