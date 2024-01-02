package org.wordpress.android.fluxc.persistence.blaze

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BlazeTargetingDao {
    @Query("SELECT * FROM BlazeTargetingDevices WHERE locale = :locale")
    fun observeDevices(locale: String): Flow<List<BlazeTargetingDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<BlazeTargetingDeviceEntity>)

    @Query("DELETE FROM BlazeTargetingDevices")
    suspend fun deleteDevices()

    @Transaction
    suspend fun replaceDevices(devices: List<BlazeTargetingDeviceEntity>) {
        deleteDevices()
        insertDevices(devices)
    }

    @Query("SELECT * FROM BlazeTargetingTopics WHERE locale = :locale")
    fun observeTopics(locale: String): Flow<List<BlazeTargetingTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<BlazeTargetingTopicEntity>)

    @Query("DELETE FROM BlazeTargetingTopics")
    suspend fun deleteTopics()

    @Transaction
    suspend fun replaceTopics(topics: List<BlazeTargetingTopicEntity>) {
        deleteTopics()
        insertTopics(topics)
    }

    @Query("SELECT * FROM BlazeTargetingLanguages WHERE locale = :locale")
    fun observeLanguages(locale: String): Flow<List<BlazeTargetingLanguageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguages(languages: List<BlazeTargetingLanguageEntity>)

    @Query("DELETE FROM BlazeTargetingLanguages")
    suspend fun deleteLanguages()

    @Transaction
    suspend fun replaceLanguages(languages: List<BlazeTargetingLanguageEntity>) {
        deleteLanguages()
        insertLanguages(languages)
    }
}

@Entity(tableName = "BlazeTargetingDevices")
data class BlazeTargetingDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val locale: String
)

@Entity(tableName = "BlazeTargetingTopics")
data class BlazeTargetingTopicEntity(
    @PrimaryKey val id: String,
    val description: String,
    val locale: String
)

@Entity(tableName = "BlazeTargetingLanguages")
data class BlazeTargetingLanguageEntity(
    @PrimaryKey val id: String,
    val name: String,
    /**
     * The locale used to localize the name of the language.
     */
    val locale: String
)