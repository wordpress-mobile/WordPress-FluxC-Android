package org.wordpress.android.fluxc.persistance.room

import android.arch.persistence.db.SupportSQLiteDatabase
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import org.wordpress.android.fluxc.persistence.room.Migration1to2
import org.wordpress.android.fluxc.persistence.room.STATS_DB_NAME

class StatsDatabaseMigrationTest : MigrationTest() {
    @Test
    fun migrationFrom1to2() {
        val db = testHelper.createDatabase(STATS_DB_NAME, 1)
        // Insert stats block without nullable fields
        insertV1StatsBlock(db, 1, ALL_TIME_INSIGHTS, INSIGHTS, json = "{test: value}")
        // Insert stats block with nullable fields
        insertV1StatsBlock(db, 2, LATEST_POST_DETAIL_INSIGHTS, DAY, "2019-10-10", 5, json = "{test: value2}")
        db.close()

        val dbV2 = testHelper.runMigrationsAndValidate(STATS_DB_NAME, 2, true, Migration1to2)

        val cursor = dbV2.query("SELECT * FROM StatsBlock WHERE localSiteId = 1")

        assertThat(cursor.count).isEqualTo(1)
        val columnIndex = cursor.getColumnIndex("postId")
        assertThat(columnIndex).isEqualTo(5)
        cursor.moveToFirst()
        assertThat(cursor.getInt(columnIndex)).isEqualTo(0)
    }

    private fun insertV1StatsBlock(
        db: SupportSQLiteDatabase,
        siteId: Int,
        blockType: BlockType,
        statsType: StatsType,
        date: String? = null,
        postId: Int? = null,
        json: String
    ) {
        val values = ContentValues()
        values.put("localSiteId", siteId)
        values.put("blockType", blockType.name)
        values.put("statsType", statsType.name)
        if (date != null) {
            values.put("date", date)
        }
        if (postId != null) {
            values.put("postId", postId)
        }
        values.put("json", json)
        db.insert("StatsBlock", CONFLICT_FAIL, values)
    }
}
