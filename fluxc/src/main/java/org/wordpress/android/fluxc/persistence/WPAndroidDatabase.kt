package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity

@Database(
        entities = [
            BloggingReminders::class,
            CommentEntity::class
        ],
        version = 2
)
abstract class WPAndroidDatabase : RoomDatabase() {
    abstract fun bloggingRemindersDao(): BloggingRemindersDao

    abstract fun commentsDao(): CommentsDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WPAndroidDatabase::class.java,
                "wp-android-database"
        ).fallbackToDestructiveMigration().build()
        // TODOD: add migrations
    }
}
