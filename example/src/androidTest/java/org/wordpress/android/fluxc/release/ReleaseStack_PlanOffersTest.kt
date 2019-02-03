package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.PlanOffersAction
import org.wordpress.android.fluxc.generated.PlanOffersActionBuilder
import org.wordpress.android.fluxc.store.PlanOffersStore
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_PlanOffersTest : ReleaseStack_Base() {
    @Suppress("unused")
    @Inject lateinit var planOffersStore: PlanOffersStore // needs to be injected for test to work properly
    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        PLAN_OFFERS_FETCHED
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
    fun testFetchPlanOffers() {
        nextEvent = TestEvents.PLAN_OFFERS_FETCHED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(PlanOffersActionBuilder.generateNoPayloadAction(PlanOffersAction.FETCH_PLAN_OFFERS))
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Subscribe
    @Suppress("unused")
    fun onPlanOffersFetched(event: OnPlanOffersFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }

        Assert.assertNotNull(event.planOffers)
        Assert.assertTrue(event.planOffers!!.isNotEmpty())
        mCountDownLatch.countDown()
    }
}
