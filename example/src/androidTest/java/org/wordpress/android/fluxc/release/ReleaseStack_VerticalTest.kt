package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.VerticalActionBuilder
import org.wordpress.android.fluxc.release.ReleaseStack_VerticalTest.TestEvents.SEGMENTS_FETCHED
import org.wordpress.android.fluxc.release.ReleaseStack_VerticalTest.TestEvents.SEGMENT_PROMPT_FETCHED
import org.wordpress.android.fluxc.release.ReleaseStack_VerticalTest.TestEvents.VERTICALS_FETCHED
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptPayload
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Add a static segment id to test to make the tests that depend on a segment id less convoluted and to the point.
 *
 * If these tests ever start to fail because of this ID, we can simply change it to a different value.
 */
private const val SEGMENT_ID_TO_TEST = 1L
private const val FETCH_VERTICALS_SEARCH_QUERY = "restaurant"
private const val FETCH_VERTICALS_LIMIT = 2

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_VerticalTest : ReleaseStack_Base() {
    @Suppress("unused")
    @Inject lateinit var verticalStore: VerticalStore

    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        SEGMENTS_FETCHED,
        SEGMENT_PROMPT_FETCHED,
        VERTICALS_FETCHED
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

    @Test
    fun testFetchSegmentPrompt() {
        nextEvent = SEGMENT_PROMPT_FETCHED
        mCountDownLatch = CountDownLatch(1)
        val payload = FetchSegmentPromptPayload(segmentId = SEGMENT_ID_TO_TEST)
        mDispatcher.dispatch(VerticalActionBuilder.newFetchSegmentPromptAction(payload))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Test
    fun testFetchVerticals() {
        nextEvent = VERTICALS_FETCHED
        mCountDownLatch = CountDownLatch(1)
        val payload = FetchVerticalsPayload(searchQuery = FETCH_VERTICALS_SEARCH_QUERY, limit = FETCH_VERTICALS_LIMIT)
        mDispatcher.dispatch(VerticalActionBuilder.newFetchVerticalsAction(payload))

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

    @Subscribe
    @Suppress("unused")
    fun onSegmentPromptFetched(event: OnSegmentPromptFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertEquals(TestEvents.SEGMENT_PROMPT_FETCHED, nextEvent)
        assertNotNull(event.prompt)
        mCountDownLatch.countDown()
    }

    @Subscribe
    @Suppress("unused")
    fun onVerticalsFetched(event: OnVerticalsFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertEquals(TestEvents.VERTICALS_FETCHED, nextEvent)
        assertTrue(event.verticalList.size == FETCH_VERTICALS_LIMIT)
        mCountDownLatch.countDown()
    }
}
