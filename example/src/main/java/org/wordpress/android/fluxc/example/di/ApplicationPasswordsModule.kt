package org.wordpress.android.fluxc.example.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.example.ApplicationPasswordsLogger
import org.wordpress.android.fluxc.module.ApplicationPasswordClientId
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@Module
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(logger: ApplicationPasswordsLogger): ApplicationPasswordsListener

    companion object {
        @Provides
        @ApplicationPasswordClientId
        fun provideApplicationName(): String {
            return "fluxc-example-android"
        }
    }
}
