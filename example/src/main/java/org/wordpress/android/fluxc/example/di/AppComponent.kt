package org.wordpress.android.fluxc.example.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import org.wordpress.android.fluxc.example.ExampleApp
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseWCNetworkModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidSupportInjectionModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        ReleaseOkHttpClientModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class,
        ReleaseWCNetworkModule::class,
        MainActivityModule::class,
        WCOrderListActivityModule::class))
interface AppComponent : AndroidInjector<ExampleApp> {
    override fun inject(app: ExampleApp)

    // Allows us to inject the application without having to instantiate any modules, and provides the Application
    // in the app graph
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}
