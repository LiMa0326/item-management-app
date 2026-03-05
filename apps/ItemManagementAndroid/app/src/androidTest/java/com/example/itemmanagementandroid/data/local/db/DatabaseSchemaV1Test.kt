package com.example.itemmanagementandroid.data.local.db

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchemaV1Test {
    private lateinit var database: ItemManagementDatabase
    private lateinit var sqliteDatabase: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)

        database = Room.databaseBuilder(
            context,
            ItemManagementDatabase::class.java,
            TEST_DB_NAME
        )
            .allowMainThreadQueries()
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

        sqliteDatabase = database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database.close()
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun databaseCreatesAllCoreTables() {
        val tableNames = readTableNames()

        assertTrue(tableNames.contains("categories"))
        assertTrue(tableNames.contains("items"))
        assertTrue(tableNames.contains("item_photos"))
    }

    @Test
    fun categoriesColumnsAndNotNullMatchSpec() {
        val columns = readTableColumns("categories")

        assertEquals(
            setOf(
                "id",
                "name",
                "sort_order",
                "is_archived",
                "is_system_default",
                "created_at",
                "updated_at"
            ),
            columns.keys
        )

        assertColumnNotNull(columns, "id")
        assertColumnNotNull(columns, "name")
        assertColumnNotNull(columns, "sort_order")
        assertColumnNotNull(columns, "is_archived")
        assertColumnNotNull(columns, "is_system_default")
        assertColumnNotNull(columns, "created_at")
        assertColumnNotNull(columns, "updated_at")
    }

    @Test
    fun itemsColumnsAndNotNullMatchSpec() {
        val columns = readTableColumns("items")

        assertEquals(
            setOf(
                "id",
                "category_id",
                "name",
                "purchase_date",
                "purchase_price",
                "purchase_currency",
                "purchase_place",
                "description",
                "tags_json",
                "custom_attributes_json",
                "created_at",
                "updated_at",
                "deleted_at"
            ),
            columns.keys
        )

        assertColumnNotNull(columns, "id")
        assertColumnNotNull(columns, "category_id")
        assertColumnNotNull(columns, "name")
        assertColumnNotNull(columns, "tags_json")
        assertColumnNotNull(columns, "custom_attributes_json")
        assertColumnNotNull(columns, "created_at")
        assertColumnNotNull(columns, "updated_at")
    }

    @Test
    fun itemPhotosColumnsAndNotNullMatchSpec() {
        val columns = readTableColumns("item_photos")

        assertEquals(
            setOf(
                "id",
                "item_id",
                "local_uri",
                "thumbnail_uri",
                "content_type",
                "width",
                "height",
                "created_at"
            ),
            columns.keys
        )

        assertColumnNotNull(columns, "id")
        assertColumnNotNull(columns, "item_id")
        assertColumnNotNull(columns, "local_uri")
        assertColumnNotNull(columns, "content_type")
        assertColumnNotNull(columns, "created_at")
    }

    @Test
    fun databaseVersionIsV1() {
        val userVersion = sqliteDatabase.query("PRAGMA user_version").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        assertEquals(DatabaseVersions.SCHEMA_V1, userVersion)
        assertEquals(DatabaseVersions.CURRENT, userVersion)
    }

    private fun readTableNames(): Set<String> {
        return sqliteDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val result = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                result.add(cursor.getString(nameIndex))
            }
            result
        }
    }

    private fun readTableColumns(tableName: String): Map<String, SqliteColumnInfo> {
        return sqliteDatabase.query("PRAGMA table_info($tableName)").use { cursor ->
            val result = linkedMapOf<String, SqliteColumnInfo>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val type = cursor.getString(typeIndex)
                val isNotNull = cursor.getInt(notNullIndex) == 1
                result[name] = SqliteColumnInfo(
                    name = name,
                    type = type,
                    notNull = isNotNull
                )
            }
            result
        }
    }

    private fun assertColumnNotNull(
        columns: Map<String, SqliteColumnInfo>,
        columnName: String
    ) {
        val column = columns[columnName]
        assertTrue("Missing column: $columnName", column != null)
        assertTrue("Column should be NOT NULL: $columnName", column!!.notNull)
    }

    private data class SqliteColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean
    )

    companion object {
        private const val TEST_DB_NAME: String = "schema-v1-test.db"
    }
}
