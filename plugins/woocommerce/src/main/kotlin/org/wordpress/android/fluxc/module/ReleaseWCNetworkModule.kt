package org.wordpress.android.fluxc.module

import android.content.Context
import com.android.volley.RequestQueue
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class ReleaseWCNetworkModule {
    @Singleton
    @Provides
    fun provideOrderRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = OrderRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideOrderStatsRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = OrderStatsRestClient(appContext, dispatcher, requestQueue, token, userAgent)
}
