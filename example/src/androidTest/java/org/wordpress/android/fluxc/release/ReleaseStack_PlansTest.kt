package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.PlansAction
import org.wordpress.android.fluxc.generated.PlansActionBuilder
import org.wordpress.android.fluxc.store.PlanOffersStore
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_PlansTest : ReleaseStack_Base() {
    @Inject lateinit var planOffersStore: PlanOffersStore
    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        PLANS_FETCHED,
        ERROR_FETCHING_PLANS
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
    fun testFetchPlans() {
        nextEvent = TestEvents.PLANS_FETCHED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(PlansActionBuilder.generateNoPayloadAction(PlansAction.FETCH_PLANS))
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Subscribe
    @Suppress("unused")
    fun onPlansFetched(event: OnPlanOffersFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }

        Assert.assertNotNull(event.planOffers)
        Assert.assertTrue(event.planOffers!!.isNotEmpty())
        mCountDownLatch.countDown()
    }
}