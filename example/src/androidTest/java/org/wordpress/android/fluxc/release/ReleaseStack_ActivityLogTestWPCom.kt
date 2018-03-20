package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.Store
import java.util.Date

class ReleaseStack_ActivityLogTestWPCom : ReleaseStack_WPComBase() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    private val incomingChangeEvents: MutableList<Store.OnChanged<ActivityLogStore.ActivityError>> = mutableListOf()
    @Inject lateinit var activityLogStore: ActivityLogStore
    private var backup: CountDownLatch? = null

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
        this.backup = this.mCountDownLatch
        this.incomingActions.clear()
        this.incomingChangeEvents.clear()
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
                .activityLogModels
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

    @Test
    fun storeAndRetrieveActivityLogInOrderByDateFromDb() {
        this.mCountDownLatch = CountDownLatch(1)

        val firstActivity = activityLogModel(1)
        val secondActivity = activityLogModel(2)
        val thirdActivity = activityLogModel(3)
        val payload = FetchedActivitiesPayload(activityModels = listOf(firstActivity,
                secondActivity,
                thirdActivity),
                site = sSite,
                number = 3,
                offset = 0)

        activityLogStore.onAction(ActivityActionBuilder.newFetchedActivitiesAction(payload))

        val activityLogForSite = awaitActivities(3, ascending = false)

        assertEquals(activityLogForSite.size, 3)
        assertEquals(activityLogForSite[0], thirdActivity)
        assertEquals(activityLogForSite[1], secondActivity)
        assertEquals(activityLogForSite[2], firstActivity)
    }

    @Test
    fun updatesActivityWithTheSameActivityId() {
        this.mCountDownLatch = CountDownLatch(1)

        val activity = activityLogModel(1)
        val payload = FetchedActivitiesPayload(listOf(activity), sSite, 1, 0)

        activityLogStore.onAction(ActivityActionBuilder.newFetchedActivitiesAction(payload))

        val activityLogForSite = awaitActivities(1)

        assertEquals(activityLogForSite.size, 1)
        assertEquals(activityLogForSite[0], activity)

        val updatedName = "updatedName"
        val updatedPayload = FetchedActivitiesPayload(listOf(activity.copy(name = updatedName)), sSite, 1, 0)

        activityLogStore.onAction(ActivityActionBuilder.newFetchedActivitiesAction(updatedPayload))

        val updatedActivityLogForSite = awaitActivities(1)

        assertEquals(updatedActivityLogForSite.size, 1)
        assertEquals(updatedActivityLogForSite[0].activityID, activity.activityID)
        assertEquals(updatedActivityLogForSite[0].name, updatedName)
    }

    private fun activityLogModel(index: Long): ActivityLogModel {
        return ActivityLogModel(activityID = "$index",
                summary = "summary$index",
                text = "text$index",
                name = "name$index",
                type = "type",
                discarded = false,
                published = Date(index * 100),
                rewindID = null,
                gridicon = null,
                rewindable = true,
                status = "status")
    }

    private fun awaitActivities(count: Int, ascending: Boolean = true): List<ActivityLogModel> {
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertEquals(incomingChangeEvents.size, 1)
        val onFetchedEvent = incomingChangeEvents[0] as ActivityLogStore.OnActivitiesFetched
        with(onFetchedEvent) {
            assertEquals(onFetchedEvent.rowsAffected, count)
            assertNull(onFetchedEvent.error)
        }
        incomingChangeEvents.clear()
        return activityLogStore.getActivityLogForSite(sSite, ascending = ascending)
    }

    @After
    fun tearDown() {
        this.mCountDownLatch = backup
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is ActivityAction) {
            incomingActions.add(action)
            mCountDownLatch?.countDown()
        }
    }

    @Subscribe
    fun onChange(onChangedEvent: Store.OnChanged<ActivityLogStore.ActivityError>) {
        incomingChangeEvents.add(onChangedEvent)
        mCountDownLatch?.countDown()
    }
}
