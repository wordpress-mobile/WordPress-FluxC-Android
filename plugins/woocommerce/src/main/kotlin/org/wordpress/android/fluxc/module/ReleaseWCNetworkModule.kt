package org.wordpress.android.fluxc.module

import android.content.Context
import com.android.volley.RequestQueue
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class ReleaseWCNetworkModule {
    @Singleton
    @Provides
    fun provideWooCommerceRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = WooCommerceRestClient(appContext, dispatcher, requestQueue, token, userAgent)

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
    fun provideProductRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent,
        requestBuilder: JetpackTunnelGsonRequestBuilder
    ) = ProductRestClient(appContext, dispatcher, requestQueue, token, userAgent, requestBuilder)

    @Singleton
    @Provides
    fun provideOrderStatsRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = OrderStatsRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideCustomerRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = CustomerRestClient(appContext, dispatcher, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideRefundsRestClient(
        appContext: Context,
        requestBuilder: JetpackTunnelGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = RefundRestClient(dispatcher, requestBuilder, appContext, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideGatewaysRestClient(
        appContext: Context,
        requestBuilder: JetpackTunnelGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = GatewayRestClient(dispatcher, requestBuilder, appContext, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideTaxRestClient(
        appContext: Context,
        requestBuilder: JetpackTunnelGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = WCTaxRestClient(dispatcher, requestBuilder, appContext, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideDataClient(
        appContext: Context,
        requestBuilder: JetpackTunnelGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = WCDataRestClient(dispatcher, requestBuilder, appContext, requestQueue, token, userAgent)

    @Singleton
    @Provides
    fun provideShippingLabelRestClient(
        appContext: Context,
        requestBuilder: JetpackTunnelGsonRequestBuilder,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        token: AccessToken,
        userAgent: UserAgent
    ) = ShippingLabelRestClient(dispatcher, requestBuilder, appContext, requestQueue, token, userAgent)

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

    @Singleton
    @Provides
    fun provideLeaderboardsRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        accessToken: AccessToken,
        userAgent: UserAgent,
        requestBuilder: JetpackTunnelGsonRequestBuilder
    ) = LeaderboardsRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent, requestBuilder)

    @Singleton
    @Provides
    fun provideProductAttributeRestClient(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        accessToken: AccessToken,
        userAgent: UserAgent,
        requestBuilder: JetpackTunnelGsonRequestBuilder
    ) = ProductAttributeRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent, requestBuilder)

    @Singleton
    @Provides
    fun provideJetpackTunnelGsonRequestBuilder(): JetpackTunnelGsonRequestBuilder {
        return JetpackTunnelGsonRequestBuilder()
    }

    @Singleton
    @Provides
    fun provideRefundsMapper(): RefundMapper {
        return RefundMapper()
    }
}
