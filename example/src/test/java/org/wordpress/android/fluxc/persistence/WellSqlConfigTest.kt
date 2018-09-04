package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.post.PostType
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class WellSqlConfigTest {
    @Test
    fun testMigrationFrom39to40() {
        val postModelNewColumns = arrayOf("_id", "TYPE", "LOCAL_SITE_ID", "REMOTE_SITE_ID", "REMOTE_POST_ID", "TITLE",
                "CONTENT", "DATE_CREATED", "CATEGORY_IDS", "CUSTOM_FIELDS", "LINK", "EXCERPT", "TAG_NAMES", "STATUS",
                "PASSWORD", "FEATURED_IMAGE_ID", "POST_FORMAT", "SLUG", "LATITUDE", "LONGITUDE", "PARENT_ID",
                "PARENT_TITLE", "IS_LOCAL_DRAFT", " IS_LOCALLY_CHANGED", "DATE_LOCALLY_CHANGED",
                "LAST_KNOWN_REMOTE_FEATURED_IMAGE_ID", "HAS_CAPABILITY_PUBLISH_POST", "HAS_CAPABILITY_EDIT_POST",
                "HAS_CAPABILITY_DELETE_POST")

        val database = createDataBase("schemas_39.sql")
        insertIntoPostModel(database, createPostPropertiesV39(1, true))
        insertIntoPostModel(database, createPostPropertiesV39(2, false))

        runMigration(database, 39, 40)

        val cursor1 = queryFromPostModel(database, postModelNewColumns, "1")
        cursor1.moveToFirst()
        assertThat(cursor1.count).isEqualTo(1)
        assertThat(cursor1.getInt(cursor1.getColumnIndex("TYPE"))).isEqualTo(PostType.PAGE.modelValue())
        cursor1.close()

        val cursor2 = queryFromPostModel(database, postModelNewColumns, "2")
        cursor2.moveToFirst()
        assertThat(cursor2.count).isEqualTo(1)
        assertThat(cursor2.getInt(cursor1.getColumnIndex("TYPE"))).isEqualTo(PostType.POST.modelValue())
        cursor2.close()
    }

    private fun createDataBase(schema: String): SQLiteDatabase {
        val tempFile = File.createTempFile("WellSqlConfigTest", "db")
        val database = SQLiteDatabase.openOrCreateDatabase(tempFile.path, null)
        execSQLFromFile(database, "database/$schema")
        return database
    }

    private fun insertIntoPostModel(database: SQLiteDatabase, values: ContentValues) =
            assertThat(database.insert("PostModel", null, values)).isNotEqualTo(-1)

    private fun queryFromPostModel(database: SQLiteDatabase, columns: Array<String>, id: String) =
            database.query("PostModel", columns, "_id=?", arrayOf(id), null, null, null, null)

    private fun runMigration(database: SQLiteDatabase, from: Int, to: Int) {
        val wellSqlConfig = WellSqlConfig(RuntimeEnvironment.application, WellSqlConfig.ADDON_WOOCOMMERCE)
        wellSqlConfig.onUpgrade(database, null, from, to)
    }

    private fun execSQLFromFile(database: SQLiteDatabase, filename: String) {
        try {
            val inputStream = this.javaClass.classLoader.getResourceAsStream(filename)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            bufferedReader.forEachLine { database.execSQL(it) }
            bufferedReader.close()
        } catch (e: IOException) {
            throw IllegalStateException("Could not load file: $filename")
        }
    }

    private fun createPostPropertiesV39(id: Int, isPage: Boolean): ContentValues {
        val cv = ContentValues()
        cv.put("_id", id)
        cv.put("IS_PAGE", isPage)
        cv.put("LOCAL_SITE_ID", id)
        cv.put("REMOTE_SITE_ID", id)
        cv.put("REMOTE_POST_ID", id)
        cv.put("TITLE", "Title $id")
        cv.put("CONTENT", "Content $id")
        cv.put("DATE_CREATED", Date(id.toLong()).toString())
        cv.put("CATEGORY_IDS", "")
        cv.put("CUSTOM_FIELDS", "")
        cv.put("LINK", "")
        cv.put("EXCERPT", "")
        cv.put("TAG_NAMES", "")
        cv.put("STATUS", "published")
        cv.put("PASSWORD", "")
        cv.put("FEATURED_IMAGE_ID", id)
        cv.put("POST_FORMAT", "")
        cv.put("SLUG", "")
        cv.put("LATITUDE", id)
        cv.put("LONGITUDE", id)
        cv.put("PARENT_ID", id)
        cv.put("PARENT_TITLE", "")
        cv.put("IS_LOCAL_DRAFT", false)
        cv.put("IS_LOCALLY_CHANGED", false)
        cv.put("DATE_LOCALLY_CHANGED", null as String?)
        cv.put("LAST_KNOWN_REMOTE_FEATURED_IMAGE_ID", null as String?)
        cv.put("HAS_CAPABILITY_PUBLISH_POST", true)
        cv.put("HAS_CAPABILITY_EDIT_POST", true)
        cv.put("HAS_CAPABILITY_DELETE_POST", true)
        return cv
    }
}
