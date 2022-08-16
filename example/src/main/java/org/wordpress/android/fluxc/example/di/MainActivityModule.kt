package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.MainExampleActivity

@Module
internal interface MainActivityModule {
    @ContributesAndroidInjector(modules = arrayOf(FragmentsModule::class))
    fun provideMainActivityInjector(): MainExampleActivity
}
