package org.wordpress.android.fluxc.ditests

import dagger.Component
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            OkHttpClientModule::class,
            ReleaseNetworkModule::class,
            TestInterceptorModule::class
        ]
)
interface OkHttpTestComponent {
    fun inject(test: OkHttpInjectionTest)
}
