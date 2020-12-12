package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ThreatAction
import org.wordpress.android.fluxc.generated.ThreatActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.ThreatRestClient
import org.wordpress.android.fluxc.persistence.ThreatSqlUtils
import org.wordpress.android.fluxc.store.ThreatStore.FetchThreatPayload
import org.wordpress.android.fluxc.store.ThreatStore.FetchedThreatPayload
import org.wordpress.android.fluxc.store.ThreatStore.ThreatError
import org.wordpress.android.fluxc.store.ThreatStore.ThreatErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ThreatStoreTest {
    @Mock private lateinit var threatRestClient: ThreatRestClient
    @Mock private lateinit var threatSqlUtils: ThreatSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var threatStore: ThreatStore
    private val threatId: Long = 1L

    @Before
    fun setUp() {
        threatStore = ThreatStore(
            threatRestClient,
            threatSqlUtils,
            initCoroutineEngine(),
            dispatcher
        )
    }

    @Test
    fun `fetch threat triggers rest client`() = test {
        val payload = FetchThreatPayload(siteModel, threatId)
        whenever(threatRestClient.fetchThreat(siteModel, threatId)).thenReturn(FetchedThreatPayload(null, siteModel))

        val action = ThreatActionBuilder.newFetchThreatAction(payload)
        threatStore.onAction(action)

        verify(threatRestClient).fetchThreat(siteModel, threatId)
    }

    @Test
    fun `error on fetch threat returns the error`() = test {
        val error = ThreatError(ThreatErrorType.INVALID_RESPONSE, "error")

        val payload = FetchedThreatPayload(error, siteModel)
        whenever(threatRestClient.fetchThreat(siteModel, threatId)).thenReturn(payload)

        val fetchAction = ThreatActionBuilder.newFetchThreatAction(FetchThreatPayload(siteModel, threatId))
        threatStore.onAction(fetchAction)

        val expectedEventWithError = ThreatStore.OnThreatFetched(payload.error, ThreatAction.FETCH_THREAT)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `get threats for a site returns threats for it from the db`() {
        val threatModels = mock<List<ThreatModel>>()
        whenever(threatSqlUtils.getThreatsForSite(siteModel)).thenReturn(threatModels)

        val threatModelsFromDb = threatStore.getThreatsForSite(siteModel)

        verify(threatSqlUtils).getThreatsForSite(siteModel)
        Assert.assertEquals(threatModels, threatModelsFromDb)
    }
}
