package org.wordpress.android.fluxc.example.di

import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Named

@Module
class InterceptorModule {
    @Provides @IntoSet @Named("network-interceptors")
    fun provideFlipperInterceptor(): Interceptor =
            FlipperOkhttpInterceptor(
                    AndroidFlipperClient.getInstanceIfInitialized()?.getPlugin(NetworkFlipperPlugin.ID)
            )
}
