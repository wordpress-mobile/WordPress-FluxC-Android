package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.SSREntity

@RunWith(RobolectricTestRunner::class)
internal class SSRDaoTest {
    private lateinit var database: WCAndroidDatabase
    private lateinit var sut: SSRDao

    private companion object {
        const val TEST_SITE_REMOTE_ID = 1337L
        val sampleEntity = SSREntity(
                remoteSiteId = TEST_SITE_REMOTE_ID,
                environment = "",
                database = "",
                activePlugins = "",
                theme = "",
                settings = "",
                security = "",
                pages = ""
        )
        val sampleModel = WCSSRModel(
                remoteSiteId = TEST_SITE_REMOTE_ID,
                environment = "",
                database = "",
                activePlugins = "",
                theme = "",
                settings = "",
                security = "",
                pages = ""
        )
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sut = database.ssrDao()
    }

    @Test
    fun `save and retrieve SSR`(): Unit = runBlocking {
        sut.insertSSR(sampleEntity)

        val resultFromDatabase = sut.observeSSRForSite(TEST_SITE_REMOTE_ID).first()
        assertEquals(resultFromDatabase, sampleModel)
    }

    @After
    fun tearDown() {
        database.close()
    }
}
