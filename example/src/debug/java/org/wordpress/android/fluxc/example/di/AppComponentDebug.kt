package org.wordpress.android.fluxc.example.di

import dagger.Component
import dagger.android.AndroidInjectionModule
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidInjectionModule::class,
        ApplicationModule::class,
        AppSecretsModule::class,
        DebugOkHttpClientModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class,
        MainActivityModule::class))
interface AppComponentDebug : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder
}
