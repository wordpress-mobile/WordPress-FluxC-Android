package org.wordpress.android.fluxc.persistence.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration1to2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE StatsBlock SET postId=0 WHERE postId IS NULL")
        database.beginTransaction()
        try {
            database.execSQL("ALTER TABLE StatsBlock RENAME TO StatsBlock_old;")
            database.execSQL(
                    """
            CREATE TABLE `StatsBlock`
            (`id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `localSiteId` INTEGER NOT NULL,
            `blockType` TEXT NOT NULL,
            `statsType` TEXT NOT NULL,
            `date` TEXT,
            `postId` INTEGER NOT NULL,
            `json` TEXT NOT NULL)
            """
            )
            database.execSQL(
                    """
            INSERT INTO StatsBlock(`id`, `localSiteId`, `blockType`, `statsType`, `date`, `postId`, `json`)
             SELECT `id`, `localSiteId`, `blockType`, `statsType`, `date`, `postId`, `json`
             FROM StatsBlock_old
        """
            )
            database.execSQL("DROP TABLE StatsBlock_old")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
