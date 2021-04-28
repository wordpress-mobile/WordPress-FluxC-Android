package org.wordpress.android.fluxc.ditests

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Named

@Module
class TestInterceptorModule {
    @Provides @IntoSet @Named("interceptors")
    fun provideDummyInterceptor(): Interceptor = DummyInterceptor

    @Provides @IntoSet @Named("network-interceptors")
    fun provideDummyNetworkInterceptor(): Interceptor = DummyNetworkInterceptor
}

object DummyInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        return Response.Builder().build()
    }
}

object DummyNetworkInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        return Response.Builder().build()
    }
}