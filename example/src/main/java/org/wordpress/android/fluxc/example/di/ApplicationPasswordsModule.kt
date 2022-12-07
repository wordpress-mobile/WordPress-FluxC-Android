package org.wordpress.android.fluxc.example.di

import dagger.Binds
import dagger.Module
import org.wordpress.android.fluxc.example.ApplicationPasswordsLogger
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@Module
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(logger: ApplicationPasswordsLogger): ApplicationPasswordsListener
}
