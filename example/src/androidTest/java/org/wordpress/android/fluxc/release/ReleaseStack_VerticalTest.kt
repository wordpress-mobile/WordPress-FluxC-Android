package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.release.ReleaseStack_VerticalTest.TestEvents.SEGMENTS_FETCHED
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_VerticalTest : ReleaseStack_Base() {
    @Suppress("unused")
    @Inject lateinit var verticalStore: VerticalStore

    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        SEGMENTS_FETCHED
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvents.NONE
    }

    @Test
    fun testFetchSegments() {
        nextEvent = SEGMENTS_FETCHED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(VerticalActionBuilder.newFetchSegmentsAction())

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Subscribe
    @Suppress("unused")
    fun onSegmentsFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertEquals(TestEvents.SEGMENTS_FETCHED, nextEvent)
        assertTrue(event.segmentList.isNotEmpty())
        mCountDownLatch.countDown()
    }
}
