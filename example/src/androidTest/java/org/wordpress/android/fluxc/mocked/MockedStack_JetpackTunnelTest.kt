package org.wordpress.android.fluxc.mocked

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_JetpackTunnelTest : MockedStack_Base() {
    @Inject internal lateinit var jetpackTunnelClient: JetpackTunnelClientForTests

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Singleton
    class JetpackTunnelClientForTests @Inject constructor(appContext: Context, dispatcher: Dispatcher,
                                                          requestQueue: RequestQueue, accessToken: AccessToken,
                                                          userAgent: UserAgent
    ) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
        /**
         * Wraps and exposes the protected [add] method so that tests can add requests directly.
         */
        fun <T> exposedAdd(request: WPComGsonRequest<T>?) { add(request) }
    }
}
