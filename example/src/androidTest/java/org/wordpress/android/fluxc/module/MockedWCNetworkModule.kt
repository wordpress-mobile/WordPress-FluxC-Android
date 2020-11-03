package org.wordpress.android.fluxc.module

import android.content.Context
import com.android.volley.RequestQueue
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class MockedWCNetworkModule {
    @Singleton
    @Provides
    fun provideWooCommerceRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = WooCommerceRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideOrderRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = OrderRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideProductRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = ProductRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideOrderStatsRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = OrderStatsRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideWooPluginRestClient(
        appContext: Context,
        wpComBuilder: WPComGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = WooPluginRestClient(dispatcher, wpComBuilder, appContext, requestQueue, token, userAgent)
}
