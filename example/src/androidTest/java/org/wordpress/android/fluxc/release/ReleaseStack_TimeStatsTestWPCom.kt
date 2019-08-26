package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.stats.time.FileDownloadsStore
import java.util.Calendar
import javax.inject.Inject

private const val ITEMS_TO_LOAD = 8
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)

class ReleaseStack_TimeStatsTestWPCom : ReleaseStack_WPComBase() {
    @Inject lateinit var fileDownloadsStore: FileDownloadsStore

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
    }

    @Test
    fun testFetchFileDownloads() {
        val selectedDate = Calendar.getInstance()
        selectedDate.set(2019, 7, 7)

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                fileDownloadsStore.fetchFileDownloads(
                        sSite,
                        granularity,
                        LIMIT_MODE,
                        selectedDate.time,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = fileDownloadsStore.getFileDownloads(
                    sSite,
                    granularity,
                    LIMIT_MODE,
                    selectedDate.time
            )

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }
}
