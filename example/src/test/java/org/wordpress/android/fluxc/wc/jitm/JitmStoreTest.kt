package org.wordpress.android.fluxc.wc.jitm

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmDismissApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmRestClient
import org.wordpress.android.fluxc.store.JitmStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class JitmStoreTest {
    private val restClient = mock<JitmRestClient>()

    private lateinit var store: JitmStore

    @Before
    fun setUp() {
        store = JitmStore(initCoroutineEngine(), restClient)
    }

    @Test
    fun `given jitm dismiss success, when jitm dismissed, then return true`() {
        test {
            whenever(restClient.dismissJitmMessage(any(), any(), any())).thenReturn(
                WooPayload(JitmDismissApiResponse(
                    data = true
                ))
            )

            val result = store.dismissJitmMessage(mock(), "", "")

            assertTrue(result)
        }
    }

    @Test
    fun `given jitm dismiss failure, when jitm dismissed, then return false`() {
        test {
            whenever(restClient.dismissJitmMessage(any(), any(), any())).thenReturn(
                WooPayload(WooError(
                    WooErrorType.GENERIC_ERROR,
                    original = BaseRequest.GenericErrorType.NETWORK_ERROR
                ))
            )

            val result = store.dismissJitmMessage(mock(), "", "")

            assertFalse(result)
        }
    }
}