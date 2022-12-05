package org.wordpress.android.fluxc.example.di

import dagger.Binds
import dagger.Module
import org.wordpress.android.fluxc.example.ApplicationPasswordsUnavailableLogger
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsUnavailableListener

@Module
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(logger: ApplicationPasswordsUnavailableLogger)
        : ApplicationPasswordsUnavailableListener
}
