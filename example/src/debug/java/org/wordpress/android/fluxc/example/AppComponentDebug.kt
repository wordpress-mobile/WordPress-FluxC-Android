package org.wordpress.android.fluxc.example

import dagger.Component
import org.wordpress.android.fluxc.module.AppContextModule
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AppContextModule::class,
        AppSecretsModule::class,
        DebugOkHttpClientModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class))
interface AppComponentDebug : AppComponent
