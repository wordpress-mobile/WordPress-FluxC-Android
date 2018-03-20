package org.wordpress.android.fluxc.release

import junit.framework.Assert.assertEquals
import org.greenrobot.eventbus.Subscribe
import org.junit.After
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.ActivityAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivitiesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivitiesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue

class ReleaseStack_ActivityLogTestWPCom : ReleaseStack_WPComBase() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject lateinit var activityLogStore: ActivityLogStore
    private var mBackup: CountDownLatch? = null

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
        this.mBackup = this.mCountDownLatch
        this.incomingActions.clear()
    }

    @Test
    fun testFetchActivities() {
        this.mCountDownLatch = CountDownLatch(1)
        val numOfActivitiesRequested = 1
        val payload = FetchActivitiesPayload(ReleaseStack_WPComBase.sSite, numOfActivitiesRequested, 0)
        activityLogStore.onAction(ActivityActionBuilder.newFetchActivitiesAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        assertEquals((incomingActions[0].payload as FetchedActivitiesPayload)
                .activityLogModelRespons
                .size, numOfActivitiesRequested)
    }

    @Test
    fun testFetchRewindState() {
        this.mCountDownLatch = CountDownLatch(1)
        val payload = ActivityLogStore.FetchRewindStatePayload(ReleaseStack_WPComBase.sSite)
        activityLogStore.onAction(ActivityActionBuilder.newFetchRewindStateAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        val fetchedRewindStatePayload = incomingActions[0].payload as FetchedRewindStatePayload
        with(fetchedRewindStatePayload) {
            assertNotNull(this.rewindStatusModelResponse)
            assertNotNull(this.rewindStatusModelResponse?.state)
        }
    }

    @After
    fun tearDown() {
        this.mCountDownLatch = mBackup
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is ActivityAction) {
            incomingActions.add(action)
            mCountDownLatch?.countDown()
        }
    }
}
